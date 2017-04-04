/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.requests.FetchMemberRequest;
import io.javadog.cws.api.responses.FetchMemberResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Servicable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchMemberService extends Servicable<FetchMemberResponse, FetchMemberRequest> {

    public FetchMemberService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchMemberResponse perform(final FetchMemberRequest request) {
        verifyRequest(request, Permission.FETCH_MEMBER);

        final FetchMemberResponse response = new FetchMemberResponse();

        if (request.getMemberId() != null) {
            final MemberEntity requestedMember = dao.findMemberByExternalId(request.getMemberId());
            if (requestedMember != null) {
                final List<Member> members = new ArrayList<>(1);
                members.add(convert(requestedMember));
                response.setMembers(members);

                response.setCircles(findCirclesMemberBelongsTo(requestedMember));
            } else {
                response.setReturnCode(Constants.IDENTIFICATION_WARNING);
                response.setReturnMessage("The requested Member cannot be found.");
            }
        } else {
            final List<MemberEntity> members = dao.findAllMembers();
            response.setMembers(convertMembers(members));
        }

        return response;
    }

    private static Member convert(final MemberEntity entity) {
        final Authentication authentication = new Authentication();
        authentication.setAccount(entity.getName());
        final Member member = new Member();

        member.setId(entity.getExternalId());
        member.setAuthentication(authentication);
        member.setAdded(entity.getCreated());

        return member;
    }

    private static List<Member> convertMembers(final List<MemberEntity> entities) {
        final List<Member> circles = new ArrayList<>(entities.size());

        for (final MemberEntity entity : entities) {
            circles.add(convert(entity));
        }

        return circles;
    }

    private List<Circle> findCirclesMemberBelongsTo(final MemberEntity member) {
        final List<CircleEntity> circles = dao.findCirclesForMember(member);

        return convertCircles(circles);
    }

    private static List<Circle> convertCircles(final List<CircleEntity> entities) {
        final List<Circle> circles = new ArrayList<>(entities.size());

        for (final CircleEntity entity : entities) {
            circles.add(convert(entity));
        }

        return circles;
    }

    private static Circle convert(final CircleEntity entity) {
        final Circle circle = new Circle();

        circle.setId(entity.getExternalId());
        circle.setName(entity.getName());
        circle.setCreated(entity.getCreated());

        return circle;
    }
}
