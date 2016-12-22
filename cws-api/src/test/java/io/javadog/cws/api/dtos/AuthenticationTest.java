package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.CredentialType;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class AuthenticationTest {

    @Test
    public void testClass() {
        final String name = "Authentication Name";
        final char[] credentials = "Member Passphrase".toCharArray();
        final CredentialType type = CredentialType.KEY;

        final Authentication authentication = new Authentication();
        assertThat(authentication.getName(), is(not(name)));
        assertThat(authentication.getCredentialType(), is(not(type)));
        assertThat(authentication.getCredentials(), is(not(credentials)));

        authentication.setName(name);
        authentication.setCredentialType(type);
        authentication.setCredentials(credentials);
        assertThat(authentication.getName(), is(name));
        assertThat(authentication.getCredentialType(), is(type));
        assertThat(authentication.getCredentials(), is(credentials));

        final Map<String, String> errors = authentication.validate();
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void testValidationOfEmptyObject() {
        final Authentication authentication = new Authentication();
        final Map<String, String> errors = authentication.validate();
        assertThat(errors.size(), is(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullName() {
        final Authentication authentication = new Authentication();
        authentication.setName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyName() {
        final Authentication authentication = new Authentication();
        authentication.setName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLongName() {
        final Authentication authentication = new Authentication();
        authentication.setName("1234567890123456789012345678901234567891234567890123456789012345678901234567890");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullType() {
        final Authentication authentication = new Authentication();
        authentication.setCredentialType(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCredentials() {
        final Authentication authentication = new Authentication();
        authentication.setCredentials(null);
    }
}
