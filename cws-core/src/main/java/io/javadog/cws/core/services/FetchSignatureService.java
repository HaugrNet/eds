/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-core)
 * =============================================================================
 */
package io.javadog.cws.core.services;

import io.javadog.cws.api.dtos.Signature;
import io.javadog.cws.api.requests.FetchSignatureRequest;
import io.javadog.cws.api.responses.FetchSignatureResponse;
import io.javadog.cws.core.enums.Permission;
import io.javadog.cws.core.model.Settings;
import io.javadog.cws.core.model.entities.SignatureEntity;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class FetchSignatureService extends Serviceable<FetchSignatureResponse, FetchSignatureRequest> {

    public FetchSignatureService(final Settings settings, final EntityManager entityManager) {
        super(settings, entityManager);
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
