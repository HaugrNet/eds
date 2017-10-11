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
 * The Sorting Order to be used.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlType(name = "sortingOrder")
public enum SortingOrder {

    /** For Sorting in ascending order. */
    ASC,

    /** For sorting in descending order. */
    DESC
}
