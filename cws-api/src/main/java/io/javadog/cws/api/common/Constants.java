/*
 * Cryptographic Web Store, CWS, open source backend service.
 * Copyright (C) 2016-2018 JavaDog.io
 * Apache Software License, version 2
 * mailto:cws AT JavaDog DOT io
 *
 * CWS is released in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 */
package io.javadog.cws.api.common;

import java.util.regex.Pattern;

/**
 * <p>Common Constant values used throughout the API.</p>
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
public final class Constants {

    /**
     * <p>The current version of the CWS, this is used to tell Clients which
     * version is being communicated with, and the same number is also being
     * used to generate the SerialVersionUID for all API Classes.</p>
     */
    public static final String CWS_VERSION = "1.1-SNAPSHOT";

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
    public static final long SERIAL_VERSION_UID = CWS_VERSION.hashCode();

    /**
     * <p>All Id's must be compliant with a standard UUID Pattern, which this
     * regular expression matches.</p>
     */
    public static final String ID_PATTERN_REGEX = "[\\da-z]{8}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{4}-[\\da-z]{12}";

    /**
     * <p>Pattern to test if a given value matches a UUID, which is what CWS
     * uses for some Ids.</p>
     */
    public static final Pattern ID_PATTERN = Pattern.compile(ID_PATTERN_REGEX);

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

    // =========================================================================
    // Following is a list of the Field names being used as part of the SOAP
    // WSDL file - they are listed here, so the names can be used as part of
    // the XML Class Annotations.
    public static final String FIELD_IDS = "circle & data Id";
    public static final String FIELD_CIRCLE_ID = "circleId";
    public static final String FIELD_TARGET_CIRCLE_ID = "targetCircleId";
    public static final String FIELD_MEMBER_ID = "memberId";
    public static final String FIELD_FOLDER_ID = "folderId";
    public static final String FIELD_TARGET_FOLDER_ID = "targetFolderId";
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
    public static final String FIELD_SECRET = "secret";
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
    // =========================================================================

    // =========================================================================
    // Following is the part of the URLs for all REST based requests. It is
    // divided into the base call, and the sub calls.

    // General requests from the Management Interface
    public static final String REST_VERSION = "/version";
    public static final String REST_SETTINGS = "/settings";
    public static final String REST_MASTERKEY = "/masterKey";
    public static final String REST_SANITIZED = "/sanitized";

    // Member requests from the Management Interface
    public static final String REST_MEMBERS_BASE = "/members";
    public static final String REST_MEMBERS_CREATE = "/createMember";
    public static final String REST_MEMBERS_INVITE = "/inviteMember";
    public static final String REST_MEMBERS_UPDATE = "/updateMember";
    public static final String REST_MEMBERS_INVALIDATE = "/invalidate";
    public static final String REST_MEMBERS_DELETE = "/deleteMember";
    public static final String REST_MEMBERS_FETCH = "/fetchMembers";

    // Circle requests from the Management Interface
    public static final String REST_CIRCLES_BASE = "/circles";
    public static final String REST_CIRCLES_CREATE = "/createCircle";
    public static final String REST_CIRCLES_UPDATE = "/updateCircle";
    public static final String REST_CIRCLES_DELETE = "/deleteCircle";
    public static final String REST_CIRCLES_FETCH = "/fetchCircles";

    // Trustee requests from the Management Interface
    public static final String REST_TRUSTEES_BASE = "/trustees";
    public static final String REST_TRUSTEES_ADD = "/addTrustee";
    public static final String REST_TRUSTEES_ALTER = "/alterTrustee";
    public static final String REST_TRUSTEES_REMOVE = "/removeTrustee";
    public static final String REST_TRUSTEES_FETCH = "/fetchTrustees";

    // DataType requests from the Share Interface
    public static final String REST_DATATYPES_BASE = "/dataTypes";
    public static final String REST_DATATYPES_PROCESS = "/processDataType";
    public static final String REST_DATATYPES_DELETE = "/deleteDataType";
    public static final String REST_DATATYPES_FETCH = "/fetchDataTypes";

    // Data requests from the Share Interface
    public static final String REST_DATA_BASE = "/data";
    public static final String REST_DATA_ADD = "/addData";
    public static final String REST_DATA_UPDATE = "/updateData";
    public static final String REST_DATA_DELETE = "/deleteData";
    public static final String REST_DATA_FETCH = "/fetchData";

    // Signature requests from the Share Interface
    public static final String REST_SIGNATURES_BASE = "/signatures";
    public static final String REST_SIGNATURES_SIGN = "/signDocument";
    public static final String REST_SIGNATURES_VERIFY = "/verifySignature";
    public static final String REST_SIGNATURES_FETCH = "/fetchSignatures";
    // =========================================================================

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
