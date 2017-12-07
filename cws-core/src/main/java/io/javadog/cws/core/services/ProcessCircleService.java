/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.common.enums.KeyAlgorithm;
import io.javadog.cws.common.enums.Status;
import io.javadog.cws.common.exceptions.CWSException;
import io.javadog.cws.common.keys.PublicCWSKey;
import io.javadog.cws.common.keys.SecretCWSKey;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.KeyEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

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
        verifyRequest(request, Permission.PROCESS_CIRCLE);
        final ProcessCircleResponse response;
        final Action action = request.getAction();

        if (action == Action.CREATE) {
            // Anyone cay create a Circle, only a Circle Administrator of the
            // Circle in question may perform other actions on Circles.
            response = createCircle(request);
        } else {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName()) || (trustees.get(0).getTrustLevel() == TrustLevel.ADMIN)) {
                switch (request.getAction()) {
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
                        // Unreachable Code by design.
                        throw new CWSException(ReturnCode.ILLEGAL_ACTION, "Unsupported Action.");
                }
            } else {
                response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may perform this action.");
            }
        }

        return response;
    }

    /**
     * <p>Creation of Circles can be made by the System Administrator provided
     * that the System Administrator has set the Id of the initial Circle
     * Administrator, which cannot be the System Administrator.</p>
     *
     * <p>Creation of Circles can also be made by other members, and if that
     * is the case, then they will be set themselves as the initial Circle
     * Administrator.</p>
     *
     * @param request Request Object with the Circle Name and Administrator
     * @return Response from the creation.
     */
    private ProcessCircleResponse createCircle(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        final String name = request.getCircleName();
        final CircleEntity existing = dao.findCircleByName(name);
        if (existing != null) {
            response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
        } else {
            if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
                // The administrator is creating a new Circle, which requires that
                // a Member Id is provided as the new Circle Administrator.
                final MemberEntity owner = dao.find(MemberEntity.class, request.getMemberId());
                if (owner == null) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "Cannot create a new Circle with a non-existing Circle Administrator.");
                } else if (Objects.equals(owner.getName(), Constants.ADMIN_ACCOUNT)) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "It is not allowed for the System Administrator to be part of a Circle.");
                } else {
                    response = createCircle(owner, name);
                }
            } else {
                response = createCircle(member, name);
            }
        }
        return response;
    }

    /**
     * <p>Creates the actual Circle with given Circle Name and Admin. The Circle
     * will also be given a new encryption key and a default root folder for
     * storing of all Data Objects.</p>
     *
     * @param circleAdmin The initial Circle Administrator
     * @param name        The name of the new Circle
     * @return Response Object with the new Circle Id
     */
    private ProcessCircleResponse createCircle(final MemberEntity circleAdmin, final String name) {
        final CircleEntity circle = new CircleEntity();
        circle.setName(name);
        dao.persist(circle);

        createRootFolder(circle);
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        final KeyEntity keyEntity = new KeyEntity();
        keyEntity.setAlgorithm(algorithm);
        keyEntity.setStatus(Status.ACTIVE);
        dao.persist(keyEntity);

        final SecretCWSKey key = crypto.generateSymmetricKey(keyEntity.getAlgorithm());
        final PublicKey publicKey = crypto.dearmoringPublicKey(circleAdmin.getPublicKey());
        final PublicCWSKey cwsPublicKey = new PublicCWSKey(circleAdmin.getRsaAlgorithm(), publicKey);
        final String circleKey = crypto.encryptAndArmorCircleKey(cwsPublicKey, key);
        final TrusteeEntity trustee = new TrusteeEntity();
        trustee.setMember(circleAdmin);
        trustee.setCircle(circle);
        trustee.setKey(keyEntity);
        trustee.setTrustLevel(TrustLevel.ADMIN);
        trustee.setCircleKey(circleKey);
        dao.persist(trustee);

        final ProcessCircleResponse response = new ProcessCircleResponse();
        response.setCircleId(circle.getExternalId());

        return response;
    }

    private void createRootFolder(final CircleEntity circle) {
        final DataTypeEntity dataTypeEntity = dao.getReference(DataTypeEntity.class, 1L);
        final MetadataEntity entity = new MetadataEntity();
        entity.setCircle(circle);
        entity.setName("/");
        entity.setParentId(0L);
        entity.setType(dataTypeEntity);
        dao.persist(entity);
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
            if (entity != null) {
                entity.setName(name);
                dao.persist(entity);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");
            }
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
            if (entity != null) {
                dao.delete(entity);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");
            }
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

            if (newTrusteeMember != null) {
                final List<TrusteeEntity> existing = dao.findTrustByMemberAndCircle(newTrusteeMember, request.getCircleId(), TrustLevel.getLevels(TrustLevel.ALL));

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

                    response = new ProcessCircleResponse();
                } else {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "The Member is already a trustee of the requested Circle.");
                }
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "No Member could be found with the given Id.");
            }
        }

        return response;
    }

    private ProcessCircleResponse alterTrustee(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        if (!Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
            final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());
            if (trustee != null) {
                final TrustLevel newTrustLevel = request.getTrustLevel();
                trustee.setTrustLevel(newTrustLevel);
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
            final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());
            if (trustee != null) {
                dao.delete(trustee);

                response = new ProcessCircleResponse();
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "The requested Trustee could not be found.");
            }
        } else {
            response = new ProcessCircleResponse(ReturnCode.AUTHORIZATION_WARNING, "Only a Circle Administrator may remove a Trustee.");
        }

        return response;
    }
}
