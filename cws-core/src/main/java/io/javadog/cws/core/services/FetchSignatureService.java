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

import io.javadog.cws.api.dtos.Signature;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.SignatureDao;
import io.javadog.cws.core.model.entities.SignatureEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Business Logic implementation for the CWS FetchSignature request.</p>
 *
 * @author Kim Jensen
 * @since CWS 1.0
 */
public final class FetchSignatureService extends Serviceable<SignatureDao, FetchSignatureResponse, FetchSignatureRequest> {

    public FetchSignatureService(final Settings settings, final EntityManager entityManager) {
        super(settings, new SignatureDao(entityManager));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FetchSignatureResponse perform(final FetchSignatureRequest request) {
        // Pre-checks, & destruction of credentials
        verifyRequest(request, Permission.FETCH_SIGNATURES);
        Arrays.fill(request.getCredential(), (byte) 0);

        final List<SignatureEntity> found = dao.findAllSignatures(member);
        final List<Signature> signatures = new ArrayList<>(found.size());

        for (final SignatureEntity entity : found) {
            final Signature signature = new Signature();
            signature.setChecksum(entity.getChecksum());
            signature.setExpires(entity.getExpires());
            signature.setVerifications(entity.getVerifications());
            signature.setAdded(entity.getAdded());
            signature.setLastVerification(entity.getAltered());

            signatures.add(signature);
        }

        final FetchSignatureResponse response = new FetchSignatureResponse();
        response.setSignatures(signatures);
        return response;
    }
}
