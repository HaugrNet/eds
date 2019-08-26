package io.javadog.cws.jsf;

import io.javadog.cws.api.Management;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.client.rest.ManagementRestClient;
import java.nio.charset.StandardCharsets;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

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
        final ProcessMemberRequest request = new ProcessMemberRequest();
        request.setCredential(password.getBytes(StandardCharsets.UTF_8));
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setAccountName(userName);
        final CwsResponse response = client.authenticated(request);
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
