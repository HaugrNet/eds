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
package net.haugr.eds.api.common;

import static net.haugr.eds.api.common.ReturnCode.Classification.CLASS_ERROR;
import static net.haugr.eds.api.common.ReturnCode.Classification.CLASS_INFO;
import static net.haugr.eds.api.common.ReturnCode.Classification.CLASS_WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * <p>To figure out why something has gone wrong, we need to look at the
 * ReturnCode, which is given. These serve as a general purpose value
 * given to the User.</p>
 *
 * <p>This rather primitive test serves the purpose of verifying that the
 * numbers and texts aren't accidentally altered, as this may affect how other
 * systems react to the results.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
final class ReturnCodeTest {

    @Test
    void testReturnCodes() {
        // Success
        runAssertions(ReturnCode.SUCCESS, 200, 200, CLASS_INFO, "Request completed normally.");

        // Warnings
        runAssertions(ReturnCode.WARNING, 400, 400, CLASS_WARNING, "General Warning occurred while handling the request.");
        runAssertions(ReturnCode.AUTHORIZATION_WARNING, 401, 401, CLASS_WARNING, "The Account is not permitted to perform requested Action.");
        runAssertions(ReturnCode.AUTHENTICATION_WARNING, 403, 403, CLASS_WARNING, "Authentication of the Account failed.");
        runAssertions(ReturnCode.IDENTIFICATION_WARNING, 404, 404, CLASS_WARNING, "Not possible to positively identify the requested Data.");
        runAssertions(ReturnCode.ILLEGAL_ACTION, 405, 405, CLASS_WARNING, "The Account tried to invoke an Action not allowed.");
        runAssertions(ReturnCode.VERIFICATION_WARNING, 406, 406, CLASS_WARNING, "The provided Request information is insufficient or invalid.");
        runAssertions(ReturnCode.INTEGRITY_WARNING, 409, 409, CLASS_WARNING, "Not possible to perform the given action, as it will lead to data integrity problems.");
        runAssertions(ReturnCode.SIGNATURE_WARNING, 491, 400, CLASS_WARNING, "There Signature is not usable.");
        runAssertions(ReturnCode.SETTING_WARNING, 492, 400, CLASS_WARNING, "Not permitted to add, alter or delete the given Setting.");

        // Errors
        runAssertions(ReturnCode.ERROR, 500, 500, CLASS_ERROR, "General Error occurred while handling the request.");
        runAssertions(ReturnCode.CRYPTO_ERROR, 591, 500, CLASS_ERROR, "Cryptographic Error occurred during the handling of the request.");
        runAssertions(ReturnCode.INTEGRITY_ERROR, 592, 500, CLASS_ERROR, "The Encrypted Data is having integrity problems.");
        runAssertions(ReturnCode.SETTING_ERROR, 593, 500, CLASS_ERROR, "Error extracting settings value.");
        runAssertions(ReturnCode.DATABASE_ERROR, 594, 500, CLASS_ERROR, "Database Error occurred during the handling of the request.");
        runAssertions(ReturnCode.CONSTRAINT_ERROR, 595, 500, CLASS_ERROR, "Unique Constraint Violation in the Database.");

        // Checking that findReturnCode of a non-existing returnCode value will
        // result in a general error.
        assertEquals(ReturnCode.ERROR, ReturnCode.findReturnCode(1));
    }

    private static void runAssertions(final ReturnCode returnCode, final int code, final int httpCode, final ReturnCode.Classification classification, final String description) {
        assertEquals(code, returnCode.getCode());
        assertEquals(httpCode, returnCode.getHttpCode());
        assertEquals(description, returnCode.getDescription());
        assertEquals(classification, returnCode.getClassification());
        assertEquals(returnCode, ReturnCode.findReturnCode(code));
    }
}
