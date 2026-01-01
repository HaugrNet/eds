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
package net.haugr.eds.fitnesse;

import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.haugr.eds.api.common.Action;
import net.haugr.eds.api.common.Constants;
import net.haugr.eds.api.common.CredentialType;
import net.haugr.eds.api.dtos.Circle;
import net.haugr.eds.api.requests.Authentication;
import net.haugr.eds.api.responses.EDSResponse;
import net.haugr.eds.api.responses.ProcessCircleResponse;
import net.haugr.eds.api.responses.ProcessDataResponse;
import net.haugr.eds.api.responses.ProcessMemberResponse;
import net.haugr.eds.api.responses.SignResponse;
import net.haugr.eds.fitnesse.exceptions.StopTestException;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>General Request Object for the FitNesse Fixtures.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public class EDSRequest<R extends EDSResponse> {

    protected static String requestUrl = "http://localhost:8080/eds";
    protected static final String EXTENSION_ID = "_id";
    protected static final String EXTENSION_SIGNATURE = "_signature";

    // All Ids in EDS which is externally exposed is UUIDs, meaning that they
    // are always unique. And as the use of them should also be made with
    // easily identifiable, yet unique names, they are simply stored in a
    // single internal record, where the name has "_id" appended. This is then
    // used to both store, delete and find Ids.
    private static final Map<String, String> ids = new ConcurrentHashMap<>(16);
    private static final Map<String, String> signatures = new ConcurrentHashMap<>(16);

    protected String accountName = null;
    protected byte[] credential = null;
    protected R response = null;
    private CredentialType credentialType = null;

    public static void updateUrl(final String url) {
        requestUrl = url;
    }

    public void setAccountName(final String accountName) {
        this.accountName = Converter.preCheck(accountName);
    }

    public void setCredential(final String credential) {
        if ((credential != null) && credential.endsWith(EXTENSION_SIGNATURE)) {
            this.credential = Base64.getDecoder().decode(getSignature(credential));
        } else {
            this.credential = Converter.convertBytes(credential);
        }
    }

    public void setCredentialType(final String credentialType) {
        this.credentialType = Converter.findCredentialType(credentialType);
    }

    public String returnCode() {
        return (response != null) ? String.valueOf(response.getReturnCode()) : null;
    }

    public String returnMessage() {
        return (response != null) ? response.getReturnMessage() : null;
    }

    protected <T extends Authentication> T prepareRequest(final Class<T> clazz) {
        try {
            final T request = clazz.getConstructor().newInstance();
            request.setAccountName(accountName);
            request.setCredential(credential);
            request.setCredentialType(credentialType);

            return request;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new StopTestException("Cannot instantiate Request Object", e);
        }
    }

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the execute
     * method, which runs the request. It builds the Request Object and saves
     * the Response Object.
     */
    public void execute() {
        throw new StopTestException("Unsupported Action.");
    }

    /**
     * When using the Fit Table Fixture, FitNesse is invoking the reset method
     * before preparing a new line in the table to build a new request.
     */
    public void reset() {
        this.accountName = null;
        this.credential = null;
        this.credentialType = null;
    }

    // =========================================================================
    // Operating the internal mapping of Members & Circles
    // =========================================================================

    protected void clearSignatures() {
        signatures.clear();
    }

    protected static void setSignature(final String key, final ProcessMemberResponse response) {
        if (response != null) {
            final byte[] signature = response.getSignature();
            if (signature != null) {
                signatures.put(key, Base64.getEncoder().encodeToString(signature));
            }
        }
    }

    protected static void setSignature(final String baseKey, final SignResponse response) {
        if (response != null) {
            final String signature = response.getSignature();
            if (signature != null) {
                signatures.put(generateSignatureKey(baseKey), signature);
            }
        }
    }

    private static String generateSignatureKey(final String baseKey) {
        int index = 1;

        for (final String currentKey : signatures.keySet()) {
            if (currentKey.startsWith(baseKey)) {
                index++;
            }
        }

        return baseKey + index;
    }

    protected static String getSignature(final String key) {
        return signatures.get(key);
    }

    protected void addCircleInfo(final StringBuilder builder, final List<Circle> circles) {
        for (int i = 0; i < circles.size(); i++) {
            final Circle circle = circles.get(i);
            if (i >= 1) {
                builder.append(", ");
            }
            builder.append("Circle{circleId='")
                    .append(getKey(circle.getCircleId()))
                    .append("', circleName='")
                    .append(circle.getCircleName())
                    .append("', circleKey='")
                    .append(circle.getCircleKey())
                    .append("'}");
        }
    }

    protected static void clearAndAddAdminId(final String value) {
        ids.clear();
        ids.put(Constants.ADMIN_ACCOUNT + EXTENSION_ID, value);
    }

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessMemberResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getMemberId());
        }
    }

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessCircleResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getCircleId());
        }
    }

    protected static void processId(final Action action, final String currentKey, final String newKey, final ProcessDataResponse response) {
        if (response != null) {
            processId(action, currentKey, newKey, response.getDataId());
        }
    }

    protected static void processId(final Action action, final String currentKey, final String newKey, final String id) {
        switch (action) {
            case REMOVE:
            case DELETE:
                if (currentKey != null) {
                    ids.remove(currentKey);
                }
                break;
            default:
                if ((newKey != null) && (id != null)) {
                    ids.put(newKey + EXTENSION_ID, id);
                }
                break;
        }
    }

    protected String getId(final String key) {
        String id = null;

        if (key != null) {
            id = ids.entrySet().stream()
                    .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getKey(), key))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);
        }

        return id;
    }

    protected static String getSignatureKey(final String signature) {
        return (signature != null) ? findKey(signatures, signature) : null;
    }

    protected String getKey(final String id) {
        return (id != null) ? findKey(ids, id) : null;
    }

    private static String findKey(final Map<String, String> map, final String id) {
        return map.entrySet().stream()
                .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getValue(), id))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    protected static Map<String, String> checksums() {
        final Base64.Decoder decoder = Base64.getDecoder();
        final Map<String, String> checksums = new ConcurrentHashMap<>(signatures.size());

        for (final Map.Entry<String, String> signature : signatures.entrySet()) {
            try {
                final MessageDigest digest = MessageDigest.getInstance("SHA-512");
                final byte[] bytes = decoder.decode(signature.getValue());
                final byte[] hashed = digest.digest(bytes);

                checksums.put(Base64.getEncoder().encodeToString(hashed), signature.getKey());
            } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
                throw new StopTestException(e.getMessage(), e);
            }
        }

        return checksums;
    }
}
