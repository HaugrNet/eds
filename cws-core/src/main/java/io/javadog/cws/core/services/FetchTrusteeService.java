/*
 * CWS, Cryptographic Web Store - open source Cryptographic Storage system.
 * Copyright (C) 2016-2019, JavaDog.io
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

import io.javadog.cws.api.common.MemberRole;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.IdentificationException;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.TrusteeDao;
import io.javadog.cws.core.model.entities.TrusteeEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * <p>Business Logic implementation for the CWS FetchTrustee request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchTrusteeService extends Serviceable<TrusteeDao, FetchTrusteeResponse, FetchTrusteeRequest> {

    public FetchTrusteeService(final Settings settings, final EntityManager entityManager) {
        super(settings, new TrusteeDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse perform(final FetchTrusteeRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        final List<TrusteeEntity> trustees;
        final String memberId = request.getMemberId();
        final String circleId = request.getCircleId();

        // Let the check-hell commence. Three factors are important, the
        // MemberId, CircleId and if the Member is also a System Administrator.
        if (circleId != null) {
            trustees = checkTrusteesForSpecificCircle(memberId, circleId);
        } else if (memberId != null) {
            trustees = checkTrusteesForSpecificMember(memberId);
        } else {
            trustees = dao.findTrusteesByMember(member.getExternalId());
        }

        final List<Trustee> currentTrustees = new ArrayList<>(trustees.size());
        for (final TrusteeEntity entity : trustees) {
            currentTrustees.add(convert(entity));
        }

        final FetchTrusteeResponse response = new FetchTrusteeResponse();
        response.setTrustees(currentTrustees);

        return response;
    }

    private List<TrusteeEntity> checkTrusteesForSpecificCircle(final String memberId, final String circleId) {
        final List<TrusteeEntity> trustees;

        // The pre-checks will prevent that a non-System Administrator
        // can access information about Circles of Trust for Circle's
        // where there is no relation.
        if (memberId != null) {
            trustees = dao.findTrusteesByMemberAndCircle(memberId, circleId);
            if (trustees.isEmpty()) {
                throw new IdentificationException("Unable to find any relation between given Circle & Member Id's.");
            }
        } else {
            trustees = dao.findTrusteesByCircle(circleId);
            if (trustees.isEmpty()) {
                throw new IdentificationException("The requested Circle cannot be found.");
            }
        }

        return trustees;
    }

    private List<TrusteeEntity> checkTrusteesForSpecificMember(final String memberId) {
        final List<TrusteeEntity> trustees;

        if (member.getMemberRole() == MemberRole.ADMIN) {
            // If information about a specific member is inquired,
            // then it is normally only permitted to be made by a
            // System Administrator.
            trustees = dao.findTrusteesByMember(memberId);
            if (trustees.isEmpty()) {
                throw new IdentificationException("Unable to find any Trustee information for the given Member Id.");
            }
        } else if (memberId.equals(member.getExternalId())) {
            // Exception to the rule, is if the requesting user is
            // inquiring about themselves.
            trustees = dao.findTrusteesByMember(memberId);
        } else {
            throw new IdentificationException("Requesting Member is not authorized to inquire about other Member's.");
        }

        return trustees;
    }

    private static Trustee convert(final TrusteeEntity entity) {
        final Trustee trustee = new Trustee();

        trustee.setMemberId(entity.getMember().getExternalId());
        trustee.setAccountName(entity.getMember().getName());
        trustee.setPublicKey(entity.getMember().getMemberKey());
        trustee.setCircleId(entity.getCircle().getExternalId());
        trustee.setCircleName(entity.getCircle().getName());
        trustee.setTrustLevel(entity.getTrustLevel());
        trustee.setChanged(entity.getAltered());
        trustee.setAdded(entity.getAdded());

        return trustee;
    }
}
