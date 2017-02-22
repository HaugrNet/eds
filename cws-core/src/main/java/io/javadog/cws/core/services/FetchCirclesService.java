package io.javadog.cws.core.services;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.dtos.Authentication;
import io.javadog.cws.api.dtos.Circle;
import io.javadog.cws.api.dtos.Member;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchCircleRequest;
import io.javadog.cws.api.responses.FetchCircleResponse;
import io.javadog.cws.common.Settings;
import io.javadog.cws.core.Permission;
import io.javadog.cws.core.Servicable;
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
public final class FetchCirclesService extends Servicable<FetchCircleResponse, FetchCircleRequest> {

    public FetchCirclesService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchCircleResponse perform(final FetchCircleRequest request) {
        verifyRequest(request, Permission.FETCH_CIRCLE);
        final FetchCircleResponse response = new FetchCircleResponse();

        if (request.getCircleId() != null) {
            // Our check here should be made with both the Requesting Member and
            // the Id of the Circle. If the requesting Member is the System
            // Administrator, then we'll make a lookup and fetch all information
            // about current Trustees - otherwise, an additional check is made,
            // to ensure that only an existing Trustee may see who else have
            // access to the Circle.
            final List<TrusteeEntity> members;
            if (Objects.equals(Constants.ADMIN_ACCOUNT, member.getName())) {
                members = dao.findTrusteesByCircle(request.getCircleId());
            } else {
                final Long circleId = findCircleId(request.getCircleId());
                members = dao.findTrusteesByCircle(circleId);
            }
            response.setTrustees(convertTrustees(members));
            final List<Circle> circles = new ArrayList<>(1);
            circles.add(convert(members.get(0).getCircle()));
            response.setCircles(circles);
        } else {
            final List<CircleEntity> circles = dao.findAllCircles();
            response.setCircles(convertCircles(circles));
        }

        return response;
    }

    private Long findCircleId(final String externalCircleId) {
        Long circleId = null;

        for (final TrusteeEntity entity : trustees) {
            if (Objects.equals(entity.getCircle().getExternalId(), externalCircleId)) {
                circleId = entity.getCircle().getId();
            }
        }

        return circleId;
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
        trustee.setModified(entity.getModified());
        trustee.setSince(entity.getCreated());

        return trustee;
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

    private static Circle convert(final CircleEntity entity) {
        final Circle circle = new Circle();

        circle.setId(entity.getExternalId());
        circle.setName(entity.getName());
        circle.setCreated(entity.getCreated());

        return circle;
    }
}
