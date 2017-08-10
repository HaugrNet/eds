/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.common.CWSKey;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.KeyEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleService extends Serviceable<ProcessCircleResponse, ProcessCircleRequest> {

    public ProcessCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse perform(final ProcessCircleRequest request) {
        verifyRequest(request, Permission.PROCESS_CIRCLE, readExternalCircleId(request));
        final ProcessCircleResponse response;

        switch (request.getAction()) {
            case CREATE:
                response = createCircle(request);
                break;
            case UPDATE:
                response = updateCircle(request);
                break;
            case DELETE:
                response = deleteCircle(request);
                break;
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
                throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
        }

        return response;
    }

    private static String readExternalCircleId(final ProcessCircleRequest request) {
        String circleId = null;

        if (request != null) {
            circleId = request.getCircleId();
        }

        return circleId;
    }

    /**
     * Creating a new Circle, can only be performed by the System Administrator,
     * and requires the name of a Circle, and the initial Circle Administrator
     * to be set. As part of creating the new Circle, a new Secret Key will be
     * generated and added encrypted via the Circle Administrator. And a new
     * folder (initial data record, used by all data belonging to the Circle),
     * will also be added.
     *
     * @param request Request Object with the Circle Name and Administrator
     * @return Response from the creation.
     */
    private ProcessCircleResponse createCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final String name = request.getCircleName();
            final CircleEntity existing = dao.findCircleByName(name);

            if (existing == null) {
                final MemberEntity circleAdmin = dao.find(MemberEntity.class, request.getMemberId());

                if (circleAdmin == null) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "Cannot create a new Circle with a non-existing Circle Administrator.");
                } else if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "It is not allowed for the System Administrator to be part of a Circle.");
                } else {
                    final CircleEntity circle = new CircleEntity();
                    circle.setName(name);
                    dao.persist(circle);

                    final String salt = UUID.randomUUID().toString();
                    final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
                    final KeyEntity keyEntity = new KeyEntity();
                    keyEntity.setAlgorithm(algorithm);
                    keyEntity.setSalt(salt);
                    keyEntity.setStatus(Status.ACTIVE);
                    dao.persist(keyEntity);

                    final CWSKey key = crypto.generateSymmetricKey(keyEntity.getAlgorithm(), keyEntity.getSalt());
                    final String circleKey = crypto.encryptAndArmorCircleKey(circleAdmin.getKey(), key);
                    final TrusteeEntity trustee = new TrusteeEntity();
                    trustee.setMember(circleAdmin);
                    trustee.setCircle(circle);
                    trustee.setKey(keyEntity);
                    trustee.setTrustLevel(TrustLevel.ADMIN);
                    trustee.setCircleKey(circleKey);
                    dao.persist(trustee);

                    response = new ProcessCircleResponse();
                }
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
            }
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only the System Administrator may create a new Circle.");
        }

        return response;
    }

    /**
     * <p>Updating a Circle, means changing the name of it, as this is the only
     * valid information it has. Both the System Administrator and Circle
     * Administrator is allowed to perform this action. And as the initial
     * checks already have verified that it is either of the Administrators,
     * there are no other permission checks required.</p>
     *
     * <p>Only checks needed, is to verify that the name is not already taken
     * by any other circle.</p>
     *
     * @param request Request Object with Id and new Name for the Circle
     * @return Response Object with the changed information
     */
    private ProcessCircleResponse updateCircle(final ProcessCircleRequest request) {
        final String externalId = request.getCircleId();
        final String name = request.getCircleName();
        final ProcessCircleResponse response;

        final CircleEntity existing = dao.findCircleByName(name);

        if (existing == null) {
            final CircleEntity entity = dao.find(CircleEntity.class, externalId);
            entity.setName(name);
            dao.persist(entity);

            response = new ProcessCircleResponse();
        } else {
            response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
        }

        return response;
    }

    /**
     * <p>Deleting an existing Circle, is an irreversible process, by which all
     * the data, keys & trustees will also be deleted alongside the Circle.</p>
     *
     * <p>The operation can only be performed by the System Administrator, due
     * to the nature of it.</p>
     *
     * @param request Request Object with the Id of the Circle to delete
     * @return Response Object with error information
     */
    private ProcessCircleResponse deleteCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final String externalId = request.getCircleId();
            final CircleEntity entity = dao.find(CircleEntity.class, externalId);
            dao.delete(entity);

            response = new ProcessCircleResponse();
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only the System Administrator may delete a Circle.");
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
    private ProcessCircleResponse addTrustee(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "The System Administrator cannot add a Member to a Circle.");
        } else {
            final String memberId = request.getMemberId();
            final MemberEntity newTrusteeMember = dao.find(MemberEntity.class, memberId);
            final List<TrusteeEntity> existing = dao.findTrustByMemberAndCircle(newTrusteeMember, request.getCircleId());

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

                if ((trustLevel == TrustLevel.ADMIN) || (trustLevel == TrustLevel.WRITE) || (trustLevel == TrustLevel.READ)) {
                    final CWSKey circleKey = crypto.extractCircleKey(admin.getKey().getAlgorithm(), member.getKey(), admin.getCircleKey());
                    trustee.setCircleKey(crypto.encryptAndArmorCircleKey(newTrusteeMember.getKey(), circleKey));
                }

                dao.persist(trustee);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
            }
        }

        return response;
    }

    private ProcessCircleResponse alterTrustee(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final TrusteeEntity trustee = dao.find(TrusteeEntity.class, request.getMemberId());
            if (trustee != null) {
                final TrustLevel newTrustLevel = request.getTrustLevel();
                trustee.setTrustLevel(newTrustLevel);

                if (newTrustLevel == TrustLevel.GUEST) {
                    trustee.setCircleKey(null);
                } else if (trustee.getTrustLevel() == TrustLevel.GUEST) {
                    final TrusteeEntity admin = trustees.get(0);
                    final CWSKey circleKey = crypto.extractCircleKey(admin.getKey().getAlgorithm(), member.getKey(), admin.getCircleKey());
                    final String armoredKey = crypto.encryptAndArmorCircleKey(trustee.getMember().getKey(), circleKey);
                    trustee.setCircleKey(armoredKey);
                }

                dao.persist(trustee);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Trustee could not be found.");
            }
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may alter a Trustee.");
        }

        return response;
    }

    private ProcessCircleResponse removeTrustee(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final TrusteeEntity trustee = dao.find(TrusteeEntity.class, request.getMemberId());
            if (trustee != null) {
                dao.delete(trustee);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Trustee could not be found.");
            }
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may delete a Trustee.");
        }

        return response;
    }
}
