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

import io.javadog.cws.api.Management;
import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.client.rest.ManagementRestClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

/**
 * @author Kim Jensen
 * @since CWS 1.1
 */
@ManagedBean
@RequestScoped
public class AuthenticationBean {

    private Management client = new ManagementRestClient("http://localhost:8080/cws");

    private String userName;
    private String password;
    private String returnMessage;

    public String login() {
        final FacesContext context = FacesContext.getCurrentInstance();
        final HttpSession session = (HttpSession) context.getExternalContext().getSession(true);

        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(password.getBytes(StandardCharsets.UTF_8));
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setAccountName(userName);
        request.setAction(Action.LOGIN);
        request.setNewCredential(session.getId().getBytes(StandardCharsets.UTF_8));
        final Map<String, String> validation = request.validate();
        assert validation.isEmpty();

        final CwsResponse response = client.processMember(request);
        System.out.println(response.getReturnCode());
        System.out.println(response.getReturnMessage());
        returnMessage = response.getReturnMessage();

        return response.isOk() ? "success" : "failure";
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
}
