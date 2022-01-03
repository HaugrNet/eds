/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
 *
 * CWS is free software; you can redistribute it and/or modify it under the
 * terms of the Apache License, as published by the Apache Software Foundation.
 *
 * CWS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the Apache License for more details.
 *
 * You should have received a copy of the Apache License, version 2, along with
 * this program; If not, you can download a copy of the License
 * here: https://www.apache.org/licenses/
 */
package net.haugr.cws.core.managers;

import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.common.TrustLevel;
import net.haugr.cws.api.requests.ProcessTrusteeRequest;
import net.haugr.cws.api.responses.ProcessTrusteeResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.exceptions.CWSException;
import net.haugr.cws.core.exceptions.IdentificationException;
import net.haugr.cws.core.exceptions.IllegalActionException;
import net.haugr.cws.core.jce.Crypto;
import net.haugr.cws.core.jce.PublicCWSKey;
import net.haugr.cws.core.jce.SecretCWSKey;
import net.haugr.cws.core.model.CommonDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.model.entities.TrusteeEntity;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS ProcessTrustee request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
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
        final ProcessTrusteeResponse response;

        switch (request.getAction()) {
            case ADD:
                response = addTrustee(request);
                break;
            case ALTER:
                response = alterTrustee(request);
                break;
            case REMOVE:
                response = removeTrustee(request);
                break;
            default:
                // Unreachable Code by design.
                throw new IllegalActionException("Unsupported Action.");
        }

        return response;
    }

    /**
     * As the Circle Id is mandatory for this request, it also means that the
     * Validation logic has extracted the Circle Administrator as the only
     * Trustee record, so we can use this to create a new Trustee record from.
     *
     * @param request Request Object with new Trustee information
     * @return Response with error information.
     */
    private ProcessTrusteeResponse addTrustee(final ProcessTrusteeRequest request) {
        if (trustees.isEmpty()) {
            throw new CWSException(ReturnCode.ILLEGAL_ACTION, "It is not possible to add a member to a circle, without membership.");
        }

        final String memberId = request.getMemberId();
        final MemberEntity newTrusteeMember = dao.find(MemberEntity.class, memberId);

        if (newTrusteeMember == null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "No Member could be found with the given Id.");
        }

        final List<TrusteeEntity> existing = dao.findTrusteesByMemberAndCircle(newTrusteeMember, request.getCircleId(), TrustLevel.getLevels(TrustLevel.ALL));

        if (!existing.isEmpty()) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
        }

        // Please be aware, that during re-key requests - there will
        // exist 2 Trustee entities, one with the old Key and one with
        // the new. In the unlikely event that someone is being added
        // during this - the logic should also reflect it. However, as
        // re-key is not supported in version 1.0, support for multiple
        // Keys can wait until this is also supported.
        final TrusteeEntity admin = trustees.get(0);
        final var trustLevel = request.getTrustLevel();
        final var trustee = new TrusteeEntity();
        trustee.setMember(newTrusteeMember);
        trustee.setCircle(admin.getCircle());
        trustee.setKey(admin.getKey());
        trustee.setTrustLevel(trustLevel);

        final SecretCWSKey circleKey = Crypto.extractCircleKey(admin.getKey().getAlgorithm(), keyPair.getPrivate(), admin.getCircleKey());
        final var publicKey = crypto.dearmoringPublicKey(newTrusteeMember.getPublicKey());
        final var cwsPublicKey = new PublicCWSKey(newTrusteeMember.getRsaAlgorithm(), publicKey);
        trustee.setCircleKey(Crypto.encryptAndArmorCircleKey(cwsPublicKey, circleKey));

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
