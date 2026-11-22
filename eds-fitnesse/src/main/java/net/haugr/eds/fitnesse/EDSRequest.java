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
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.haugr.eds.client.model.Action;
import net.haugr.eds.fitnesse.utils.Constants;
import net.haugr.eds.client.model.CredentialType;
import net.haugr.eds.client.model.Circle;
import net.haugr.eds.client.model.Authentication;
import net.haugr.eds.client.model.EDSResponse;
import net.haugr.eds.client.model.ProcessCircleResponse;
import net.haugr.eds.client.model.ProcessDataResponse;
import net.haugr.eds.client.model.ProcessMemberResponse;
import net.haugr.eds.client.model.TrusteeSecret;
import net.haugr.eds.fitnesse.exceptions.StopTestException;
import net.haugr.eds.fitnesse.utils.Converter;

/**
 * <p>General Request Object for the FitNesse Fixtures.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public class EDSRequest<R extends EDSResponse> {

    protected static String requestUrl = "http://localhost:8080/api/v1";
    protected static final String EXTENSION_ID = "_id";
    protected static final String EXTENSION_INVITATION = "_invitation";

    // All Ids in EDS which is externally exposed is UUIDs, meaning that they
    // are always unique. And as the use of them should also be made with
    // easily identifiable, yet unique names, they are simply stored in a
    // single internal record, where the name has "_id" appended. This is then
    // used to both store, delete and find Ids.
    private static final Map<String, String> ids = new ConcurrentHashMap<>(16);
    private static final Map<String, String> invitations = new ConcurrentHashMap<>(16);
    // EDS generates the Session tokens; the tests refer to them by the name
    // given in the "new credential" column of the login.
    private static final Map<String, String> sessions = new ConcurrentHashMap<>(16);

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
        if ((credential != null) && credential.endsWith(EXTENSION_INVITATION)) {
            this.credential = Base64.getDecoder().decode(invitations.get(credential));
        } else if ((credential != null) && sessions.containsKey(credential)) {
            this.credential = Base64.getDecoder().decode(sessions.get(credential));
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

    protected void clearInvitations() {
        invitations.clear();
        sessions.clear();
    }

    protected static void setSession(final String name, final ProcessMemberResponse response) {
        if ((name != null) && (response != null) && (response.getSession() != null)) {
            sessions.put(name, Base64.getEncoder().encodeToString(response.getSession()));
        }
    }

    /**
     * Keeps a status key under a name, so it can be given as credential,
     * like a Session.
     *
     * @param name Name of the key in the tests
     * @param key  The key, Base64 (URL-safe), as EDS returns it
     */
    protected static void setStatusKey(final String name, final String key) {
        if ((name != null) && (key != null)) {
            sessions.put(name, Base64.getEncoder().encodeToString(Base64.getUrlDecoder().decode(key)));
        }
    }

    /**
     * The secrets of Trustees from a cell, as {@code member_id=secret, ...},
     * where the Member IDs are given by their names; an empty secret
     * removes it, and "null" gives an empty list.
     *
     * @param cell The cell
     * @return The secrets
     */
    protected List<TrusteeSecret> trusteeSecrets(final String cell) {
        final List<TrusteeSecret> secrets = new ArrayList<>();
        if ((cell != null) && !"null".equals(cell)) {
            for (final String pair : cell.split(",")) {
                final int equals = pair.indexOf('=');
                final String key = pair.substring(0, equals).trim();
                final String id = getId(key);
                secrets.add(new TrusteeSecret()
                        .memberId((id != null) ? id : key)
                        .secret(pair.substring(equals + 1).trim()));
            }
        }
        return secrets;
    }

    protected static void setInvitation(final String key, final ProcessMemberResponse response) {
        if (response != null) {
            final byte[] invitation = response.getInvitation();
            if (invitation != null) {
                invitations.put(key, Base64.getEncoder().encodeToString(invitation));
            }
        }
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

    protected String getKey(final String id) {
        return (id != null) ? findKey(ids, id) : null;
    }

    private static String findKey(final Map<String, String> map, final String id) {
        return map.entrySet().stream()
                .filter((Map.Entry<String, String> entry) -> Objects.equals(entry.getValue(), id))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
}
