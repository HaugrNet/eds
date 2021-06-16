package io.javadog.cws.fitnesse;

import static org.junit.jupiter.api.Assertions.*;

import io.javadog.cws.api.common.Utilities;
import io.javadog.cws.fitnesse.utils.Converter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * @author Kim Jensen
 * @since CWS 2.0
 */
@EnabledIfSystemProperty(named = "local.instance.running", matches = "true")
class SignatureTest {

    @BeforeAll
    static void beforeAll() {
        final var controller = new ControlCws();
        controller.setUrl("http://localhost:8080/cws");
        controller.removeCircles();
        controller.removeMembers();
        controller.removeDataTypes();
    }

    @Test
    void createSignatureWithoutExpiration() {
        final var document = "My Wonderful Document to sign without expiration.";
        final var signature = signDocument(document, null);

        final var verification = verifySignature(document, signature.signature());
        assertTrue(verification.response.isVerified());
    }

    @Test
    void createSignatureWithExpiration() {
        final var document = "My Wonderful Document to sign";
        final var expires = Converter.convertDate(Utilities.newDate().plusDays(2));
        final var signature = signDocument(document, expires);

        final var verification = verifySignature(document, signature.signature());
        assertTrue(verification.response.isVerified());
    }

    @Test
    void createSignatureAlreadyExpired() {
        final var document = "My already expired Document to sign.";
        final var expires = Converter.convertDate(Utilities.newDate().minusDays(2));
        final var signature = signDocument(document, expires);

        final var verification = verifySignature(document, signature.signature());
        assertFalse(verification.response.isVerified());
    }

    private static Sign signDocument(final String document, final String expires) {
        final var sign = new Sign();
        sign.setAccountName("admin");
        sign.setCredential("admin");
        sign.setData(document);
        sign.setExpires(expires);
        sign.execute();

        return sign;
    }

    private static Verify verifySignature(final String document, final String signature) {
        final var verify = new Verify();
        verify.setAccountName("admin");
        verify.setCredential("admin");
        verify.setData(document);
        verify.setSignature(signature);
        verify.execute();

        return verify;
    }
}
