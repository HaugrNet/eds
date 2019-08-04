package io.javadog.cws.jsf;

import io.javadog.cws.api.common.Action;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.requests.Authentication;
import io.javadog.cws.api.requests.ProcessMemberRequest;
import io.javadog.cws.api.responses.CwsResponse;
import io.javadog.cws.core.ManagementBean;
import io.javadog.cws.core.model.Settings;
import java.util.Enumeration;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpSession;

/**
 * @author Kim Jensen
 * @since CWS 1.1
 */
@ManagedBean
@SessionScoped
public class AuthenticationBean {

    private static final Logger LOG = Logger.getLogger(AuthenticationBean.class.getName());
    private static final FacesContext facesContext = FacesContext.getCurrentInstance();
    private static final Settings settings = Settings.getInstance();

    @Inject
    private ManagementBean bean;

    private String errorMessage = "";
    private String userName;
    private String password;

    public String login() {
        final Authentication request = new Authentication();
        request.setCredential(password.getBytes(settings.getCharset()));
        request.setCredentialType(CredentialType.PASSPHRASE);
        request.setAccountName(userName);
        final CwsResponse response = bean.authenticated(request);
        errorMessage = response.getReturnMessage();

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

    private String getErrorMessage() {
        return errorMessage;
    }

    private HttpSession dumpSession() {
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);
        Enumeration e = session.getAttributeNames();
        while (e.hasMoreElements())
        {
            String attr = (String)e.nextElement();
            System.err.println("      attr  = "+ attr);
            Object value = session.getValue(attr);
            System.err.println("      value = "+ value);
        }

        return session;
    }
}
