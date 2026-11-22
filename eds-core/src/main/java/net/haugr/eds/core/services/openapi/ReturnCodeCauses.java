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
package net.haugr.eds.core.services.openapi;

import java.util.EnumMap;
import java.util.Map;
import net.haugr.eds.api.common.ReturnCode;

/**
 * <p>When each return code occurs, for the client-facing documentation. The
 * texts describe where EDS raises the codes (the managers and exceptions in
 * eds-core). Every {@link ReturnCode} must have an entry; the unit test
 * {@code ReturnCodeCausesTest} enforces that.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
final class ReturnCodeCauses {

    private static final Map<ReturnCode, String> CAUSES = new EnumMap<>(ReturnCode.class);

    static {
        CAUSES.put(ReturnCode.SUCCESS, "The request was completed.");
        CAUSES.put(ReturnCode.WARNING, "Not used at the moment; reserved for general warnings.");
        CAUSES.put(ReturnCode.AUTHORIZATION_WARNING, "The Member lacks the required role (System Administrator) or trust " +
                "level in the Circle for the operation. Also used when changing the default Data Types.");
        CAUSES.put(ReturnCode.AUTHENTICATION_WARNING, "The account is unknown, the credentials are wrong, or the session " +
                "is unknown or has expired. Also used when a Member other than admin calls the MasterKey operation.");
        CAUSES.put(ReturnCode.IDENTIFICATION_WARNING, "A referenced Member, Circle, Trustee, Data Object, Folder or Data " +
                "Type does not exist or is not visible to the Member, or a name is already in use (new Member, Circle " +
                "or Data Object).");
        CAUSES.put(ReturnCode.ILLEGAL_ACTION, "The operation is not allowed in this situation, e.g. copying or " +
                "moving folders, or changing the System Administrator account.");
        CAUSES.put(ReturnCode.VERIFICATION_WARNING, "The request failed validation: required fields are missing, values " +
                "are too long or IDs are invalid. returnMessage lists the fields.");
        CAUSES.put(ReturnCode.INTEGRITY_WARNING, "The operation would break the data structure, e.g. deleting a folder " +
                "that is not empty, or using an unknown Data Type or folder.");
        CAUSES.put(ReturnCode.SETTING_WARNING, "The setting can't be added, changed or deleted, or its value is invalid.");
        CAUSES.put(ReturnCode.ERROR, "An unexpected error occurred in EDS; returnMessage has the details.");
        CAUSES.put(ReturnCode.NETWORK_ERROR, "EDS could not read from a network resource, e.g. the MasterKey URL.");
        CAUSES.put(ReturnCode.PRECONDITION_FAILED, "If-Match doesn't match the ETag of the stored content.");
        CAUSES.put(ReturnCode.CONTENT_TOO_LARGE, "A content upload larger than eds.stream.max.object.size.");
        CAUSES.put(ReturnCode.RANGE_NOT_SATISFIABLE, "A content download with a Range outside the content.");
        CAUSES.put(ReturnCode.INSUFFICIENT_STORAGE, "Storing the content would exceed eds.data.max.total.size, the ceiling for all stored content.");
        CAUSES.put(ReturnCode.CRYPTO_ERROR, "A cryptographic operation failed, e.g. a key could not be unlocked, or EDS is locked because the MasterKey is not loaded.");
        CAUSES.put(ReturnCode.INTEGRITY_ERROR, "Stored data failed its integrity check (corrupted), or the Circle's " +
                "folder structure is broken. Needs an administrator.");
        CAUSES.put(ReturnCode.SETTING_ERROR, "A stored setting could not be read.");
        CAUSES.put(ReturnCode.DATABASE_ERROR, "The database is not ready or not valid, or a database operation failed.");
        CAUSES.put(ReturnCode.CONSTRAINT_ERROR, "A unique value already exists: the account name of an invitation, or " +
                "a new account name when renaming.");
        CAUSES.put(ReturnCode.ILLEGAL_SERVICE, "Not used at the moment; reserved for requests to services that don't exist.");
    }

    private ReturnCodeCauses() {
        // Utility class, no instances.
    }

    /**
     * Returns when the given return code occurs.
     *
     * @param returnCode The return code
     * @return The explanation, or an empty string if none is defined
     */
    static String of(final ReturnCode returnCode) {
        return CAUSES.getOrDefault(returnCode, "");
    }
}
