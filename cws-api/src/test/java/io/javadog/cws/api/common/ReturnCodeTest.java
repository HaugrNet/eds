/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * <p>To ascertain why something has gone wrong, we need to look at the
 * ReturnCode, which is given. These serve as a general purpose value that
 * is given to the User.</p>
 *
 * <p>This rather primitive test serves the purpose of verifying that the
 * numbers and texts aren't accidentally altered, as this may affect how other
 * systems react to the results.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class ReturnCodeTest {

    @Test
    public void testSuccess() {
        assertThat(ReturnCode.SUCCESS.getCode(), is(200));
        assertThat(ReturnCode.SUCCESS.getHttpCode(), is(200));
        assertThat(ReturnCode.SUCCESS.getDescription(), is("Request completed normally."));
        assertThat(ReturnCode.SUCCESS.getClassification(), is(ReturnCode.Classification.CLASS_INFO));
    }

    @Test
    public void testWarning() {
        assertThat(ReturnCode.WARNING.getCode(), is(400));
        assertThat(ReturnCode.WARNING.getHttpCode(), is(400));
        assertThat(ReturnCode.WARNING.getDescription(), is("General Warning occurred while handling the request."));
        assertThat(ReturnCode.WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.AUTHORIZATION_WARNING.getCode(), is(401));
        assertThat(ReturnCode.AUTHORIZATION_WARNING.getHttpCode(), is(401));
        assertThat(ReturnCode.AUTHORIZATION_WARNING.getDescription(), is("The Account is not permitted to perform requested Action."));
        assertThat(ReturnCode.AUTHORIZATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.AUTHENTICATION_WARNING.getCode(), is(403));
        assertThat(ReturnCode.AUTHENTICATION_WARNING.getHttpCode(), is(403));
        assertThat(ReturnCode.AUTHENTICATION_WARNING.getDescription(), is("Authentication of the Account failed."));
        assertThat(ReturnCode.AUTHENTICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.IDENTIFICATION_WARNING.getCode(), is(404));
        assertThat(ReturnCode.IDENTIFICATION_WARNING.getHttpCode(), is(404));
        assertThat(ReturnCode.IDENTIFICATION_WARNING.getDescription(), is("Not possible to positively identify the requested Data."));
        assertThat(ReturnCode.IDENTIFICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.ILLEGAL_ACTION.getCode(), is(405));
        assertThat(ReturnCode.ILLEGAL_ACTION.getHttpCode(), is(405));
        assertThat(ReturnCode.ILLEGAL_ACTION.getDescription(), is("The Account tried to invoke an Action not allowed."));
        assertThat(ReturnCode.ILLEGAL_ACTION.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.VERIFICATION_WARNING.getCode(), is(406));
        assertThat(ReturnCode.VERIFICATION_WARNING.getHttpCode(), is(406));
        assertThat(ReturnCode.VERIFICATION_WARNING.getDescription(), is("The provided Request information is insufficient or invalid."));
        assertThat(ReturnCode.VERIFICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.INTEGRITY_WARNING.getCode(), is(409));
        assertThat(ReturnCode.INTEGRITY_WARNING.getHttpCode(), is(409));
        assertThat(ReturnCode.INTEGRITY_WARNING.getDescription(), is("Not possible to perform the given action, as it will lead to data integrity problems."));
        assertThat(ReturnCode.INTEGRITY_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.SIGNATURE_WARNING.getCode(), is(491));
        assertThat(ReturnCode.SIGNATURE_WARNING.getHttpCode(), is(400));
        assertThat(ReturnCode.SIGNATURE_WARNING.getDescription(), is("There Signature is not usable."));
        assertThat(ReturnCode.SIGNATURE_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.SETTING_WARNING.getCode(), is(492));
        assertThat(ReturnCode.SETTING_WARNING.getHttpCode(), is(400));
        assertThat(ReturnCode.SETTING_WARNING.getDescription(), is("Not permitted to add, alter or delete the given Setting."));
        assertThat(ReturnCode.SETTING_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));
    }

    @Test
    public void testError() {
        assertThat(ReturnCode.ERROR.getCode(), is(500));
        assertThat(ReturnCode.ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.ERROR.getDescription(), is("General Error occurred while handling the request."));
        assertThat(ReturnCode.ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.CRYPTO_ERROR.getCode(), is(591));
        assertThat(ReturnCode.CRYPTO_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.CRYPTO_ERROR.getDescription(), is("Cryptographic Error occurred during the handling of the request."));
        assertThat(ReturnCode.CRYPTO_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.INTEGRITY_ERROR.getCode(), is(592));
        assertThat(ReturnCode.INTEGRITY_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.INTEGRITY_ERROR.getDescription(), is("The Encrypted Data is having integrity problems."));
        assertThat(ReturnCode.INTEGRITY_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.SETTING_ERROR.getCode(), is(593));
        assertThat(ReturnCode.SETTING_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.SETTING_ERROR.getDescription(), is("Error extracting settings value."));
        assertThat(ReturnCode.SETTING_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.IDENTIFICATION_ERROR.getCode(), is(594));
        assertThat(ReturnCode.IDENTIFICATION_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.IDENTIFICATION_ERROR.getDescription(), is("Not possible to positively identify the Requested Data."));
        assertThat(ReturnCode.IDENTIFICATION_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.DATABASE_ERROR.getCode(), is(595));
        assertThat(ReturnCode.DATABASE_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.DATABASE_ERROR.getDescription(), is("Database Error occurred during the handling of the request."));
        assertThat(ReturnCode.DATABASE_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.CONSTRAINT_ERROR.getCode(), is(596));
        assertThat(ReturnCode.CONSTRAINT_ERROR.getHttpCode(), is(500));
        assertThat(ReturnCode.CONSTRAINT_ERROR.getDescription(), is("Unique Constraint Violation in the Database."));
        assertThat(ReturnCode.CONSTRAINT_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));
    }
}
