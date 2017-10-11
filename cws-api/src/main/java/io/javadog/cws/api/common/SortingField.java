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
 * This enum contains all allowed fields to be sorted by. However, the
 * individual requests will only allow a sub-set of these, so please check the
 * documentation in the Request for the actually allowed fields.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlType(name = "sortingField")
public enum SortingField {

    /**
     * The default sort order is the date of creation (timestamp), i.e. at which
     * time the Object was saved in the database.
     */
    CREATED,

    /** The sorting will be made on the last updated (timestamp). */
    MODIFIED,

    /** The principal name of the Object to sort by. */
    NAME

}
