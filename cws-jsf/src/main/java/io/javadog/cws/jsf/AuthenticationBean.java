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
package io.javadog.cws.jsf;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.ManagementBean;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

/**
 * @author Kim Jensen
 * @since CWS 1.1
 */
@ManagedBean
@RequestScoped
public class AuthenticationBean {

    private static final FacesContext CONTEXT = FacesContext.getCurrentInstance();
    private static final String COOKIE = "CWSToken";

    @Inject
    private ManagementBean client;

    private String userName;
    private String password;
    private String returnMessage;

    public String login() {
        try {
            final ExternalContext context = CONTEXT.getExternalContext();
            final String sessionKey = getOrCreateCookie(context);

            final ProcessMemberRequest request = new ProcessMemberRequest();
            request.setCredential(toBytes(password));
            request.setCredentialType(CredentialType.PASSPHRASE);
            request.setAccountName(userName);
            request.setAction(Action.LOGIN);
            request.setNewCredential(toBytes(sessionKey));

            final CwsResponse response = client.processMember(request);
            returnMessage = response.getReturnMessage();
            password = null;

            if (response.isOk()) {
                redirect(context, "success.xhtml");
            } else {
                redirect(context, "failure.xhtml");
            }

            return response.isOk() ? "success" : "failure";
        } catch (IOException | RuntimeException e) {
            returnMessage = e.getMessage();
            return "fatal";
        }
    }

    private String getOrCreateCookie(final ExternalContext context) {
        final Map<String, Object> cookies = context.getRequestCookieMap();
        final String cookie;

        if (cookies.containsKey(COOKIE)) {
            cookie = (String) cookies.get(COOKIE);
        } else {
            cookie = UUID.randomUUID().toString();

            Map<String, Object> options = new ConcurrentHashMap<>();
            options.put("path", "/");
            context.addResponseCookie(COOKIE, cookie, options);
        }

        return cookie;
    }

    private byte[] toBytes(final String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    private void redirect(final ExternalContext context, final String page) throws IOException {
        context.redirect(context.getRequestContextPath() + '/' + page);
    }
}
