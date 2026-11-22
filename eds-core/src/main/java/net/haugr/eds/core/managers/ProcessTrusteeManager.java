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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.common.TrustLevel;
import net.haugr.eds.api.common.Utilities;
import net.haugr.eds.api.dtos.TrusteeSecret;
import net.haugr.eds.api.requests.ProcessTrusteeRequest;
import net.haugr.eds.api.responses.ProcessTrusteeResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.AuthorizationException;
import net.haugr.eds.core.exceptions.EDSException;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.exceptions.IllegalActionException;
import net.haugr.eds.core.jce.Crypto;
import net.haugr.eds.core.jce.SecretEDSKey;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.TrusteeDao;
import net.haugr.eds.core.model.entities.MemberEntity;
import net.haugr.eds.core.model.entities.TrusteeEntity;
import net.haugr.eds.core.model.entities.CircleEntity;

/**
 * <p>Business Logic implementation for the EDS ProcessTrustee request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessTrusteeManager extends AbstractManager<TrusteeDao, ProcessTrusteeResponse, ProcessTrusteeRequest> {

    public ProcessTrusteeManager(final Settings settings, final EntityManager entityManager) {
        super(settings, new TrusteeDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    // verifyRequest() refuses a null request before it is used.
    @Override
    @SuppressWarnings("findbugs:NP_NULL_ON_SOME_PATH")
    public ProcessTrusteeResponse perform(final ProcessTrusteeRequest request) {
        if ((request != null) && (request.getAction() == Action.REMOVE)) {
            return removeOrLeave(request);
        }
        if ((request != null) && (request.getAction() == Action.ALTER) && (request.getTrustLevel() == null) && (request.getSecret() != null)) {
            return alterSecret(request);
        }

        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.PROCESS_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        return switch (request.getAction()) {
            case ADD -> addTrustee(request);
            case ALTER -> alterTrustee(request);
            case SECRETS -> alterSecrets(request);
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

        // While the Circle Secret is rotated, the new Trustee gets both the
        // current and the retiring secret (PLAN.md, Crypto C6). The Circle is
        // locked first, so a rotation which starts meanwhile is seen.
        final TrusteeEntity admin = lockedOwnTrustee(trustees.getFirst());
        final TrustLevel trustLevel = request.getTrustLevel();
        final TrusteeEntity trustee = new TrusteeEntity();
        trustee.setMember(newTrusteeMember);
        trustee.setCircle(admin.getCircle());
        trustee.setTrustLevel(trustLevel);

        final SecretEDSKey circleKey = unwrapCircleKey(admin);
        grantCircleSecrets(trustee, admin, circleKey);
        trustee.setExternalKey(sealTrusteeSecret(circleKey, admin.getCircle(), newTrusteeMember, request.getSecret()));

        dao.save(trustee);
        return new ProcessTrusteeResponse("The Member '" + trustee.getMember().getName() + "' was successfully added as trustee to '" + trustee.getCircle().getName() + "'.");
    }

    private ProcessTrusteeResponse alterTrustee(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = findTrusteeForCircleAndMember(request);
        // The whole row is written, so it is read anew under the lock of its
        // Circle: a rotation of the Circle Secret (PLAN.md, Crypto C6) which
        // started meanwhile must not be undone.
        dao.lockCircle(trustee.getCircle());
        dao.refresh(trustee);
        if (request.getSecret() != null) {
            // Sealing needs the Circle Secret, which the System Administrator
            // doesn't have without being a Trustee.
            if (trustees.isEmpty()) {
                throw new IllegalActionException("It is not possible to set a secret in a Circle, without membership.");
            }
            final TrusteeEntity admin = lockedOwnTrustee(trustees.getFirst());
            trustee.setExternalKey(sealTrusteeSecret(unwrapCircleKey(admin), admin.getCircle(), trustee.getMember(), request.getSecret()));
        }
        trustee.setTrustLevel(request.getTrustLevel());
        dao.save(trustee);

        return new ProcessTrusteeResponse("The Trustee '" + trustee.getMember().getName() + "' has successfully been given the trust level '"
                + trustee.getTrustLevel() + "' in the Circle '" + trustee.getCircle().getName() + "'.");
    }

    /**
     * Sets the secret of a Trustee and nothing else (E2E.md). Every Trustee
     * may set their own, whatever their trust level; the secret of another
     * Trustee needs the trust level ADMIN in the Circle. The requesting
     * Member must be a Trustee of the Circle, as the secret is sealed with
     * the Circle Secret.
     */
    private ProcessTrusteeResponse alterSecret(final ProcessTrusteeRequest request) {
        verifyRequest(request, Permission.FETCH_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final TrusteeEntity found = dao.findTrusteeByCircleAndMember(request.getCircleId(), member.getExternalId());
        if (found == null) {
            throw new AuthorizationException("The requesting Account is not a Trustee of the Circle.");
        }
        final TrusteeEntity own = lockedOwnTrustee(found);
        final boolean self = Objects.equals(request.getMemberId(), member.getExternalId());
        if (!self && !TrustLevel.getLevels(TrustLevel.ADMIN).contains(own.getTrustLevel())) {
            throw new AuthorizationException("Only an administrator of the Circle may set the secret of another Trustee.");
        }

        final TrusteeEntity trustee = self ? own : findTrusteeForCircleAndMember(request);
        trustee.setExternalKey(sealTrusteeSecret(unwrapCircleKey(own), own.getCircle(), trustee.getMember(), request.getSecret()));
        dao.save(trustee);

        return new ProcessTrusteeResponse("The secret of the Trustee '" + trustee.getMember().getName() + "' in the Circle '" + trustee.getCircle().getName() + "' was successfully set.");
    }

    /**
     * Sets the secrets of several Trustees of a Circle, and optionally the
     * Circle's external key, in one step (E2E.md): either all are set or
     * none, so a change of a key which clients keep in them can't end half
     * done. Every Member named must be a Trustee of the Circle.
     */
    private ProcessTrusteeResponse alterSecrets(final ProcessTrusteeRequest request) {
        if (trustees.isEmpty()) {
            throw new EDSException(ReturnCode.ILLEGAL_ACTION, "It is not possible to set secrets in a Circle, without membership.");
        }
        // Nobody joins or leaves while the secrets are set, and a rotation
        // of the Circle Secret which started meanwhile is seen.
        lockOwnTrustee(trustees.getFirst());

        // Everything is checked before anything is changed.
        final List<TrusteeSecret> secrets = (request.getSecrets() != null) ? request.getSecrets() : List.of();
        final List<TrusteeEntity> found = new ArrayList<>(secrets.size());
        for (final TrusteeSecret entry : secrets) {
            final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), entry.getMemberId());
            if (trustee == null) {
                throw new IdentificationException("The Member '" + entry.getMemberId() + "' is not a Trustee of the Circle; no secret was set.");
            }
            found.add(trustee);
        }

        final TrusteeEntity admin = trustees.getFirst();
        final SecretEDSKey circleKey = unwrapCircleKey(admin);
        final CircleEntity circle = admin.getCircle();
        if (request.getCircleKey() != null) {
            circle.setCircleKey(encryptExternalKey(circleKey, circle, request.getCircleKey()));
            dao.save(circle);
        }
        for (int i = 0; i < found.size(); i++) {
            final TrusteeEntity trustee = found.get(i);
            trustee.setExternalKey(sealTrusteeSecret(circleKey, circle, trustee.getMember(), secrets.get(i).getSecret()));
            dao.save(trustee);
        }

        return new ProcessTrusteeResponse("The secrets of " + found.size() + " Trustee(s) of the Circle '" + circle.getName() + "' were successfully set.");
    }

    /**
     * Every Trustee may leave a Circle, i.e. remove themselves, which only
     * needs the membership; removing someone else needs the trust level ADMIN
     * (or the System Administrator), as before. Even the last administrator
     * may leave; the System Administrator can then make another Trustee an
     * administrator. The last Trustee can't: see {@link #removeTrustee}.
     */
    private ProcessTrusteeResponse removeOrLeave(final ProcessTrusteeRequest request) {
        verifyRequest(request, Permission.FETCH_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final boolean leaving = Objects.equals(request.getMemberId(), member.getExternalId());
        if (!leaving && (member.getMemberRole() != MemberRole.ADMIN)) {
            final boolean administrator = trustees.stream()
                    .anyMatch(t -> Objects.equals(t.getCircle().getExternalId(), request.getCircleId())
                            && TrustLevel.getLevels(TrustLevel.ADMIN).contains(t.getTrustLevel()));
            if (!administrator) {
                throw new AuthorizationException("The requesting Account is not permitted to " + Permission.PROCESS_TRUSTEE.getDescription());
            }
        }

        return removeTrustee(request);
    }

    /**
     * The last Trustee of a Circle holds the only copy of the Circle Secret
     * which can be unwrapped. Without it nobody, not even the System
     * Administrator, could read the Circle's data again, while it still
     * counts against the storage ceiling. So the last Trustee stays, also
     * when the System Administrator asks; the Circle is deleted instead.
     */
    private ProcessTrusteeResponse removeTrustee(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = findTrusteeForCircleAndMember(request);
        // Two Trustees leaving at the same time mustn't both be the one
        // before the last.
        dao.lockCircle(trustee.getCircle());
        if (dao.findTrusteesByCircle(trustee.getCircle().getExternalId()).size() <= 1) {
            throw new IllegalActionException("The last Member of the Circle '" + trustee.getCircle().getName() + "' can't leave it, as nobody could read its data again. Delete the Circle instead.");
        }
        dao.delete(trustee);
        // The Circle Secret is rotated now, so the one who left can't use
        // what they may have kept of it (PLAN.md, Crypto C6).
        final CircleEntity circle = trustee.getCircle();
        circle.setRotateCircleKeyDue(Utilities.newDate());
        dao.save(circle);

        return new ProcessTrusteeResponse("The Trustee '" + trustee.getMember().getName() + "' was successfully removed from the Circle '" + trustee.getCircle().getName() + "'.");
    }

    /**
     * Locks the Circle of the requesting Member's Trustee row, and reads the
     * row anew: a rotation of the Circle Secret which started meanwhile has
     * changed it (PLAN.md, Crypto C6), and what is sealed now must use the
     * current secret.
     *
     * @param own The requesting Member's Trustee row
     * @return The row, as it is now
     */
    private TrusteeEntity lockedOwnTrustee(final TrusteeEntity own) {
        lockOwnTrustee(own);
        return own;
    }

    /** As {@link #lockedOwnTrustee}, for a caller which uses the row later. */
    private void lockOwnTrustee(final TrusteeEntity own) {
        dao.lockCircle(own.getCircle());
        dao.refresh(own);
    }

    private TrusteeEntity findTrusteeForCircleAndMember(final ProcessTrusteeRequest request) {
        final TrusteeEntity trustee = dao.findTrusteeByCircleAndMember(request.getCircleId(), request.getMemberId());

        if (trustee == null) {
            throw new IdentificationException("The requested Trustee could not be found.");
        }

        return trustee;
    }
}
