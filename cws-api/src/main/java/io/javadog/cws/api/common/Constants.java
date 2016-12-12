package io.javadog.cws.api.common;

/**
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public class Constants {

    /**
     * <p>The Version of the current CWS Instance.</p>
     */
    public static final String CWS_VERSION = "CWS 1.0.0";

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

}
