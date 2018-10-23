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
import io.javadog.cws.api.dtos.Trustee;
import io.javadog.cws.api.requests.FetchTrusteeRequest;
import io.javadog.cws.api.responses.FetchTrusteeResponse;
import io.javadog.cws.core.enums.Permission;
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

        final FetchTrusteeResponse response = new FetchTrusteeResponse();

        // First retrieve the Circle via the ExternalId given. If no Circle
        // is found, the DAO will throw an Exception.
        final CircleEntity circle = dao.find(CircleEntity.class, request.getCircleId());

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
            final List<TrusteeEntity> members = dao.findTrusteesByCircle(circle);
            response.setTrustees(convertTrustees(members));
        } else {
            response.setReturnCode(ReturnCode.IDENTIFICATION_WARNING);
            response.setReturnMessage("The requested Circle cannot be found.");
        }

        return response;
    }

    private static List<Trustee> convertTrustees(final List<TrusteeEntity> entities) {
        final List<Trustee> trustees = new ArrayList<>(entities.size());

        for (final TrusteeEntity entity : entities) {
            trustees.add(convert(entity));

        }

        return trustees;
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
