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
 * @since  CWS 1.0
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
