/*
 * EDS, Encrypted Data Share - open source Cryptographic Sharing system.
 * Copyright (c) 2016-2026, haugr.net
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

    /** Length of the AES-GCM authentication tag, in bits. */
    public static final int GCM_TAG_BITS = 128;

    /** Length of the AES-GCM nonce (IV), in bytes; 96 bits as recommended by NIST SP 800-38D. */
    public static final int GCM_NONCE_BYTES = 12;

    /**
     * <p>All serialized classes should use this value. The value reflects the
     * current version of the system. If updates are made in one or more of the
     * serialized classes, it should be updated.</p>
     *
     * <p>If not used, then all classes that are serialized will have a runtime
     * performance overhead while calculating a UID, which matches the current
     * class. See "Effective Java, 2nd Edition" - Item 75.</p>
     *
     * <p>To prevent this unnecessary burden, which is easily circumvented, it
     * is advised to implement the following: Incorporate the later line
     * at the commencement of all classes that are serialized and derived:</p>
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
     * <p>The name which the tests and the examples give the System
     * Administrator. A stone doesn't ask for it: it has no account to begin
     * with, and the first who comes to it becomes its System Administrator,
     * under the name and with the credentials given, whatever the name. So
     * there is neither a default name nor default credentials to exploit.</p>
     */
    public static final String ADMIN_ACCOUNT = "admin";

    /** Folder TypeName. */
    public static final String FOLDER_TYPENAME = "folder";

    /** Data TypeName. */
    public static final String DATA_TYPENAME = "data";

    /** Names starting with this prefix are hidden: listings skip them, unless asked for. */
    public static final String HIDDEN_PREFIX = ".";

    /** Thumbnail TypeName, the Data Type of the thumbnail of a Data Object (PLAN.md, L5). */
    public static final String THUMBNAIL_TYPENAME = "thumbnail";

    /** Name prefix of a thumbnail, followed by the ID of its Data Object; hidden. */
    public static final String THUMBNAIL_PREFIX = ".thumbnail-";

    /** Largest thumbnail, in bytes. */
    public static final int MAX_THUMBNAIL_SIZE = 262144;

    /** Max Name Length. */
    public static final int MAX_NAME_LENGTH = 75;

    /**
     * The largest stored name of a Data Object (file or folder). Larger than
     * {@link #MAX_NAME_LENGTH}, which stays the limit of a name a person
     * chooses, because a client of a chamber (E2E.md) encrypts the name before
     * the stone gets it, and the ciphertext of a 75-character name, Base64url
     * encoded, is longer. The stone never looks inside the name; it only
     * stores it, so the only need is room for the encrypted form.
     */
    public static final int MAX_DATA_NAME_LENGTH = 512;

    /**
     * The Name Key of a Data Object: a keyed hash of its name, which a client
     * of a chamber (E2E.md) sends next to the encrypted name. The ciphertext
     * differs each time, so the stone can't compare two names; it compares
     * the Name Keys instead, to keep a name unique within its folder. Opaque
     * to the stone, Base64url without padding; an HMAC-SHA256 is 43
     * characters.
     */
    public static final int MAX_NAME_KEY_LENGTH = 128;

    /** {@link #MAX_NAME_KEY_LENGTH} Base64url characters, without padding. */
    public static final String NAME_KEY_PATTERN_REGEX = "^[A-Za-z0-9_-]{1,128}$";

    /** {@link #NAME_KEY_PATTERN_REGEX} compiled. */
    public static final Pattern NAME_KEY_PATTERN = Pattern.compile(NAME_KEY_PATTERN_REGEX);

    /**
     * An avatar: the ID of one of the clients' pictures, e.g. "raven". EDS
     * only stores it; lowercase letters, digits and hyphens.
     */
    public static final String AVATAR_PATTERN_REGEX = "^[a-z0-9-]{0,32}$";

    /** {@link #AVATAR_PATTERN_REGEX} compiled. */
    public static final Pattern AVATAR_PATTERN = Pattern.compile(AVATAR_PATTERN_REGEX);

    /** Max Avatar Length. */
    public static final int MAX_AVATAR_LENGTH = 32;

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
    public static final String FIELD_FIRST_ADMINISTRATOR = "firstAdministrator";
    /** Field :: CredentialType. */
    public static final String FIELD_CREDENTIALTYPE = "credentialType";
    /** Field :: Circles. */
    public static final String FIELD_CIRCLES = "circles";
    /** Field :: CircleKey. */
    public static final String FIELD_CIRCLE_KEY = "circleKey";
    /** Field :: Avatar. */
    public static final String FIELD_AVATAR = "avatar";
    /** Field :: Circle Avatar. */
    public static final String FIELD_CIRCLE_AVATAR = "circleAvatar";
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
    /** Field :: NameKey, see {@link #MAX_NAME_KEY_LENGTH}. */
    public static final String FIELD_NAME_KEY = "nameKey";
    /** Field :: IncludeHidden. */
    public static final String FIELD_INCLUDE_HIDDEN = "includeHidden";
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
    /** Field :: Invitation. */
    public static final String FIELD_INVITATION = "invitation";
    /** Field :: Session. */
    public static final String FIELD_SESSION = "session";
    /** Field :: ETag. */
    public static final String FIELD_ETAG = "etag";
    /** Field :: Media Type. */
    public static final String FIELD_MEDIA_TYPE = "mediaType";
    /** Maximum length of a Media Type. */
    public static final int MAX_MEDIA_TYPE_LENGTH = 255;
    /** A Media Type, type/subtype without parameters (RFC 6838). */
    public static final String MEDIA_TYPE_PATTERN_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}/[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}$";
    /** Field :: Sanities. */
    public static final String FIELD_SANITIES = "sanities";
    /** Field :: Inventory, what a stone holds, as a part of its status (STATUS.md). */
    public static final String FIELD_INVENTORY = "inventory";
    /** Field :: Settings. */
    public static final String FIELD_SETTINGS = "settings";

    /** Field :: Features (FEATURES.md). */
    public static final String FIELD_FEATURES = "features";
    /** Field :: Feature, the name of a feature. */
    public static final String FIELD_FEATURE = "feature";
    /** Field :: Declaration of a feature. */
    public static final String FIELD_DECLARATION = "declaration";
    /** Field :: State of a feature in a Circle. */
    public static final String FIELD_STATE = "state";
    /** Field :: Entries of a feature. */
    public static final String FIELD_ENTRIES = "entries";
    /** Field :: EntryId. */
    public static final String FIELD_ENTRY_ID = "entryId";
    /** Field :: Kind of an entry. */
    public static final String FIELD_KIND = "kind";
    /** Field :: Position of an entry in its feature. */
    public static final String FIELD_POSITION = "position";
    /** Field :: About, the entry which an entry is about. */
    public static final String FIELD_ABOUT = "about";
    /** Field :: Name. */
    public static final String FIELD_NAME = "name";
    /** Field :: Content of an entry. */
    public static final String FIELD_CONTENT = "content";
    /** Field :: Closed. */
    public static final String FIELD_CLOSED = "closed";
    /** Field :: Count. */
    public static final String FIELD_COUNT = "count";

    /** Max length of the name of a feature, and of a kind of its entries (FEATURES.md). */
    public static final int MAX_FEATURE_NAME_LENGTH = 32;
    /** Max length of the declaration of a feature. */
    public static final int MAX_DECLARATION_LENGTH = 16384;
    /** Max number of entries in one listing. */
    public static final int MAX_ENTRY_PAGE = 200;
    /** Entry TypeName: the Data Type of the entries of the features of a Circle (FEATURES.md). */
    public static final String ENTRY_TYPENAME = "entry";
    /** Field :: Ref, which a client gives a frame, and gets back with the answer. */
    public static final String FIELD_REF = "ref";
    /** Field :: Entry. */
    public static final String FIELD_ENTRY = "entry";
    /** Field :: After, a position. */
    public static final String FIELD_AFTER = "after";
    /** Field :: Before, a position. */
    public static final String FIELD_BEFORE = "before";
    /** Field :: OldestFirst. */
    public static final String FIELD_OLDEST_FIRST = "oldestFirst";
    /** Field :: Limit. */
    public static final String FIELD_LIMIT = "limit";
    /** Field :: Bare, without the content of the entries. */
    public static final String FIELD_BARE = "bare";
    /** Field :: Signal, a short text which is passed on and not stored. */
    public static final String FIELD_SIGNAL = "signal";
    /** Field :: To, the Member a signal is for. */
    public static final String FIELD_TO = "to";
    /** Field :: Unseen, joining a Circle without being shown as present. */
    public static final String FIELD_UNSEEN = "unseen";
    /** Field :: Present, the Members who are present in a Circle. */
    public static final String FIELD_PRESENT = "present";

    /** Max length of a signal (FEATURES.md). */
    public static final int MAX_SIGNAL_LENGTH = 32768;
    /** The longest content, Base64-encoded, which an announcement of an entry carries; a longer one is fetched. */
    public static final int MAX_ANNOUNCED_CONTENT = 16384;
    /** Field :: Load, how busy a stone is, as a part of its status (STATUS.md). */
    public static final String FIELD_LOAD = "load";
    /** Field :: Day, the load of the last day in steps. */
    public static final String FIELD_DAY = "day";
    /** Field :: Samples. */
    public static final String FIELD_SAMPLES = "samples";
    /** Field :: Values. */
    public static final String FIELD_VALUES = "values";
    /** Field :: Sequence. */
    public static final String FIELD_SEQUENCE = "sequence";
    /** Field :: Time. */
    public static final String FIELD_TIME = "time";
    /** Field :: Interval. */
    public static final String FIELD_INTERVAL = "interval";
    /** Field :: Awake. */
    public static final String FIELD_AWAKE = "awake";
    /** Field :: Started. */
    public static final String FIELD_STARTED = "started";
    /** Field :: DatabaseMark. */
    public static final String FIELD_DATABASE_MARK = "databaseMark";
    /** Field :: Label. */
    public static final String FIELD_LABEL = "label";
    /** Field :: Key. */
    public static final String FIELD_KEY = "key";
    /** Field :: Keys. */
    public static final String FIELD_KEYS = "keys";

    /** Max length of the name of a stone (STATUS.md). */
    public static final int MAX_STONE_NAME_LENGTH = 64;
    /** REST endpoint for the status of the stone (STATUS.md). */
    public static final String REST_STATUS_BASE = "/status";
    /** REST endpoint for the status keys of the stone. */
    public static final String REST_STATUS_KEYS = "/keys";
    /** REST endpoint for the features of the stone. */
    public static final String REST_FEATURES_BASE = "/features";

    /** Field :: Vault, a Member's own opaque text (E2E.md). */
    public static final String FIELD_VAULT = "vault";

    /** Field :: Secrets, the secrets of several Trustees (E2E.md). */
    public static final String FIELD_SECRETS = "secrets";

    /** Max length of a Trustee's secret (E2E.md): room for hybrid post-quantum wraps and several device keys. */
    public static final int MAX_SECRET_LENGTH = 65536;

    /** Max length of a Member's vault (E2E.md). */
    public static final int MAX_VAULT_LENGTH = 65536;
    public static final String FIELD_ALLOWED_VALUES = "allowedValues";
    /** Max length of a Member's settings for clients (JSON text). */
    public static final int MAX_MEMBER_SETTINGS_LENGTH = 4096;
    /** Field :: Secret: the MasterKey's in its request, and the opaque text of a Member in a Circle (E2E.md). */
    public static final String FIELD_SECRET = "secret";
    /** Field :: Expires. */
    public static final String FIELD_EXPIRES = "expires";
    /** Field :: Verifications. */
    public static final String FIELD_VERIFICATIONS = "verifications";
    /** Field :: Last Verification. */
    public static final String FIELD_LAST_VERIFICATION = "lastVerification";
    /** Field :: Added */
    public static final String FIELD_SIZE = "size";
    /** Field :: Thumbnail Id. */
    public static final String FIELD_THUMBNAIL_ID = "thumbnailId";
    /** Field :: Token. */
    public static final String FIELD_TOKEN = "token";
    public static final String FIELD_ADDED = "added";
    /** Field :: Changed. */
    public static final String FIELD_CHANGED = "changed";
    /** Field :: When the current Circle Secret was made (PLAN.md, Crypto C6). */
    public static final String FIELD_SECRET_ROTATED = "secretRotated";
    /** Field :: When the Circle Secret is to be rotated next. */
    public static final String FIELD_ROTATION_DUE = "rotationDue";
    /** Field :: The rotation of the Circle Secret under way. */
    public static final String FIELD_ROTATION = "rotation";
    /** Field :: Objects of a Circle. */
    public static final String FIELD_OBJECTS = "objects";
    /** Field :: Objects re-encrypted. */
    public static final String FIELD_OBJECTS_DONE = "objectsDone";
    /** Field :: Since. */
    public static final String FIELD_SINCE = "since";
    /** Field :: Version. */
    public static final String FIELD_VERSION = "version";
    /** Field :: Database. */
    public static final String FIELD_DATABASE = "database";
    /** Field :: Persistent. */
    public static final String FIELD_PERSISTENT = "persistent";
    public static final String FIELD_STORAGE_CEILING = "storageCeiling";
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
    // Following is the part of the URLs for all REST-based requests. It is
    // divided into the base call and the sub calls.
    // =========================================================================

    /**
     * Root of the REST API, and of the WebSocket. Rune is served at the
     * root of the stone (`/`), the API below it, with its version in the path.
     */
    public static final String REST_API = "/api/v1";

    // General requests from the Management Interface
    /** REST endpoint for the Version Service. */
    public static final String REST_VERSION = "/version";
    /** Path of the OpenAPI specification (PLAN.md, L2). */
    public static final String REST_SPECIFICATION = "/openapi.json";
    /** REST endpoint for the Settings Service. */
    public static final String REST_SETTINGS = "/settings";
    /** REST endpoint for the MasterKey Service. */
    public static final String REST_MASTERKEY = "/masterKey";
    /** REST endpoint for the Sanitized Service. */
    public static final String REST_SANITIZED = "/sanitized";
    /** REST endpoint for the Authenticated Service. */
    public static final String REST_AUTHENTICATED = "/authenticated";

    // Member requests from the Management Interface
    /** PATH for the REST-based Members Service. */
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
    /** PATH for the REST-based Circle of Trust Service. */
    public static final String REST_CIRCLES_BASE = "/circles";
    /** REST endpoint for Creating a Circle of Trust. */
    public static final String REST_CIRCLES_CREATE = "/createCircle";
    /** REST endpoint for Updating a Circle of Trust. */
    public static final String REST_CIRCLES_UPDATE = "/updateCircle";
    /** REST endpoint for Deleting a Circle of Trust. */
    public static final String REST_CIRCLES_DELETE = "/deleteCircle";
    /** REST endpoint for Fetching Circles of Trust. */
    public static final String REST_CIRCLES_FETCH = "/fetchCircles";
    /** REST endpoint for Rotating the Circle Secret of a Circle of Trust. */
    public static final String REST_CIRCLES_ROTATE = "/rotateCircle";

    // Trustee requests from the Management Interface
    /** PATH for the REST-based Trustee Service. */
    public static final String REST_TRUSTEES_BASE = "/trustees";
    /** REST endpoint for Adding a Trustee. */
    public static final String REST_TRUSTEES_ADD = "/addTrustee";
    /** REST endpoint for Altering a Trustee. */
    public static final String REST_TRUSTEES_ALTER = "/alterTrustee";
    /** REST endpoint for Removing a Trustee. */
    public static final String REST_TRUSTEES_REMOVE = "/removeTrustee";
    /** REST endpoint to set the secrets of several Trustees of a Circle (E2E.md). */
    public static final String REST_TRUSTEES_SECRETS = "/setSecrets";
    /** REST endpoint for Fetching Trustees. */
    public static final String REST_TRUSTEES_FETCH = "/fetchTrustees";

    // DataType requests from the Share Interface
    /** PATH for the REST-based DataType Service. */
    public static final String REST_DATATYPES_BASE = "/dataTypes";
    /** REST endpoint for Processing a DataType. */
    public static final String REST_DATATYPES_PROCESS = "/processDataType";
    /** REST endpoint for Deleting a DataType. */
    public static final String REST_DATATYPES_DELETE = "/deleteDataType";
    /** REST endpoint for Fetching all DataTypes. */
    public static final String REST_DATATYPES_FETCH = "/fetchDataTypes";

    // Data requests from the Share Interface
    /** Path for the REST-based Data Service. */
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
    /** Path parameter for the Data Id of the content resource. */
    public static final String PATH_DATA_ID = "dataId";
    /** REST resource for the content of a Data Object (upload and download). */
    public static final String REST_DATA_CONTENT = "/{" + PATH_DATA_ID + "}/content";
    /** REST resource for a download token of a Data Object (PLAN.md, L5). */
    public static final String REST_DATA_TOKEN = "/{" + PATH_DATA_ID + "}/token";
    /** REST resource for the thumbnail of a Data Object (PLAN.md, L5). */
    public static final String REST_DATA_THUMBNAIL = "/{" + PATH_DATA_ID + "}/thumbnail";
    /** Query parameter with a download token, for clients that can't send headers. */
    public static final String QUERY_TOKEN = "token";
    /** WebSocket endpoint of the stone (FEATURES.md): the features of the Circles, and what changes in them. */
    public static final String WS_SOCKET = REST_API + "/socket";

    // =========================================================================

    private Constants() {
        // Private Constructor, this is a Constants Class.
    }
}
