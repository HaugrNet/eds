/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
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

    public static final String FOLDER_TYPENAME = "folder";

    public static final String DATA_TYPENAME = "data";

    public static final int MAX_STRING_LENGTH = 256;

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
