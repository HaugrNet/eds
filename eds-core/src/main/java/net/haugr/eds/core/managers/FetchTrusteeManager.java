/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.EntityManager;
import net.haugr.eds.api.common.MemberRole;
import net.haugr.eds.api.common.ReturnCode;
import net.haugr.eds.api.dtos.Trustee;
import net.haugr.eds.api.requests.FetchTrusteeRequest;
import net.haugr.eds.api.responses.FetchTrusteeResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.exceptions.IdentificationException;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.TrusteeDao;
import net.haugr.eds.core.model.entities.TrusteeEntity;

/**
 * <p>Business Logic implementation for the EDS FetchTrustee request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchTrusteeManager extends AbstractManager<TrusteeDao, FetchTrusteeResponse, FetchTrusteeRequest> {

    public FetchTrusteeManager(final Settings settings, final EntityManager entityManager) {
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
        // can access information about Circles of Trust for Circles
        // where there is no relation.
        if (memberId != null) {
            trustees = dao.findTrusteesByMemberAndCircle(memberId, circleId);
            throwConditionalException(trustees.isEmpty(),
                    ReturnCode.IDENTIFICATION_WARNING, "Unable to find any relation between given Circle & Member Id's.");
        } else {
            trustees = dao.findTrusteesByCircle(circleId);
            throwConditionalException(trustees.isEmpty(),
                    ReturnCode.IDENTIFICATION_WARNING, "The requested Circle cannot be found.");
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
            throwConditionalException(trustees.isEmpty(),
                    ReturnCode.IDENTIFICATION_WARNING, "Unable to find any Trustee information for the given Member Id.");
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
