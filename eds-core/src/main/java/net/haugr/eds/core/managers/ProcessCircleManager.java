/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

import java.util.Arrays;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.requests.ProcessCircleRequest;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.core.enums.KeyAlgorithm;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.enums.Status;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.TrusteeDao;
import net.haugr.eds.core.model.entities.CircleEntity;
import net.haugr.eds.core.model.entities.DataTypeEntity;
import net.haugr.eds.core.model.entities.KeyEntity;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.MetadataEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessCircle request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessCircleManager extends AbstractManager<TrusteeDao, ProcessCircleResponse, ProcessCircleRequest> {

    private static final String NO_SUCH_CIRCLE = "No Circle could be found with the given Id.";

    public ProcessCircleManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new TrusteeDao(entityManager));
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
        return (member.getMemberRole() == MemberRole.ADMIN) || (trustees.getFirst().getTrustLevel() == TrustLevel.ADMIN);
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
            response = createCircle(owner, name, request.getCircleKey(), request.getAvatar(), request.getSecret());
        } else {
            response = createCircle(member, name, request.getCircleKey(), request.getAvatar(), request.getSecret());
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
     * @param avatar            Avatar of the Circle
     * @param secret            The secret of the first administrator in the Circle
     * @return Response Object with the new Circle ID
     */
    private ProcessCircleResponse createCircle(final MemberEntity circleAdmin, final String name, final String externalCircleKey, final String avatar, final String secret) {
        final KeyAlgorithm algorithm = settings.getSymmetricAlgorithm();
        // The Circle Secret: random 256 bits (PLAN.md, Crypto §3).
        final SecretEDSKey key = Crypto.generateSymmetricKey(algorithm);

        // The Circle is saved first, as its ID is part of the AADs.
        final CircleEntity circle = new CircleEntity();
        circle.setName(name);
        circle.setAvatar(isEmpty(avatar) ? null : avatar);
        // The first rotation of the Circle Secret is due a period from now
        // (PLAN.md, Crypto C6).
        circle.setRotateCircleKeyDue(settings.nextCircleSecretRotation());
        dao.save(circle);
        circle.setCircleKey(encryptExternalKey(key, circle, externalCircleKey));

        createRootFolder(circle);
        // The key row is saved before the secret is wrapped, as its ID is
        // part of the AADs.
        final KeyEntity keyEntity = new KeyEntity();
        keyEntity.setCircle(circle);
        keyEntity.setAlgorithm(algorithm);
        keyEntity.setStatus(Status.ACTIVE);
        dao.save(keyEntity);
        final String circleKey = wrapCircleKey(circleAdmin, circle, keyEntity, key);

        final TrusteeEntity trustee = new TrusteeEntity();
        trustee.setMember(circleAdmin);
        trustee.setCircle(circle);
        trustee.setKey(keyEntity);
        trustee.setTrustLevel(TrustLevel.ADMIN);
        trustee.setCircleKey(circleKey);
        // The first administrator's secret in the Circle (E2E.md).
        trustee.setExternalKey(sealTrusteeSecret(key, circle, circleAdmin, secret));
        dao.save(trustee);

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
        dao.save(entity);
    }

    private ProcessCircleResponse performNonCreateActions(final ProcessCircleRequest request) {
        return switch (request.getAction()) {
            case UPDATE -> updateCircle(request);
            case DELETE -> deleteCircle(request);
            case ROTATE -> rotateCircle(request);
            // Unreachable Code by design.
            default -> throw new IllegalActionException("Unsupported Action.");
        };
    }

    /**
     * <p>Updating a Circle means changing the name of it, as this is the only
     * valid information it has. Both the System Administrator and Circle
     * Administrator are allowed to perform this action. And as the initial
     * checks already have verified that it is either of the Administrators,
     * there are no other permission checks required.</p>
     *
     * <p>Only checks needed, is to verify that the name is not already taken
     * by any other circle.</p>
     *
     * @param request Request Object with ID and a new Name for the Circle
     * @return Response Object with the changed information
     */
    private ProcessCircleResponse updateCircle(final ProcessCircleRequest request) {
        final String externalId = request.getCircleId();

        final CircleEntity entity = dao.find(CircleEntity.class, externalId);
        throwConditionalException(entity == null,
                ReturnCode.IDENTIFICATION_WARNING, NO_SUCH_CIRCLE);

        // Without a new external key, the stored one stays, as the name does.
        if (request.getCircleKey() != null) {
            entity.setCircleKey(updateExternalCircleKey(request.getCircleKey()));
        }
        checkAndUpdateCircleName(entity, request.getCircleName());
        if (request.getAvatar() != null) {
            // An empty avatar removes it.
            entity.setAvatar(request.getAvatar().isEmpty() ? null : request.getAvatar());
        }
        dao.save(entity);

        return new ProcessCircleResponse(theCircle(entity) + " was successfully updated.");
    }

    private byte[] updateExternalCircleKey(final String externalKey) {
        byte[] encryptedKey = null;

        if (externalKey != null) {
            // The Circle is locked first, so the current Circle Secret is
            // used, also if a rotation started meanwhile; see PLAN.md, Crypto C6.
            final TrusteeEntity trustee = trustees.getFirst();
            dao.lockCircle(trustee.getCircle());
            dao.refresh(trustee);
            final SecretEDSKey circleKey = unwrapCircleKey(trustee);
            encryptedKey = encryptExternalKey(circleKey, trustee.getCircle(), externalKey);
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
     * <p>Rotates the Circle Secret now (PLAN.md, Crypto C6): the rotation is
     * set due, and starts in the background with the secret of the
     * requesting Circle administrator; for the System Administrator, who
     * needn't be a Trustee, at the next request of a Trustee.</p>
     *
     * @param request Request Object with the ID of the Circle
     * @return Response Object
     */
    private ProcessCircleResponse rotateCircle(final ProcessCircleRequest request) {
        final CircleEntity entity = dao.find(CircleEntity.class, request.getCircleId());
        throwConditionalException(entity == null,
                ReturnCode.IDENTIFICATION_WARNING, NO_SUCH_CIRCLE);

        dao.lockCircle(entity);
        entity.setRotateCircleKeyDue(Utilities.newDate());
        dao.save(entity);
        for (final TrusteeEntity trustee : trustees) {
            if (Objects.equals(trustee.getCircle().getId(), entity.getId())) {
                // Unwrapping hands the secret to the rotation.
                unwrapCircleKey(trustee);
            }
        }

        return new ProcessCircleResponse("The Circle Secret of the " + theCircle(entity).substring(4) + " is being rotated.");
    }

    /**
     * <p>Deleting an existing Circle is an irreversible process, by which all
     * the data, keys, and trustees will also be deleted alongside the Circle.</p>
     *
     * <p>The operation can only be performed by the System Administrator, due
     * to the nature of it.</p>
     *
     * @param request Request Object with the ID of the Circle to delete
     * @return Response Object with error information
     */
    private ProcessCircleResponse deleteCircle(final ProcessCircleRequest request) {
        final String externalId = request.getCircleId();
        final CircleEntity entity = dao.find(CircleEntity.class, externalId);
        throwConditionalException(entity == null,
                ReturnCode.IDENTIFICATION_WARNING, NO_SUCH_CIRCLE);

        // Before deleting the Circle, all associated Trustees must be deleted.
        // This is to avoid having orphaned Trustees.
        for (final TrusteeEntity trustee : dao.findTrusteesByCircle(entity.getExternalId())) {
            dao.delete(trustee);
        }
        // The content goes before the Circle Secrets it is encrypted with,
        // which go with the Circle.
        dao.deleteContentOfCircle(entity);
        dao.deleteCircle(entity);

        return new ProcessCircleResponse(theCircle(entity) + " has successfully been removed from EDS.");
    }

    /**
     * <p>Wrapper method to ensure that the circle is always presented the
     * same way. The method simply returns the Circle plus circle name.</p>
     *
     * @param circle Circle Entity to read the name from
     * @return String starting with 'the Circle' and then the circle name quoted
     */
    private static String theCircle(final CircleEntity circle) {
        return "The Circle '" + circle.getName() + '\'';
    }
}
