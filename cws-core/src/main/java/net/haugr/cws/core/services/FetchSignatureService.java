/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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
package net.haugr.cws.core.services;

import net.haugr.cws.api.dtos.Signature;
import net.haugr.cws.api.requests.FetchSignatureRequest;
import net.haugr.cws.api.responses.FetchSignatureResponse;
import net.haugr.cws.core.enums.Permission;
import net.haugr.cws.core.model.Settings;
import net.haugr.cws.core.model.SignatureDao;
import net.haugr.cws.core.model.entities.SignatureEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

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
            final var signature = new Signature();
            signature.setChecksum(entity.getChecksum());
            signature.setExpires(entity.getExpires());
            signature.setVerifications(entity.getVerifications());
            signature.setAdded(entity.getAdded());
            signature.setLastVerification(entity.getAltered());

            signatures.add(signature);
        }

        final var response = new FetchSignatureResponse();
        response.setSignatures(signatures);
        return response;
    }
}
