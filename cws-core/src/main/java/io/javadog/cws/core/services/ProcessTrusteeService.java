/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessTrusteeRequest;
import io.javadog.cws.api.responses.ProcessTrusteeResponse;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.PublicCWSKey;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS ProcessTrustee request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessTrusteeService extends Serviceable<CommonDao, ProcessTrusteeResponse, ProcessTrusteeRequest> {

    public ProcessTrusteeService(final Settings settings, final EntityManager entityManager) {
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
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
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
        final ProcessTrusteeResponse response;

        if (member.getMemberRole() == MemberRole.ADMIN) {
            response = new ProcessTrusteeResponse(ReturnCode.AUTHORIZATION_WARNING, "The System Administrator cannot add a Member to a Circle.");
        } else {
            final String memberId = request.getMemberId();
            final MemberEntity newTrusteeMember = dao.find(MemberEntity.class, memberId);

            if (newTrusteeMember != null) {
                final List<TrusteeEntity> existing = dao.findTrusteesByMemberAndCircle(newTrusteeMember, request.getCircleId(), TrustLevel.getLevels(TrustLevel.ALL));

                if (existing.isEmpty()) {
                    // Please be aware, that during re-key requests - there will
                    // exist 2 Trustee entities, one with the old Key and one with
                    // the new. In the unlikely event that someone is being added
                    // during this - the logic should also reflect it. However, as
                    // re-key is not supported in version 1.0, support for multiple
                    // Keys can wait until this is also supported.
                    final TrusteeEntity admin = trustees.get(0);
                    final TrustLevel trustLevel = request.getTrustLevel();
                    final TrusteeEntity trustee = new TrusteeEntity();
                    trustee.setMember(newTrusteeMember);
                    trustee.setCircle(admin.getCircle());
                    trustee.setKey(admin.getKey());
                    trustee.setTrustLevel(trustLevel);

                    final SecretCWSKey circleKey = crypto.extractCircleKey(admin.getKey().getAlgorithm(), keyPair.getPrivate(), admin.getCircleKey());
                    final PublicKey publicKey = crypto.dearmoringPublicKey(newTrusteeMember.getPublicKey());
                    final PublicCWSKey cwsPublicKey = new PublicCWSKey(newTrusteeMember.getRsaAlgorithm(), publicKey);
                    trustee.setCircleKey(crypto.encryptAndArmorCircleKey(cwsPublicKey, circleKey));

                    dao.persist(trustee);

                    response = new ProcessTrusteeResponse();
                } else {
                    response = new ProcessTrusteeResponse(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
                }
            } else {
                response = new ProcessTrusteeResponse(ReturnCode.IDENTIFICATION_WARNING, "No Member could be found with the given Id.");
            }
        }

        return response;
    }

    private ProcessTrusteeResponse alterTrustee(final ProcessTrusteeRequest request) {
        final ProcessTrusteeResponse response;

        if (member.getMemberRole() != MemberRole.ADMIN) {
            final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());
            if (trustee != null) {
                final TrustLevel newTrustLevel = request.getTrustLevel();
                trustee.setTrustLevel(newTrustLevel);
                dao.persist(trustee);

                response = new ProcessTrusteeResponse();
            } else {
                response = new ProcessTrusteeResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Trustee could not be found.");
            }
        } else {
            response = new ProcessTrusteeResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may alter a Trustee.");
        }

        return response;
    }

    private ProcessTrusteeResponse removeTrustee(final ProcessTrusteeRequest request) {
        final ProcessTrusteeResponse response;

        if (member.getMemberRole() != MemberRole.ADMIN) {
            final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());
            if (trustee != null) {
                dao.delete(trustee);

                response = new ProcessTrusteeResponse();
            } else {
                response = new ProcessTrusteeResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Trustee could not be found.");
            }
        } else {
            response = new ProcessTrusteeResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may remove a Trustee.");
        }

        return response;
    }
}
