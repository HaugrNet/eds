package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.CredentialType;
import io.javadog.cws.api.common.TrustLevel;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class TrusteeTest {

    private static Member createMember() {
        final String admin = Constants.ADMIN_ACCOUNT;
        final char[] credentials = UUID.randomUUID().toString().toCharArray();

        final Authentication authentication = new Authentication();
        authentication.setName(admin);
        authentication.setCredentialType(CredentialType.PASSPHRASE);
        authentication.setCredential(credentials);

        final String id = UUID.randomUUID().toString();
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Member member = new Member();
        member.setId(id);
        member.setAuthentication(authentication);
        member.setModified(lastModified);
        member.setSince(created);

        return member;
    }

    private static Circle createCircle() {
        final Circle circle = new Circle();
        circle.setName("Circle Name");
        return circle;
    }

    @Test
    public void testClass() {
        final String id = UUID.randomUUID().toString();
        final Member member = createMember();
        final Circle circle = createCircle();
        final TrustLevel trustLevel = TrustLevel.WRITE;
        final Date lastModified = new Date(456L);
        final Date created = new Date(123L);

        final Trustee trustee = new Trustee();
        trustee.setId(id);
        trustee.setMember(member);
        trustee.setCircle(circle);
        trustee.setTrustLevel(trustLevel);
        trustee.setModified(lastModified);
        trustee.setSince(created);

        assertThat(trustee.getId(), is(id));
        assertThat(trustee.getMember(), is(member));
        assertThat(trustee.getCircle(), is(circle));
        assertThat(trustee.getTrustLevel(), is(trustLevel));
        assertThat(trustee.getModified(), is(lastModified));
        assertThat(trustee.getSince(), is(created));

        final Map<String, String> errors = trustee.validate();
        assertThat(errors.size(), is(0));
    }

    @Test
    public void testEmptyObjectValidation() {
        final Trustee trustee = new Trustee();
        final Map<String, String> errors = trustee.validate();
        assertThat(errors.size(), is(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSubObjects1() {
        final Trustee trustee = new Trustee();
        trustee.setCircle(new Circle());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSubObjects2() {
        final Trustee trustee = new Trustee();
        trustee.setMember(new Member());
    }

    @Test
    public void testInvalidSubObjects3() throws IllegalAccessException, NoSuchFieldException {
        // Create new, valid Member Object
        final Member member = createMember();

        // Set the Member Object in the Trustee Object
        final Trustee trustee = new Trustee();
        trustee.setMember(member);

        // Now, we're going to be a bit evil - we're altering the Member Object,
        // which has been set in the Trustee Object and should thus be legal.
        final Field auth = member.getClass().getDeclaredField("authentication");
        auth.setAccessible(true);
        auth.set(member, null);

        // Now, we can run the validation, and see if it manages to catch sub
        // Objects which are invalid
        final Map<String, String> errors = trustee.validate();
        assertThat(errors.size(), is(3));
    }

    @Test
    public void testInvalidSubObjects4() throws IllegalAccessException, NoSuchFieldException {
        // Create new, valid Circle Object
        final Circle circle = createCircle();

        // Set the Circle Object in the Trustee Object
        final Trustee trustee = new Trustee();
        trustee.setCircle(circle);

        // Now, we're going to be a bit evil - we're altering the Circle Object,
        // which has been set in the Trustee Object and should thus be legal.
        final Field name = circle.getClass().getDeclaredField("name");
        name.setAccessible(true);
        name.set(circle, "");

        // Now, we can run the validation, and see if it manages to catch sub
        // Objects which are invalid
        final Map<String, String> errors = trustee.validate();
        assertThat(errors.size(), is(3));
    }

    @Test
    public void testNullId() {
        final Trustee trustee = new Trustee();
        trustee.setId(null);
        assertThat(trustee.getId(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyId() {
        final Trustee trustee = new Trustee();
        trustee.setId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testShortId() {
        final Trustee trustee = new Trustee();
        trustee.setId("1234");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTooLongId() {
        final Trustee trustee = new Trustee();
        trustee.setId("12345678890123456788901234567889012345678890");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullCircle() {
        final Trustee trustee = new Trustee();
        trustee.setCircle(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMember() {
        final Trustee trustee = new Trustee();
        trustee.setMember(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTrustLevel() {
        final Trustee trustee = new Trustee();
        trustee.setTrustLevel(null);
    }
}
