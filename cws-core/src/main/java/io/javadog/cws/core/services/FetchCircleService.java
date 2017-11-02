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
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Serviceable;
import io.javadog.cws.model.entities.CircleEntity;
import io.javadog.cws.model.entities.MemberEntity;
import io.javadog.cws.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchCircleService extends Serviceable<FetchCircleResponse, FetchCircleRequest> {

    public FetchCircleService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse perform(final FetchCircleRequest request) {
        verifyRequest(request, Permission.FETCH_CIRCLE);
        final FetchCircleResponse response = new FetchCircleResponse();
        final String circleId = request.getCircleId();

        if (circleId != null) {
            // First retrieve the Circle via the ExternalId given. If no Circle
            // is found, the DAO will throw an Exception.
            final CircleEntity circle = dao.find(CircleEntity.class, circleId);

            if (circle != null) {
                // The Settings and the Requesting Member are both important when
                // trying to ascertain if the Circle Trustees may be retrieved. If
                // the requesting Member is the System Administrator, then all
                // information may be retrieved. If the Settings allow it, then all
                // information may be retrieved. However, if the request is made by
                // anyone else than the System Administrator and the Settings
                // doesn't allow exposing information, then we will only show
                // information about Circles, which the requesting Member is allowed
                // to access.
                final List<TrusteeEntity> members;
                if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName()) || settings.getShareTrustees()) {
                    members = dao.findTrusteesByCircle(circle);
                } else {
                    // Regardless of the settings and requesting Member, we should
                    // be as tolerant as possible, and if the Member is not allowed
                    // to see the Circle details, then it is simply omitted.
                    members = findTrusteeByCircle(circle);
                }
                response.setTrustees(convertTrustees(members));
                final List<Circle> circles = new ArrayList<>(1);
                circles.add(convert(circle));
                response.setCircles(circles);
            } else {
                response.setReturnCode(ReturnCode.IDENTIFICATION_WARNING);
                response.setReturnMessage("The requested Circle cannot be found.");
            }
        } else {
            final List<CircleEntity> circles = dao.findAllCircles();
            response.setCircles(convertCircles(circles));
        }

        return response;
    }

    /**
     * <p>If the Circle being sought is one, which the Member belongs to, then
     * the details will be returned, otherwise an empty list is returned.</p>
     *
     * @param circle Circle Entity
     * @return List of Trustees for the given Circle, or empty List
     */
    private List<TrusteeEntity> findTrusteeByCircle(final CircleEntity circle) {
        final List<TrusteeEntity> found = new ArrayList<>(0);

        for (final TrusteeEntity trusteeEntity : trustees) {
            if (Objects.equals(trusteeEntity.getCircle().getId(), circle.getId())) {
                found.addAll(dao.findTrusteesByCircle(circle));
                break;
            }
        }

        return found;
    }

    private static List<Trustee> convertTrustees(final List<TrusteeEntity> entities) {
        final List<Trustee> trustees = new ArrayList<>(entities.size());

        for (final TrusteeEntity entity : entities) {
            trustees.add(convert(entity));

        }

        return trustees;
    }

    private static List<Circle> convertCircles(final List<CircleEntity> entities) {
        final List<Circle> circles = new ArrayList<>(entities.size());

        for (final CircleEntity entity : entities) {
            circles.add(convert(entity));
        }

        return circles;
    }

    private static Trustee convert(final TrusteeEntity entity) {
        final Trustee trustee = new Trustee();

        trustee.setMember(convert(entity.getMember()));
        trustee.setCircle(convert(entity.getCircle()));
        trustee.setTrustLevel(entity.getTrustLevel());
        trustee.setChanged(entity.getModified());
        trustee.setSince(entity.getCreated());

        return trustee;
    }

    private static Member convert(final MemberEntity entity) {
        final Member member = new Member();

        member.setMemberId(entity.getExternalId());
        member.setAccountName(entity.getName());
        member.setAdded(entity.getCreated());

        return member;
    }

    private static Circle convert(final CircleEntity entity) {
        final Circle circle = new Circle();

        circle.setCircleId(entity.getExternalId());
        circle.setCircleName(entity.getName());
        circle.setCreated(entity.getCreated());

        return circle;
    }
}
