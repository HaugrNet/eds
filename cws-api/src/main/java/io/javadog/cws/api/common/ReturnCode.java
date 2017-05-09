/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

import javax.xml.bind.annotation.XmlType;

/**
 * @author Kim Jensen
 * @since CWS 1.0
 */
@XmlType(name = "returnCode")
public enum ReturnCode {

    SUCCESS(Classification.CLASS_INFO, 0, "Request completed normally."),

    ERROR(Classification.CLASS_ERROR, 0, "General Error occurred while handling the request."),
    DATABASE_ERROR(Classification.CLASS_ERROR, 1, "Database Error occurred during the handling of the request."),
    CONSTRAINT_ERROR(Classification.CLASS_ERROR, 2, "Unique Constraint Violation in the Database."),
    PROPERTY_ERROR(Classification.CLASS_ERROR, 3, "Error extracting settings information."),
    CRYPTO_ERROR(Classification.CLASS_ERROR, 4, "Cryptographic Error occurred during the handling of the request."),
    IDENTIFICATION_ERROR(Classification.CLASS_ERROR, 5, "Not possible to positively identify the Requested Data."),
    NOTIMPLEMENTED_ERROR(Classification.CLASS_ERROR, 99, "Logic Pending Implemented..."),

    WARNING(Classification.CLASS_WARNING, 0, "General Warning occurred while handling the request."),
    VERIFICATION_WARNING(Classification.CLASS_WARNING, 1, "The provided Request information is insufficient or invalid."),
    IDENTIFICATION_WARNING(Classification.CLASS_WARNING, 2, "Not possible to positively identify the requested Data."),
    AUTHORIZATION_WARNING(Classification.CLASS_WARNING, 3, "The Account is not permitted to perform requested Action."),
    ILLEGAL_ACTION(Classification.CLASS_WARNING, 99, "The Account tried to invoke an Action not allowed.");

    private enum Classification {

        /**
         * <p>The Info Classification is used for the Successful responses from
         * the CWS.</p>
         *
         * <p>All requests should complete successfully, and when they do, then they
         * will return with this return code.</p>
         */
        CLASS_INFO(0),

        /**
         * <p>The Warning Classification is used for the types of errors, which
         * is caused by Member input being incorrect or invalid. Meaning that
         * the problem is correctable by the Member.</p>
         *
         * <p>General number for all Warnings, a Warning is defined as an event that
         * happened during processing, which meant that the processing cannot
         * complete properly, most likely due to invalid or missing data in the
         * request, lack of privileges or similar types of events. The rule of thumb
         * is that this type of error is linked directly to the member, and can be
         * correctly by the member.</p>
         */
        CLASS_WARNING(1),

        /**
         * <p>The Error Classification is used for the types of errors, which is
         * caused internally, meaning that it is not possible for a Member to
         * do anything to correct it.</p>
         *
         * <p>General number for all Errors, an Error is defined as an event that
         * happened during processing, which meant that the processing cannot
         * complete properly, most likely due to a configuration error, programming
         * error, memory problem or similar types of events. The rule of thumb is
         * that this level of error can only be handled by an administrator or the
         * CWS Developers.</p>
         */
        CLASS_ERROR(2);

        private final int classificationCode;
        Classification(final int classificationCode) {
            this.classificationCode = classificationCode;
        }

        private int getClassificationCode() {
            return classificationCode * 100;
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
