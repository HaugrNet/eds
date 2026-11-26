/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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
package net.haugr.eds.testing.fitnesse;

import net.haugr.eds.testing.client.model.Action;
import net.haugr.eds.testing.client.model.TrustLevel;
import net.haugr.eds.testing.client.model.ProcessTrusteeRequest;
import net.haugr.eds.testing.client.model.ProcessTrusteeResponse;
import net.haugr.eds.testing.client.model.TrusteeSecret;
import java.util.List;
import java.util.Locale;
import net.haugr.eds.testing.fitnesse.callers.CallManagement;
import net.haugr.eds.testing.fitnesse.utils.Converter;

/**
 * <p>FitNesse Fixture for the EDS ProcessTrustee feature. Besides the
 * actions ADD, ALTER and REMOVE, the action SECRETS sets the secrets of
 * several Trustees at once (setSecrets), from the column "secrets", as
 * {@code member_id=secret, ...}, and the column "circle key".</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class ProcessTrustee extends EDSRequest<ProcessTrusteeResponse> {

    private Action action = null;
    private String circleId = null;
    private String memberId = null;
    private TrustLevel trustLevel = null;
    private String secret = null;
    private boolean manySecrets = false;
    private List<TrusteeSecret> secrets = null;
    private String circleKey = null;

    // =========================================================================
    // Request & Response Setters and Getters
    // =========================================================================

    public void setAction(final String action) {
        final String value = action.toUpperCase(Locale.ENGLISH);
        manySecrets = "SECRETS".equals(value);
        this.action = manySecrets ? null : Action.valueOf(value);
    }

    public void setCircleId(final String circleId) {
        this.circleId = getId(circleId);
    }

    public void setMemberId(final String memberId) {
        this.memberId = getId(memberId);
    }

    public void setTrustLevel(final String trustLevel) {
        final String value = Converter.preCheck(trustLevel);
        this.trustLevel = (value != null) ? TrustLevel.valueOf(value.toUpperCase(Locale.ENGLISH)) : null;
    }

    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * The secrets of several Trustees, as {@code member_id=secret, ...}; an
     * empty secret removes it.
     *
     * @param secrets Secrets by the names of the Member IDs
     */
    public void setSecrets(final String secrets) {
        this.secrets = trusteeSecrets(secrets);
    }

    public void setCircleKey(final String circleKey) {
        this.circleKey = Converter.preCheck(circleKey);
    }

    // =========================================================================
    // Standard FitNesse Fixture method(s)
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final ProcessTrusteeRequest request = prepareRequest(ProcessTrusteeRequest.class);
        request.setAction(action);
        request.setCircleId(circleId);
        request.setMemberId(memberId);
        request.setTrustLevel(trustLevel);
        request.setSecret(secret);
        request.setSecrets(secrets);
        request.setCircleKey(circleKey);

        response = manySecrets
                ? CallManagement.setSecrets(requestUrl, request)
                : CallManagement.processTrustee(requestUrl, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();

        this.action = null;
        this.circleId = null;
        this.memberId = null;
        this.trustLevel = null;
        this.secret = null;
        this.manySecrets = false;
        this.secrets = null;
        this.circleKey = null;
    }
}
