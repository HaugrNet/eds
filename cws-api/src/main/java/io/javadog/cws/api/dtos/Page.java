/*
 * =============================================================================
 * Copyright (c) 2016-2017, JavaDog.io
 * -----------------------------------------------------------------------------
 * Project: CWS (cws-api)
 * =============================================================================
 */
package io.javadog.cws.api.dtos;

import io.javadog.cws.api.common.Constants;
import io.javadog.cws.api.common.SortingField;
import io.javadog.cws.api.common.SortingOrder;
import io.javadog.cws.api.common.Verifiable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paging information for retrieving larger amounts of data. This will ensure
 * that the results can be retrieved in consistent blocks.
 *
 * @author Kim Jensen
 * @since  CWS 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "page", propOrder = { "pageNumber", "pageSize", "sortOrder", "sortBy" })
public final class Page extends Verifiable {

    /** {@link Constants#SERIAL_VERSION_UID}. */
    private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

    private static final String FIELD_PAGE_NUMBER = "pageNumber";
    private static final String FIELD_PAGE_SIZE = "pageSize";
    private static final String FIELD_SORT_ORDER = "sortOrder";
    private static final String FIELD_SORT_BY = "sortBy";

    /**
     * The maximum allowed number of objects to be retrieved in a single
     * request.
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * The first page to be read out is starting with 1.
     */
    private static final int FIRST_PAGE = 1;

    @XmlElement(name = FIELD_PAGE_NUMBER, required = true)
    private int pageNumber;

    @XmlElement(name = FIELD_PAGE_SIZE, required = true)
    private int pageSize;

    @XmlElement(name = FIELD_SORT_ORDER, required = true)
    private SortingOrder sortOrder;

    @XmlElement(name = FIELD_SORT_BY, required = true)
    private SortingField sortBy;

    /**
     * Empty Constructor.
     */
    public Page() {
        pageNumber = FIRST_PAGE;
        pageSize = MAX_PAGE_SIZE;

        // Default, we're sorting based on creation in descending order,
        // meaning that the most recently created Object is the first to
        // be returned.
        sortOrder = SortingOrder.DESC;
        sortBy = SortingField.CREATED;
    }

    /**
     * Full Constructor, sets all Paginating fields.
     *
     * @param pageNumber  The current page to fetch, starting from 0 (zero)
     * @param pageSize    The max number of records on each page
     * @param sortOrder   Sorting Order
     * @param sortBy      Sorting Field
     */
    public Page(final int pageNumber, final int pageSize, final SortingOrder sortOrder, final SortingField sortBy) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sortOrder = sortOrder;
        this.sortBy = sortBy;
    }

    public void setPageNumber(final int pageNumber) {
        ensureValidRange(FIELD_PAGE_NUMBER, pageNumber, 1, MAX_PAGE_SIZE);
        this.pageNumber = pageNumber;
    }

    /**
     * Retrieves the Current Page Number for this fetch request.
     *
     * @return Current Page number
     */
    public int pageNumber() {
        return pageNumber;
    }

    public void setPageSize(final int pageSize) {
        ensurePositiveNumber(FIELD_PAGE_SIZE, pageSize);
        this.pageSize = pageSize;
    }

    /**
     * Retrieves the Current Page Size for this fetch request.
     *
     * @return Current Page Size
     */
    public int pageSize() {
        return pageSize;
    }

    /**
     * Sets the Sorting Order for the page.
     *
     * @param sortOrder Sorting Order
     */
    public void setSortOrder(final SortingOrder sortOrder) {
        ensureValidEntry(FIELD_SORT_ORDER, sortOrder, Arrays.asList(SortingOrder.values()));
        this.sortOrder = sortOrder;
    }

    /**
     * Retrieves the Current Sort Order for this fetch request.
     *
     * @return Sorting Order
     */
    public SortingOrder sortOrder() {
        return sortOrder;
    }

    public void setSortBy(final SortingField sortBy) {
        ensureValidEntry(FIELD_SORT_BY, sortBy, Arrays.asList(SortingField.values()));
        this.sortBy = sortBy;
    }

    /**
     * Retrieves the Current Sort By Field. Note, that all fields that is
     * allowed to be sorted by is defined in the SortingField enumeration.
     * However, the actually allowed values are request specific, so please
     * check the documentation for the setting to see which fields may be used.
     *
     * @return Current Sort By Field
     */
    public SortingField sortBy() {
        return sortBy;
    }

    // =========================================================================
    // Standard Methods
    // =========================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> validate() {
        final Map<String, String> errors = new ConcurrentHashMap<>();

        if (pageNumber < 1) {
            errors.put(FIELD_PAGE_NUMBER, "The Page Number must be at least 1.");
        }
        if ((pageSize < 1) || (pageSize > MAX_PAGE_SIZE)) {
            errors.put(FIELD_PAGE_SIZE, "The Page Size must be at least 1 and no more than " + MAX_PAGE_SIZE + '.');
        }
        if (sortOrder == null) {
            errors.put(FIELD_SORT_ORDER, "The Sorting Order is missing.");
        }
        if (sortBy == null) {
            errors.put(FIELD_SORT_BY, "The Sorting Field is missing.");
        }

        return errors;
    }
}
