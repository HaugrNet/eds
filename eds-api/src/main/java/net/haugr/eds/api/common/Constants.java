/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2024, haugr.net
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

import jakarta.json.bind.annotation.JsonbDateFormat;
import java.util.regex.Pattern;

/**
 * <p>Common Constant values used throughout the API.</p>
 *
 * @author Kim Jensen
 * @since EDS 1.0
 */
public final class Constants {

    /**
     * <p>The current version of the EDS, this is used to tell Clients which
     * version is being communicated with, and the same number is also being
     * used to generate the SerialVersionUID for all API Classes.</p>
     */
    public static final String EDS_VERSION = "2.0-SNAPSHOT";

    /**
     * <p>Default Provider for the Crypto Library.</p>
     */
    public static final String SUN_JCE = "SunJCE";

    /**
     * <p>Default Elliptic Curve Provider for the Crypto Library.</p>
     */
    public static final String SUN_EC = "SunEC";

    /**
     * The default SALT (IV) size is 16 bytes, for GCM it is preferable to
     * only have 12 bytes - however, GCM will also work with 16 bytes albeit
     * slower.
     */
    public static final int GCM_IV_LENGTH = 128;

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
    public static final long SERIAL_VERSION_UID = EDS_VERSION.hashCode();

    /**
     * <p>All Id's must be compliant with a standard UUID Pattern, which this
     * regular expression matches.</p>
     */
    public static final String ID_PATTERN_REGEX = "\\b[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-\\b[0-9a-fA-F]{12}\\b";

    /**
     * <p>Pattern to test if a given value matches a UUID, which is what EDS
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

    /** JSON Date Format. */
    public static final String JSON_DATE_FORMAT = JsonbDateFormat.TIME_IN_MILLIS;

    /** Folder TypeName. */
    public static final String FOLDER_TYPENAME = "folder";

    /** Data TypeName. */
    public static final String DATA_TYPENAME = "data";

    /** Max Name Length. */
    public static final int MAX_NAME_LENGTH = 75;

    /** Max String Length. */
    public static final int MAX_STRING_LENGTH = 256;

    /** Max Page Size. */
    public static final int MAX_PAGE_SIZE = 100;

    // =========================================================================
    // Following is a list of the Field names being used as part of the SOAP
    // WSDL file - they are listed here, so the names can be used as part of
    // the XML Class Annotations.
    /** Field :: Circle &amp; Data Ids. */
    public static final String FIELD_IDS = "circle & data Id";
    /** Field :: CircleId. */
    public static final String FIELD_CIRCLE_ID = "circleId";
    /** Field :: Target CircleId. */
    public static final String FIELD_TARGET_CIRCLE_ID = "targetCircleId";
    /** Field :: MemberId. */
    public static final String FIELD_MEMBER_ID = "memberId";
    /** Field :: FolderId. */
    public static final String FIELD_FOLDER_ID = "folderId";
    /** Field :: Target FolderId. */
    public static final String FIELD_TARGET_FOLDER_ID = "targetFolderId";
    /** Field :: DataId. */
    public static final String FIELD_DATA_ID = "dataId";
    /** Field :: AccountName. */
    public static final String FIELD_ACCOUNT_NAME = "accountName";
    /** Field :: PublicKey. */
    public static final String FIELD_PUBLIC_KEY = "publicKey";
    /** Field :: New AccountName. */
    public static final String FIELD_NEW_ACCOUNT_NAME = "newAccountName";
    /** Field :: Credential. */
    public static final String FIELD_CREDENTIAL = "credential";
    /** Field :: New Credential. */
    public static final String FIELD_NEW_CREDENTIAL = "newCredential";
    /** Field :: MemberRole. */
    public static final String FIELD_MEMBER_ROLE = "memberRole";
    /** Field :: CredentialType. */
    public static final String FIELD_CREDENTIALTYPE = "credentialType";
    /** Field :: Circles. */
    public static final String FIELD_CIRCLES = "circles";
    /** Field :: CircleKey. */
    public static final String FIELD_CIRCLE_KEY = "circleKey";
    /** Field :: CircleName. */
    public static final String FIELD_CIRCLE_NAME = "circleName";
    /** Field :: Trustees. */
    public static final String FIELD_TRUSTEES = "trustees";
    /** Field :: Members. */
    public static final String FIELD_MEMBERS = "members";
    /** Field :: TrustLevel. */
    public static final String FIELD_TRUSTLEVEL = "trustLevel";
    /** Field :: Metadata. */
    public static final String FIELD_METADATA = "metadata";
    /** Field :: DataName. */
    public static final String FIELD_DATA_NAME = "dataName";
    /** Field :: Records. */
    public static final String FIELD_RECORDS = "records";
    /** Field :: Data. */
    public static final String FIELD_DATA = "data";
    /** Field :: DataType. */
    public static final String FIELD_DATATYPE = "dataType";
    /** Field :: DataTypes. */
    public static final String FIELD_DATATYPES = "dataTypes";
    /** Field :: TypeName. */
    public static final String FIELD_TYPENAME = "typeName";
    /** Field :: Type. */
    public static final String FIELD_TYPE = "type";
    /** Field :: Checksum. */
    public static final String FIELD_CHECKSUM = "checksum";
    /** Field :: Signature. */
    public static final String FIELD_SIGNATURE = "signature";
    /** Field :: Signatures. */
    public static final String FIELD_SIGNATURES = "signatures";
    /** Field :: Sanities. */
    public static final String FIELD_SANITIES = "sanities";
    /** Field :: Inventory. */
    public static final String FIELD_INVENTORY = "inventory";
    /** Field :: Settings. */
    public static final String FIELD_SETTINGS = "settings";
    /** Field :: Secret. */
    public static final String FIELD_SECRET = "secret";
    /** Field :: Expires. */
    public static final String FIELD_EXPIRES = "expires";
    /** Field :: Verifications. */
    public static final String FIELD_VERIFICATIONS = "verifications";
    /** Field :: Last Verification. */
    public static final String FIELD_LAST_VERIFICATION = "lastVerification";
    /** Field :: Added */
    public static final String FIELD_ADDED = "added";
    /** Field :: Changed. */
    public static final String FIELD_CHANGED = "changed";
    /** Field :: Since. */
    public static final String FIELD_SINCE = "since";
    /** Field :: Version. */
    public static final String FIELD_VERSION = "version";
    /** Field :: Verified.  */
    public static final String FIELD_VERIFIED = "verified";
    /** Field :: Action. */
    public static final String FIELD_ACTION = "action";
    /** Field :: Return Code. */
    public static final String FIELD_RETURN_CODE = "returnCode";
    /** Field :: Return Message*/
    public static final String FIELD_RETURN_MESSAGE = "returnMessage";
    /** Field :: URL. */
    public static final String FIELD_URL = "url";
    /** Field :: Page Number. */
    public static final String FIELD_PAGE_NUMBER = "pageNumber";
    /** Field :: Page Size. */
    public static final String FIELD_PAGE_SIZE = "pageSize";

    // =========================================================================
    // Following is the part of the URLs for all REST based requests. It is
    // divided into the base call, and the sub calls.
    // =========================================================================

    /** Root for the REST API. */
    public static final String REST_API = "/";

    // General requests from the Management Interface
    /** REST endpoint for the Version Service. */
    public static final String REST_VERSION = "/version";
    /** REST endpoint for the Settings Service. */
    public static final String REST_SETTINGS = "/settings";
    /** REST endpoint for the MasterKey Service. */
    public static final String REST_MASTERKEY = "/masterKey";
    /** REST endpoint for the Sanitized Service. */
    public static final String REST_SANITIZED = "/sanitized";
    /** REST endpoint for the Inventory Service. */
    public static final String REST_INVENTORY = "/inventory";
    /** REST endpoint for the Authenticated Service. */
    public static final String REST_AUTHENTICATED = "/authenticated";

    // Member requests from the Management Interface
    /** PATH for the REST based Members Service. */
    public static final String REST_MEMBERS_BASE = "/members";
    /** REST endpoint for creating a new Member. */
    public static final String REST_MEMBERS_CREATE = "/createMember";
    /** REST endpoint for Inviting a new Member. */
    public static final String REST_MEMBERS_INVITE = "/inviteMember";
    /** REST endpoint for Login of a Member. */
    public static final String REST_MEMBERS_LOGIN = "/login";
    /** REST endpoint for Logout of a Member. */
    public static final String REST_MEMBERS_LOGOUT = "/logout";
    /** REST endpoint for Altering a Member. */
    public static final String REST_MEMBERS_ALTER = "/alterMember";
    /** REST endpoint for Updating a Member. */
    public static final String REST_MEMBERS_UPDATE = "/updateMember";
    /** REST endpoint for Invalidating a Member. */
    public static final String REST_MEMBERS_INVALIDATE = "/invalidate";
    /** REST endpoint for Deleting a Member. */
    public static final String REST_MEMBERS_DELETE = "/deleteMember";
    /** REST endpoint for Fetching Members. */
    public static final String REST_MEMBERS_FETCH = "/fetchMembers";

    // Circle requests from the Management Interface
    /** PATH for the REST based Circle of Trust Service. */
    public static final String REST_CIRCLES_BASE = "/circles";
    /** REST endpoint for Creating a Circle of Trust. */
    public static final String REST_CIRCLES_CREATE = "/createCircle";
    /** REST endpoint for Updating a Circle of Trust. */
    public static final String REST_CIRCLES_UPDATE = "/updateCircle";
    /** REST endpoint for Deleting a Circle of Trust. */
    public static final String REST_CIRCLES_DELETE = "/deleteCircle";
    /** REST endpoint for Fetching Circles of Trust. */
    public static final String REST_CIRCLES_FETCH = "/fetchCircles";

    // Trustee requests from the Management Interface
    /** PATH for the REST based Trustee Service. */
    public static final String REST_TRUSTEES_BASE = "/trustees";
    /** REST endpoint for Adding a Trustee. */
    public static final String REST_TRUSTEES_ADD = "/addTrustee";
    /** REST endpoint for Altering a Trustee. */
    public static final String REST_TRUSTEES_ALTER = "/alterTrustee";
    /** REST endpoint for Removing a Trustee. */
    public static final String REST_TRUSTEES_REMOVE = "/removeTrustee";
    /** REST endpoint for Fetching Trustees. */
    public static final String REST_TRUSTEES_FETCH = "/fetchTrustees";

    // DataType requests from the Share Interface
    /** PATH for the REST based DataType Service. */
    public static final String REST_DATATYPES_BASE = "/dataTypes";
    /** REST endpoint for Processing a DataType. */
    public static final String REST_DATATYPES_PROCESS = "/processDataType";
    /** REST endpoint for Deleting a DataType. */
    public static final String REST_DATATYPES_DELETE = "/deleteDataType";
    /** REST endpoint for Fetching all DataTypes. */
    public static final String REST_DATATYPES_FETCH = "/fetchDataTypes";

    // Data requests from the Share Interface
    /** Path for the REST based Data Service. */
    public static final String REST_DATA_BASE = "/data";
    /** REST endpoint for Adding Data. */
    public static final String REST_DATA_ADD = "/addData";
    /** REST endpoint for Copying Data. */
    public static final String REST_DATA_COPY = "/copyData";
    /** REST endpoint for Moving Data. */
    public static final String REST_DATA_MOVE = "/moveData";
    /** REST endpoint for Updating Data. */
    public static final String REST_DATA_UPDATE = "/updateData";
    /** REST endpoint for Deleting Data. */
    public static final String REST_DATA_DELETE = "/deleteData";
    /** REST endpoint for Fetching Data. */
    public static final String REST_DATA_FETCH = "/fetchData";

    // Signature requests from the Share Interface
    /** Path for the REST based signature Service. */
    public static final String REST_SIGNATURES_BASE = "/signatures";
    /** REST endpoint for Signing a Document. */
    public static final String REST_SIGNATURES_SIGN = "/signDocument";
    /** REST endpoint for Verifying a Signature. */
    public static final String REST_SIGNATURES_VERIFY = "/verifySignature";
    /** REST endpoint for Fetching Signatures. */
    public static final String REST_SIGNATURES_FETCH = "/fetchSignatures";

    // =========================================================================

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
