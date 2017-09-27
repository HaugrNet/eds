/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "returnCode")
public enum ReturnCode {

    SUCCESS(Classification.CLASS_INFO, 0, "Request completed normally."),

    ERROR(Classification.CLASS_ERROR, 0, "General Error occurred while handling the request."),
    DATABASE_ERROR(Classification.CLASS_ERROR, 1, "Database Error occurred during the handling of the request."),
    CONSTRAINT_ERROR(Classification.CLASS_ERROR, 2, "Unique Constraint Violation in the Database."),
    PROPERTY_ERROR(Classification.CLASS_ERROR, 3, "Error extracting settings information."),
    CRYPTO_ERROR(Classification.CLASS_ERROR, 4, "Cryptographic Error occurred during the handling of the request."),
    IDENTIFICATION_ERROR(Classification.CLASS_ERROR, 5, "Not possible to positively identify the Requested Data."),
    INTEGRITY_ERROR(Classification.CLASS_ERROR, 6, "The Encrypted Data is having integrity problems."),
    NOTIMPLEMENTED_ERROR(Classification.CLASS_ERROR, 99, "Logic Pending Implemented..."),

    WARNING(Classification.CLASS_WARNING, 0, "General Warning occurred while handling the request."),
    AUTHENTICATION_WARNING(Classification.CLASS_WARNING, 1, "Authentication of the Account failed."),
    AUTHORIZATION_WARNING(Classification.CLASS_WARNING, 2, "The Account is not permitted to perform requested Action."),
    VERIFICATION_WARNING(Classification.CLASS_WARNING, 3, "The provided Request information is insufficient or invalid."),
    SIGNATURE_WARNING(Classification.CLASS_WARNING, 4, "There Signature is not usable."),
    IDENTIFICATION_WARNING(Classification.CLASS_WARNING, 5, "Not possible to positively identify the requested Data."),
    ILLEGAL_ACTION(Classification.CLASS_WARNING, 99, "The Account tried to invoke an Action not allowed.");

    private enum Classification {

        /**
         * <p>The Classification Info, is used for for all Successful Responses
         * from the CWS.</p>
         */
        CLASS_INFO(0),

        /**
         * <p>The Classification Warning, is used for problems which prevents
         * the successful completion of a Request - but where the cause of the
         * problem is something that can be corrected by the Requesting Member.
         * This is most likely due to invalid input or insufficient input data
         * or Authentication/Authorization problems.</p>
         */
        CLASS_WARNING(100),

        /**
         * <p>The Classification Error, is used for internal problems, which
         * prevents the CWS from successfully completing the request. These
         * errors will be caused by errors in either the implementation of the
         * logic or the quality of the existing data. Generally, this kind of
         * problem cannot be correct by the Members, but require Administrative
         * intervention or corrections by the CWS Developers.</p>
         */
        CLASS_ERROR(200);

        private final int classificationCode;
        Classification(final int classificationCode) {
            this.classificationCode = classificationCode;
        }

        private int getClassificationCode() {
            return classificationCode;
        }
    }

    // =========================================================================
    // Internal functionality for the ReturnCode Enum
    // =========================================================================

    private final Classification classification;
    private final int code;
    private final String description;

    ReturnCode(final Classification classification, final int code, final String description) {
        this.classification = classification;
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return classification.getClassificationCode() + code;
    }

    public String getDescription() {
        return description;
    }
}
