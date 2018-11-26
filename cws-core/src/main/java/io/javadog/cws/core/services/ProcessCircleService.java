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

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.jce.PublicCWSKey;
import io.javadog.cws.core.jce.SecretCWSKey;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.DataTypeEntity;
import io.javadog.cws.core.model.entities.KeyEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.MetadataEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS ProcessCircle request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class ProcessCircleService extends Serviceable<CommonDao, ProcessCircleResponse, ProcessCircleRequest> {

    public ProcessCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessCircleResponse perform(final ProcessCircleRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_CIRCLE);
        Arrays.fill(request.getCredential(), (byte) 0);
        final ProcessCircleResponse response;
        final Action action = request.getAction();

        if (action == Action.CREATE) {
            // Anyone cay create a Circle, only a Circle Administrator of the
            // Circle in question may perform other actions on Circles.
            response = createCircle(request);
        } else {
            if ((member.getMemberRole() == MemberRole.ADMIN) || (trustees.get(0).getTrustLevel() == TrustLevel.ADMIN)) {
                switch (request.getAction()) {
                    case UPDATE:
                        response = updateCircle(request);
                        break;
                    case DELETE:
                        response = deleteCircle(request);
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

        final String name = trim(request.getCircleName());
        final CircleEntity existing = dao.findCircleByName(name);
        if (existing != null) {
            response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
        } else {
            if (member.getMemberRole() == MemberRole.ADMIN) {
                // The administrator is creating a new Circle, which requires that
                // a Member Id is provided as the new Circle Administrator.
                final MemberEntity owner = dao.find(MemberEntity.class, request.getMemberId());
                if (owner == null) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "Cannot create a new Circle with a non-existing Circle Administrator.");
                } else if (Objects.equals(owner.getName(), Constants.ADMIN_ACCOUNT)) {
                    response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "It is not allowed for the System Administrator to be part of a Circle.");
                } else {
                    response = createCircle(owner, name, request.getCircleKey());
                }
            } else {
                response = createCircle(member, name, request.getCircleKey());
            }
        }

        return response;
    }

    /**
     * <p>Creates the actual Circle with given Circle Name and Admin. The Circle
     * will also be given a new encryption key and a default root folder for
     * storing of all Data Objects.</p>
     *
     * @param circleAdmin       The initial Circle Administrator
     * @param name              The name of the new Circle
     * @param externalCircleKey External Circle Key
     * @return Response Object with the new Circle Id
     */
    private ProcessCircleResponse createCircle(final MemberEntity circleAdmin, final String name, final String externalCircleKey) {
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        final SecretCWSKey key = crypto.generateSymmetricKey(algorithm);
        final PublicKey publicKey = crypto.dearmoringPublicKey(circleAdmin.getPublicKey());
        final PublicCWSKey cwsPublicKey = new PublicCWSKey(circleAdmin.getRsaAlgorithm(), publicKey);
        final String circleKey = crypto.encryptAndArmorCircleKey(cwsPublicKey, key);
        final byte[] externalKey = encryptExternalKey(key, externalCircleKey);

        final CircleEntity circle = new CircleEntity();
        circle.setName(name);
        circle.setCircleKey(externalKey);
        dao.persist(circle);

        createRootFolder(circle);
        final KeyEntity keyEntity = new KeyEntity();
        keyEntity.setAlgorithm(algorithm);
        keyEntity.setStatus(Status.ACTIVE);
        dao.persist(keyEntity);

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
        final ProcessCircleResponse response;

        final CircleEntity entity = dao.find(CircleEntity.class, externalId);

        if (entity != null) {
            entity.setCircleKey(updateExternalCircleKey(request.getCircleKey()));
            response = checkAndUpdateCircleName(entity, trim(request.getCircleName()));

            dao.persist(entity);
        } else {
            response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");
        }

        return response;
    }

    private byte[] updateExternalCircleKey(final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            final TrusteeEntity trustee = trustees.get(0);
            final SecretCWSKey circleKey = crypto.extractCircleKey(trustee.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
            encryptedKey = encryptExternalKey(circleKey, externalKey);
        }

        return encryptedKey;
    }

    private ProcessCircleResponse checkAndUpdateCircleName(final CircleEntity entity, final String name) {
        ProcessCircleResponse response = new ProcessCircleResponse();

        if (!isEmpty(name) && !Objects.equals(entity.getName(), name)) {
            final CircleEntity existing = dao.findCircleByName(name);
            if (existing == null) {
                entity.setName(name);
            } else {
                response = new ProcessCircleResponse(ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");
            }
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

        if (member.getMemberRole() == MemberRole.ADMIN) {
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
}
