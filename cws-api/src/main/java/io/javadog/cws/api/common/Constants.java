/*
 * =============================================================================
 * Copyright (c) 2010-2017, JavaDog.IO
 * -----------------------------------------------------------------------------
 * Project: ZObEL (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Constants {

    /**
     * <p>All serialized classes should use this value. The value reflects the
     * current version of the system. If updates are made in one or more of the
     * serialized classes, it should be updated.</p>
     *
     * <p>If not used, then all classes that are serialized will have a runtime
     * performance overhead while calculating a UID, which matches the current
     * class. See "Effective Java, 2nd Edition" - Item 75.</p>
     *
     * <p>As this is a useless overhead and simple to avoid, it is recommended to
     * do so. Just add the following line in the beginning of all serialized
     * and derived classes:</p>
     *
     * {@code private static final long serialVersionUID = IWSConstants.SERIAL_VERSION_UID;}
     */
    public static final long SERIAL_VERSION_UID = 201701010010000L; // YYYYMMDDvvvnnnn

    /**
     * <p>All Id's must be compliant with a standard UUID Pattern, which this
     * regular expression matches.</p>
     */
    public static final String ID_PATTERN_REGEX = "[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}";

    /**
     * <p>The System Administrator Account - this account is used as the default
     * standard administrative Member Account, and cannot be altered. Upon first
     * attempt to process a Member, the System Administrator Account will be
     * stored in the underlying database with the credentials provided. This
     * way, the Account Name of the System Administrator will remain well-known,
     * however - as there are not default credentials, there's also nothing to
     * exploit.</p>
     */
    public static final String ADMIN_ACCOUNT = "admin";

    /**
     * <p>General number for all Errors, an Error is defined as an event that
     * happened during processing, which meant that the processing cannot
     * complete properly, most likely due to a configuration error, programming
     * error, memory problem or similar types of events. The rule of thumb is
     * that this level of error can only be handled by an administrator or the
     * CWS Developers.</p>
     */
    public static final int ERROR = 100;
    public static final int DATABASE_ERROR = ERROR + 1;
    public static final int CONSTRAINT_ERROR = ERROR + 2;
    public static final int PROPERTY_ERROR = ERROR + 3;
    public static final int CRYPTO_ERROR = ERROR + 4;

    /**
     * <p>General number for all Warnings, a Warning is defined as an even that
     * happened during processing, which meant that the processing cannot
     * complete properly, most likely due to invalid or missing data in the
     * request, lack of privileges or similar types of events. The rule of thumb
     * is that this type of error is linked directly to the member, and can be
     * correctly by the member.</p>
     */
    private static final int WARNING = 100;

    /**
     * <p>All requests should complete successfully, and when they do, then they
     * will return with this return code.</p>
     */
    public static final int SUCCESS = 0;

    public static final int VERIFICATION_WARNING = WARNING + 1;
    public static final int IDENTIFICATION_WARNING = WARNING + 2;
    public static final int AUTHORIZATION_WARNING = WARNING + 3;
    public static final int NOTIMPLEMENTED_ERROR = ERROR + 99;

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
