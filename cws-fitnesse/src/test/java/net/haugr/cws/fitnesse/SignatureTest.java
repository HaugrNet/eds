/*
 * CWS, Cryptographic Web Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2022, haugr.net
 * mailto: cws AT haugr DOT net
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

package net.haugr.cws.fitnesse;

import static org.junit.jupiter.api.Assertions.*;

import net.haugr.cws.api.common.Utilities;
import net.haugr.cws.fitnesse.utils.Converter;
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
