/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2020, JavaDog.io
 * mailto: cws AT JavaDog DOT io
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
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.common.TrustLevel;
import io.javadog.cws.api.requests.ProcessCircleRequest;
import io.javadog.cws.api.responses.ProcessCircleResponse;
import io.javadog.cws.core.enums.KeyAlgorithm;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.enums.Status;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.exceptions.IllegalActionException;
import io.javadog.cws.core.jce.Crypto;
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
 * @since CWS 1.0
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
        final Action action = request.getAction();
        final ProcessCircleResponse response;

        if (action == Action.CREATE) {
            // Anyone can create a Circle, only a Circle Administrator of the
            // Circle in question may perform other actions on Circles.
            response = createCircle(request);
        } else if (hasAdminRights()) {
            response = performNonCreateActions(request);
        } else {
            throw new AuthorizationException("Only a Circle Administrator may perform this action.");
        }

        return response;
    }

    /**
     * <p>Only a System or Circle Administrator may update or delete a Circle
     * of Trust. The pre-checks will ensure that at least one Circle of Trust
     * relationship exists at this point, hence the checks will always be
     * valid and not cause any errors.</p>
     *
     * @return True if requesting user has administrative permissions
     */
    private boolean hasAdminRights() {
        return (member.getMemberRole() == MemberRole.ADMIN) || (trustees.get(0).getTrustLevel() == TrustLevel.ADMIN);
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
        final String name = trim(request.getCircleName());
        final CircleEntity existing = dao.findCircleByName(name);

        throwConditionalException(existing != null,
                ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");

        final String memberId = request.getMemberId();
        final ProcessCircleResponse response;

        if ((memberId != null) && (member.getMemberRole() == MemberRole.ADMIN)) {
            final MemberEntity owner = dao.find(MemberEntity.class, memberId);
            throwConditionalException(owner == null,
                    ReturnCode.IDENTIFICATION_WARNING, "Cannot create a new Circle with a non-existing Circle Administrator.");
            response = createCircle(owner, name, request.getCircleKey());
        } else {
            response = createCircle(member, name, request.getCircleKey());
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
        final SecretCWSKey key = Crypto.generateSymmetricKey(algorithm);
        final PublicKey publicKey = crypto.dearmoringPublicKey(circleAdmin.getPublicKey());
        final PublicCWSKey cwsPublicKey = new PublicCWSKey(circleAdmin.getRsaAlgorithm(), publicKey);
        final String circleKey = Crypto.encryptAndArmorCircleKey(cwsPublicKey, key);
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

        final ProcessCircleResponse response = new ProcessCircleResponse(theCircle(circle) + " was successfully created.");
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

    private ProcessCircleResponse performNonCreateActions(final ProcessCircleRequest request) {
        final ProcessCircleResponse response;

        switch (request.getAction()) {
            case UPDATE:
                response = updateCircle(request);
                break;
            case DELETE:
                response = deleteCircle(request);
                break;
            default:
                // Unreachable Code by design.
                throw new IllegalActionException("Unsupported Action.");
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

        final CircleEntity entity = dao.find(CircleEntity.class, externalId);
        throwConditionalException(entity == null,
                ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");

        entity.setCircleKey(updateExternalCircleKey(request.getCircleKey()));
        checkAndUpdateCircleName(entity, request.getCircleName());
        dao.persist(entity);

        return new ProcessCircleResponse(theCircle(entity) + " was successfully updated.");
    }

    private byte[] updateExternalCircleKey(final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            final TrusteeEntity trustee = trustees.get(0);
            final SecretCWSKey circleKey = Crypto.extractCircleKey(trustee.getKey().getAlgorithm(), keyPair.getPrivate(), trustee.getCircleKey());
            encryptedKey = encryptExternalKey(circleKey, externalKey);
        }

        return encryptedKey;
    }

    private void checkAndUpdateCircleName(final CircleEntity entity, final String name) {
        final String trimmedNamed = trim(name);

        if (!isEmpty(trimmedNamed) && !Objects.equals(entity.getName(), trimmedNamed)) {
            final CircleEntity existing = dao.findCircleByName(trimmedNamed);
            throwConditionalException(existing != null,
                    ReturnCode.IDENTIFICATION_WARNING, "A Circle with the requested name already exists.");

            entity.setName(trimmedNamed);
        }
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
        final String externalId = request.getCircleId();
        final CircleEntity entity = dao.find(CircleEntity.class, externalId);
        throwConditionalException(entity == null,
                ReturnCode.IDENTIFICATION_WARNING, "No Circle could be found with the given Id.");
        dao.delete(entity);

        return new ProcessCircleResponse(theCircle(entity) + " has successfully been removed from CWS.");
    }

    /**
     * <p>Wrapper method to ensure that the circle is always presented the
     * same way. The method simply returns the Circle + circle name.</p>
     *
     * @param circle Circle Entity to read the name from
     * @return String starting with 'the Circle' and then the circle name quoted
     */
    private static String theCircle(final CircleEntity circle) {
        return "The Circle '" + circle.getName() + '\'';
    }
}
