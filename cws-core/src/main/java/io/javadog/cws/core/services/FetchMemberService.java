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
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.core.enums.MemberRole;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.AuthorizationException;
import io.javadog.cws.core.model.MemberDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.MemberEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Business Logic implementation for the CWS FetchMember request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberService extends Serviceable<MemberDao, FetchMemberResponse, FetchMemberRequest> {

    public FetchMemberService(final Settings settings, final EntityManager entityManager) {
        super(settings, new MemberDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse perform(final FetchMemberRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_MEMBER);
        Arrays.fill(request.getCredential(), (byte) 0);

        final FetchMemberResponse response = new FetchMemberResponse();

        if (request.getMemberId() != null) {
            // The request is for a specific Member
            if (Objects.equals(request.getMemberId(), member.getExternalId())) {
                // If it is the Member itself, then the CWS should just return
                // the information already retrieved as part of the
                // Authentication/Authorization logic.
                fetchYourself(response);
            } else {
                final MemberEntity requestedMember = dao.find(MemberEntity.class, request.getMemberId());
                if (requestedMember != null) {
                    // Request is for a different Member...
                    fetchSomeoneElse(response, requestedMember);
                } else {
                    // No such Account exist, will simply return with an error.
                    response.setReturnCode(ReturnCode.IDENTIFICATION_WARNING);
                    response.setReturnMessage("The requested Member cannot be found.");
                }
            }
        } else {
            final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "name");
            response.setMembers(convertMembers(members));
        }

        return response;
    }

    private void fetchYourself(final FetchMemberResponse response) {
        addMemberToResponse(response, member);

        // The System Administrator cannot be part of any Circles, so
        // no need to add these.
        if (member.getMemberRole() != MemberRole.ADMIN) {
            final List<Circle> circles = new ArrayList<>(trustees.size());
            for (final TrusteeEntity trustee : trustees) {
                final String externalKey = decryptExternalKey(trustee);
                circles.add(convert(trustee.getCircle(), externalKey));
            }
            response.setCircles(circles);
        }
    }

    /**
     * <p>When requesting to see information about a different Member, a set of
     * rules must be applied. Generally, the System Administrator can always see
     * all Accounts - however, for other Members, the rules to apply must follow
     * the rules defined via the Settings.</p>
     *
     * <p>If the {@link io.javadog.cws.core.enums.StandardSetting#EXPOSE_ADMIN}
     * flag is set, then a Member may view the System Administrator Account,
     * however this setting is disabled per default.</p>
     *
     * <p>If the {@link io.javadog.cws.core.enums.StandardSetting#SHOW_TRUSTEES}
     * flag is set, then the Member may view all Circles, which the Other Member
     * belongs to, and not just the ones both Members share. By default, this
     * Settings is set to True.</p>
     *
     * @param response  Response Object to fill
     * @param requested Requested Member to see if may be viewed
     * @see io.javadog.cws.core.enums.StandardSetting#EXPOSE_ADMIN
     * @see io.javadog.cws.core.enums.StandardSetting#SHOW_TRUSTEES
     */
    private void fetchSomeoneElse(final FetchMemberResponse response, final MemberEntity requested) {
        if (member.getMemberRole() == MemberRole.ADMIN) {
            addMemberToResponse(response, requested);
            response.setCircles(findCirclesMemberBelongsTo(requested));
        } else {
            if (requested.getMemberRole() == MemberRole.ADMIN) {
                if (settings.getExposeAdmin()) {
                    addMemberToResponse(response, requested);
                } else {
                    throw new AuthorizationException("Not Authorized to access this information.");
                }
            } else {
                addMemberToResponse(response, requested);

                if (settings.getShareTrustees()) {
                    response.setCircles(findCirclesMemberBelongsTo(requested));
                } else {
                    response.setCircles(findSharedCircles(member, requested));
                }
            }
        }
    }

    private static void addMemberToResponse(final FetchMemberResponse response, final MemberEntity memberEntity) {
        final List<Member> members = new ArrayList<>(1);
        members.add(convert(memberEntity));
        response.setMembers(members);
    }

    private static Member convert(final MemberEntity entity) {
        final Member member = new Member();

        member.setMemberId(entity.getExternalId());
        member.setAccountName(entity.getName());
        member.setPublicKey(entity.getMemberKey());
        member.setAdded(entity.getAdded());

        return member;
    }

    private List<Member> convertMembers(final List<MemberEntity> entities) {
        final List<Member> circles = new ArrayList<>(entities.size());
        final boolean exposeAdmin = settings.getExposeAdmin();

        for (final MemberEntity entity : entities) {
            // If the Settings to expose the System Administrator is set to
            // true, then we'll also add this, however by default we will
            // otherwise skip the System Administrator.
            if (entity.getMemberRole() == MemberRole.ADMIN) {
                if (exposeAdmin) {
                    circles.add(convert(entity));
                }
            } else {
                circles.add(convert(entity));
            }
        }

        return circles;
    }

    private List<Circle> findCirclesMemberBelongsTo(final MemberEntity requested) {
        final List<CircleEntity> entities = dao.findCirclesForMember(requested);
        final List<Circle> circles = new ArrayList<>(entities.size());
        for (final CircleEntity entity : entities) {
            final Circle circle = convert(entity, null);
            circles.add(circle);
        }

        return circles;
    }

    private List<Circle> findSharedCircles(final MemberEntity current, final MemberEntity requested) {
        final List<TrusteeEntity> circles = dao.findCirclesBothBelongTo(current, requested);

        return convertCircles(circles);
    }

    private List<Circle> convertCircles(final List<TrusteeEntity> trustees) {
        final List<Circle> circles = new ArrayList<>(trustees.size());
        for (final TrusteeEntity trustee : trustees) {
            final String externalKey = decryptExternalKey(trustee);
            circles.add(convert(trustee.getCircle(), externalKey));
        }

        return circles;
    }
}
