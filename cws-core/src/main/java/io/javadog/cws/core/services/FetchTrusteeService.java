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

import io.javadog.cws.api.common.ReturnCode;
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.exceptions.CWSException;
import io.javadog.cws.core.model.CommonDao;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.CircleEntity;
import io.javadog.cws.core.model.entities.TrusteeEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Business Logic implementation for the CWS FetchTrustee request.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchTrusteeService extends Serviceable<CommonDao, FetchTrusteeResponse, FetchTrusteeRequest> {

    public FetchTrusteeService(final Settings settings, final EntityManager entityManager) {
        super(settings, new CommonDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchTrusteeResponse perform(final FetchTrusteeRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_TRUSTEE);
        Arrays.fill(request.getCredential(), (byte) 0);

        // First retrieve the Circle via the ExternalId given. If no Circle
        // is found, the DAO will throw an Exception.
        final CircleEntity circle = dao.find(CircleEntity.class, request.getCircleId());
        if (circle == null) {
            throw new CWSException(ReturnCode.IDENTIFICATION_WARNING, "The requested Circle cannot be found.");
        }

        final FetchTrusteeResponse response = new FetchTrusteeResponse();
        final List<TrusteeEntity> members = dao.findTrusteesByCircle(circle);
        final List<Trustee> currentTrustees = new ArrayList<>(members.size());
        for (final TrusteeEntity entity : members) {
            currentTrustees.add(convert(entity));
        }
        response.setTrustees(currentTrustees);

        return response;
    }

    private static Trustee convert(final TrusteeEntity entity) {
        final Trustee trustee = new Trustee();

        trustee.setMemberId(entity.getMember().getExternalId());
        trustee.setPublicKey(entity.getMember().getMemberKey());
        trustee.setCircleId(entity.getCircle().getExternalId());
        trustee.setTrustLevel(entity.getTrustLevel());
        trustee.setChanged(entity.getAltered());
        trustee.setAdded(entity.getAdded());

        return trustee;
    }
}
