/*
 * =============================================================================
 * Copyright (c) 2016-2018, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.common;

/**
 * <p>Common Constant values used throughout the API.</p>
 *
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
     * {@code private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;}
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

    public static final int MAX_NAME_LENGTH = 75;

    public static final int MAX_STRING_LENGTH = 256;

    public static final int MAX_PAGE_SIZE = 100;

    // Following is a list of the Field names being used as part of the SOAP
    // WSDL file - they are listed here, so the names can be used as part of
    // the XML Class Annotations.
    public static final String FIELD_IDS = "circle & data Id";
    public static final String FIELD_CIRCLE_ID = "circleId";
    public static final String FIELD_MEMBER_ID = "memberId";
    public static final String FIELD_FOLDER_ID = "folderId";
    public static final String FIELD_DATA_ID = "dataId";
    public static final String FIELD_ACCOUNT_NAME = "accountName";
    public static final String FIELD_PUBLIC_KEY = "publicKey";
    public static final String FIELD_NEW_ACCOUNT_NAME = "newAccountName";
    public static final String FIELD_CREDENTIAL = "credential";
    public static final String FIELD_NEW_CREDENTIAL = "newCredential";
    public static final String FIELD_CREDENTIALTYPE = "credentialType";
    public static final String FIELD_CIRCLE = "circle";
    public static final String FIELD_CIRCLES = "circles";
    public static final String FIELD_CIRCKE_KEY = "circleKey";
    public static final String FIELD_CIRCLE_NAME = "circleName";
    public static final String FIELD_TRUSTEE = "trustee";
    public static final String FIELD_TRUSTEES = "trustees";
    public static final String FIELD_MEMBER = "member";
    public static final String FIELD_MEMBERS = "members";
    public static final String FIELD_TRUSTLEVEL = "trustLevel";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_DATA_NAME = "dataName";
    public static final String FIELD_RECORDS = "records";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_DATATYPE = "dataType";
    public static final String FIELD_DATATYPES = "dataTypes";
    public static final String FIELD_TYPENAME = "typeName";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_CHECKSUM = "checksum";
    public static final String FIELD_SIGNATURE = "signature";
    public static final String FIELD_SIGNATURES = "signatures";
    public static final String FIELD_SANITY = "sanity";
    public static final String FIELD_SANITIES = "sanities";
    public static final String FIELD_SETTINGS = "settings";
    public static final String FIELD_EXPIRES = "expires";
    public static final String FIELD_VERIFICATIONS = "verifications";
    public static final String FIELD_LAST_VERIFICATION = "lastVerification";
    public static final String FIELD_ADDED = "added";
    public static final String FIELD_CHANGED = "changed";
    public static final String FIELD_SINCE = "since";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_VERIFIED = "verified";
    public static final String FIELD_ACTION = "action";
    public static final String FIELD_RETURN_CODE = "returnCode";
    public static final String FIELD_RETURN_MESSAGE = "returnMessage";
    public static final String FIELD_PAGE_NUMBER = "pageNumber";
    public static final String FIELD_PAGE_SIZE = "pageSize";

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
