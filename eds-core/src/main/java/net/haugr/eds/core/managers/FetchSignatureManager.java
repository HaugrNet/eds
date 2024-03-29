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
import net.haugr.eds.api.dtos.Signature;
import net.haugr.eds.api.requests.FetchSignatureRequest;
import net.haugr.eds.api.responses.FetchSignatureResponse;
import net.haugr.eds.core.enums.Permission;
import net.haugr.eds.core.model.Settings;
import net.haugr.eds.core.model.SignatureDao;
import net.haugr.eds.core.model.entities.SignatureEntity;

/**
 * <p>Business Logic implementation for the EDS FetchSignature request.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class FetchSignatureManager extends AbstractManager<SignatureDao, FetchSignatureResponse, FetchSignatureRequest> {

    public FetchSignatureManager(final Settings settings, final EntityManager entityManager) {
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
