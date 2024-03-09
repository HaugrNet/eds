/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
 * mailto: eds AT haugr DOT net
 *
 * EDS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * EDS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.eds.core.managers;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.PublicEDSKey;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.CommonDao;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessTrustee request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessTrusteeManager extends AbstractManager<CommonDao, ProcessTrusteeResponse, ProcessTrusteeRequest> {

    public ProcessTrusteeManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessTrusteeResponse perform(final ProcessTrusteeRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        return switch (request.getAction()) {
            case ADD -> addTrustee(request);
            case ALTER -> alterTrustee(request);
            case REMOVE -> removeTrustee(request);
            // Unreachable Code by design.
            default -> throw new IllegalActionException("Unsupported Action.");
        };
    }

    /**
     * As the Circle ID is mandatory for this request, it also means that the
     * Validation logic has extracted the Circle Administrator as the only
     * Trustee record, so we can use this to create a new Trustee record from.
     *
     * @param request Request Object with new Trustee information
     * @return Response with error information.
     */
    private ProcessTrusteeResponse addTrustee(final ProcessTrusteeRequest request) {
        if (trustees.isEmpty()) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "It is not possible to add a member to a circle, without membership.");
        }

        final String memberId = request.getMemberId();
        final MemberEntity newTrusteeMember = dao.find(MemberEntity.class, memberId);

        if (newTrusteeMember == null) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "No Member could be found with the given Id.");
        }

        final List<TrusteeEntity> existing = dao.findTrusteesByMemberAndCircle(newTrusteeMember, request.getCircleId(), TrustLevel.getLevels(TrustLevel.ALL));

        if (!existing.isEmpty()) {
            throw new EDSException(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
        }

        // Please be aware, that during re-key requests - there will
        // exist 2 Trustee entities, one with the old Key and one with
        // the new. In the unlikely event that someone is being added
        // during this - the logic should also reflect it. However, as
        // re-key is not supported in version 1.0, support for multiple
        // Keys can wait until this is also supported.
        final TrusteeEntity admin = trustees.getFirst();
        final TrustLevel trustLevel = request.getTrustLevel();
        final TrusteeEntity trustee = new TrusteeEntity();
        trustee.setMember(newTrusteeMember);
        trustee.setCircle(admin.getCircle());
        trustee.setKey(admin.getKey());
        trustee.setTrustLevel(trustLevel);

        final SecretEDSKey circleKey = Crypto.extractCircleKey(admin.getKey().getAlgorithm(), keyPair.getPrivate(), admin.getCircleKey());
        final PublicKey publicKey = crypto.dearmoringPublicKey(newTrusteeMember.getPublicKey());
        final PublicEDSKey edsPublicKey = new PublicEDSKey(newTrusteeMember.getRsaAlgorithm(), publicKey);
        trustee.setCircleKey(Crypto.encryptAndArmorCircleKey(edsPublicKey, circleKey));

        dao.save(trustee);
        return new ProcessTrusteeResponse("The Member '" + trustee.getMember().getName() + "' was successfully added as trustee to '" + trustee.getCircle().getName() + "'.");
    }

    private ProcessTrusteeResponse alterTrustee(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = findTrusteeForCircleAndMember(request);
        trustee.setTrustLevel(request.getTrustLevel());
        dao.save(trustee);

        return new ProcessTrusteeResponse("The Trustee '" + trustee.getMember().getName() + "' has successfully been given the trust level '" + trustee.getTrustLevel() + "' in the Circle '" + trustee.getCircle().getName() + "'.");
    }

    private ProcessTrusteeResponse removeTrustee(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = findTrusteeForCircleAndMember(request);
        dao.delete(trustee);

        return new ProcessTrusteeResponse("The Trustee '" + trustee.getMember().getName() + "' was successfully removed from the Circle '" + trustee.getCircle().getName() + "'.");
    }

    private TrusteeEntity findTrusteeForCircleAndMember(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());

        if (trustee == null) {
            throw new IdentificationException("The requested Trustee could not be found.");
        }

        return trustee;
    }
}
