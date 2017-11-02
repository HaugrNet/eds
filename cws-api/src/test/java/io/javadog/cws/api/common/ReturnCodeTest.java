/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
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
        assertThat(ReturnCode.SUCCESS.getCode(), is(0));
        assertThat(ReturnCode.SUCCESS.getDescription(), is("Request completed normally."));
        assertThat(ReturnCode.SUCCESS.getClassification(), is(ReturnCode.Classification.CLASS_INFO));
    }

    @Test
    public void testWarning() {
        assertThat(ReturnCode.WARNING.getCode(), is(100));
        assertThat(ReturnCode.WARNING.getDescription(), is("General Warning occurred while handling the request."));
        assertThat(ReturnCode.WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.VERIFICATION_WARNING.getCode(), is(101));
        assertThat(ReturnCode.VERIFICATION_WARNING.getDescription(), is("The provided Request information is insufficient or invalid."));
        assertThat(ReturnCode.VERIFICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.AUTHENTICATION_WARNING.getCode(), is(102));
        assertThat(ReturnCode.AUTHENTICATION_WARNING.getDescription(), is("Authentication of the Account failed."));
        assertThat(ReturnCode.AUTHENTICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.AUTHORIZATION_WARNING.getCode(), is(103));
        assertThat(ReturnCode.AUTHORIZATION_WARNING.getDescription(), is("The Account is not permitted to perform requested Action."));
        assertThat(ReturnCode.AUTHORIZATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.ILLEGAL_ACTION.getCode(), is(104));
        assertThat(ReturnCode.ILLEGAL_ACTION.getDescription(), is("The Account tried to invoke an Action not allowed."));
        assertThat(ReturnCode.ILLEGAL_ACTION.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.SIGNATURE_WARNING.getCode(), is(105));
        assertThat(ReturnCode.SIGNATURE_WARNING.getDescription(), is("There Signature is not usable."));
        assertThat(ReturnCode.SIGNATURE_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.IDENTIFICATION_WARNING.getCode(), is(106));
        assertThat(ReturnCode.IDENTIFICATION_WARNING.getDescription(), is("Not possible to positively identify the requested Data."));
        assertThat(ReturnCode.IDENTIFICATION_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));

        assertThat(ReturnCode.INTEGRITY_WARNING.getCode(), is(107));
        assertThat(ReturnCode.INTEGRITY_WARNING.getDescription(), is("Not possible to perform the given action, as it will lead to data integrity problems."));
        assertThat(ReturnCode.INTEGRITY_WARNING.getClassification(), is(ReturnCode.Classification.CLASS_WARNING));
    }

    @Test
    public void testError() {
        assertThat(ReturnCode.ERROR.getCode(), is(200));
        assertThat(ReturnCode.ERROR.getDescription(), is("General Error occurred while handling the request."));
        assertThat(ReturnCode.ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.DATABASE_ERROR.getCode(), is(201));
        assertThat(ReturnCode.DATABASE_ERROR.getDescription(), is("Database Error occurred during the handling of the request."));
        assertThat(ReturnCode.DATABASE_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.CONSTRAINT_ERROR.getCode(), is(202));
        assertThat(ReturnCode.CONSTRAINT_ERROR.getDescription(), is("Unique Constraint Violation in the Database."));
        assertThat(ReturnCode.CONSTRAINT_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.PROPERTY_ERROR.getCode(), is(203));
        assertThat(ReturnCode.PROPERTY_ERROR.getDescription(), is("Error extracting settings information."));
        assertThat(ReturnCode.PROPERTY_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.CRYPTO_ERROR.getCode(), is(204));
        assertThat(ReturnCode.CRYPTO_ERROR.getDescription(), is("Cryptographic Error occurred during the handling of the request."));
        assertThat(ReturnCode.CRYPTO_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.IDENTIFICATION_ERROR.getCode(), is(205));
        assertThat(ReturnCode.IDENTIFICATION_ERROR.getDescription(), is("Not possible to positively identify the Requested Data."));
        assertThat(ReturnCode.IDENTIFICATION_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));

        assertThat(ReturnCode.INTEGRITY_ERROR.getCode(), is(206));
        assertThat(ReturnCode.INTEGRITY_ERROR.getDescription(), is("The Encrypted Data is having integrity problems."));
        assertThat(ReturnCode.INTEGRITY_ERROR.getClassification(), is(ReturnCode.Classification.CLASS_ERROR));
    }
}
