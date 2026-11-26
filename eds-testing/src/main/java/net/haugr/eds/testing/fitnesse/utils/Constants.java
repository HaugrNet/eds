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
package net.haugr.eds.testing.fitnesse.utils;

/**
 * <p>Values of EDS which the tests need, but which aren't part of the API
 * contract. The fixtures only use the generated client, not eds-api.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class Constants {

    /** Account name of the System Administrator. */
    public static final String ADMIN_ACCOUNT = "admin";
    /** Name of the built-in DataType for folders. */
    public static final String FOLDER_TYPENAME = "folder";
    /** Name of the built-in DataType for data. */
    public static final String DATA_TYPENAME = "data";
    /** Name of the built-in DataType for the entries of features. */
    public static final String ENTRY_TYPENAME = "entry";
    public static final String THUMBNAIL_TYPENAME = "thumbnail";
    /** Largest page size of a listing. */
    public static final int MAX_PAGE_SIZE = 100;

    private Constants() {
        // Private Constructor, this is a utility Class.
    }
}
