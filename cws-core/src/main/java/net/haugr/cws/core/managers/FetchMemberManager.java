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

import net.haugr.cws.api.common.MemberRole;
import net.haugr.cws.api.common.ReturnCode;
import net.haugr.cws.api.dtos.Circle;
import net.haugr.cws.api.dtos.Member;
import net.haugr.cws.api.requests.FetchMemberRequest;
import net.haugr.cws.api.responses.FetchMemberResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.model.MemberDao;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.entities.CircleEntity;
import net.haugr.cws.core.model.entities.MemberEntity;
import net.haugr.cws.core.model.entities.TrusteeEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import net.haugr.cws.core.enums.StandardSetting;

/**
 * <p>Business Logic implementation for the CWS FetchMember request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchMemberManager extends AbstractManager<MemberDao, FetchMemberResponse, FetchMemberRequest> {

    public FetchMemberManager(final Settings settings, final EntityManager entityManager) {
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

        final var response = new FetchMemberResponse();

        if (request.getMemberId() != null) {
            // The request is for a specific Member
            if (Objects.equals(request.getMemberId(), member.getExternalId())) {
                // If it is the Member itself, then the CWS should just return
                // the information already retrieved as part of the
                // Authentication/Authorization logic.
                addMemberToResponse(response, member);
                response.setCircles(convertCircles(trustees));
            } else {
                final MemberEntity requestedMember = dao.find(MemberEntity.class, request.getMemberId());
                // No such Account exist, will simply return with an error.
                throwConditionalNullException(requestedMember,
                        ReturnCode.IDENTIFICATION_WARNING, "The requested Member cannot be found.");

                // Request is for a different Member...
                fetchSomeoneElse(response, requestedMember);
            }
        } else {
            final List<MemberEntity> members = dao.findAllAscending(MemberEntity.class, "name");
            response.setMembers(convertMembers(members));
        }

        return response;
    }

    /**
     * <p>When requesting to see information about a different Member, a set of
     * rules must be applied. Generally, the System Administrator can always see
     * all Accounts - however, for other Members, the rules to apply must follow
     * the rules defined via the Settings.</p>
     *
     * <p>If the {@link StandardSetting#SHOW_TRUSTEES}
     * flag is set, then the Member may view all Circles, which the Other Member
     * belongs to, and not just the ones both Members share. By default, this
     * Settings is set to True.</p>
     *
     * @param response  Response Object to fill
     * @param requested Requested Member to see if it may be viewed
     * @see StandardSetting#SHOW_TRUSTEES
     */
    private void fetchSomeoneElse(final FetchMemberResponse response, final MemberEntity requested) {
        if (member.getMemberRole() == MemberRole.ADMIN) {
            addMemberToResponse(response, requested);
            response.setCircles(findCirclesMemberBelongsTo(requested));
        } else {
            addMemberToResponse(response, requested);

            if (settings.hasShareTrustees()) {
                response.setCircles(findCirclesMemberBelongsTo(requested));
            } else {
                response.setCircles(findSharedCircles(member, requested));
            }
        }
    }

    private static void addMemberToResponse(final FetchMemberResponse response, final MemberEntity memberEntity) {
        final List<Member> members = new ArrayList<>(1);
        members.add(convert(memberEntity));
        response.setMembers(members);
    }

    private static List<Member> convertMembers(final Collection<MemberEntity> entities) {
        final List<Member> circles = new ArrayList<>(entities.size());

        for (final MemberEntity entity : entities) {
            circles.add(convert(entity));
        }

        return circles;
    }

    private static Member convert(final MemberEntity entity) {
        final var member = new Member();

        member.setMemberId(entity.getExternalId());
        member.setAccountName(entity.getName());
        member.setMemberRole(entity.getMemberRole());
        member.setPublicKey(entity.getMemberKey());
        member.setAdded(entity.getAdded());

        return member;
    }

    private List<Circle> findCirclesMemberBelongsTo(final MemberEntity requested) {
        final List<CircleEntity> entities = dao.findCirclesForMember(requested);
        final List<Circle> circles = new ArrayList<>(entities.size());
        for (final CircleEntity entity : entities) {
            final var circle = convert(entity, null);
            circles.add(circle);
        }

        return circles;
    }

    private List<Circle> findSharedCircles(final MemberEntity current, final MemberEntity requested) {
        final List<TrusteeEntity> circles = dao.findCirclesBothBelongTo(current, requested);

        return convertCircles(circles);
    }

    private List<Circle> convertCircles(final Collection<TrusteeEntity> trustees) {
        final List<Circle> circles = new ArrayList<>(trustees.size());
        for (final TrusteeEntity trustee : trustees) {
            final String externalKey = decryptExternalKey(trustee);
            circles.add(convert(trustee.getCircle(), externalKey));
        }

        return circles;
    }
}
