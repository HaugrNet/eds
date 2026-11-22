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
package net.haugr.eds.core.features;

/**
 * <p>Which entries of a feature a list asks for, and how (FEATURES.md): the
 * filters, the order, the size of the page and whether the content comes
 * along. Every filter is optional; without any, the newest entries of all
 * kinds come first, with their content.</p>
 *
 * <p>The same query serves the request over REST and the frame over the
 * socket, so both ask in one way.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
public final class EntryQuery {

    private String kind = null;
    private String about = null;
    private String namePrefix = null;
    private Long after = null;
    private Long before = null;
    private boolean oldestFirst = false;
    private Integer limit = null;
    private boolean content = true;

    private EntryQuery() {
        // Made by all(), then narrowed by the methods below.
    }

    /**
     * A query for all entries, the newest first, with their content.
     *
     * @return A new query
     */
    public static EntryQuery all() {
        return new EntryQuery();
    }

    /**
     * Only entries of this kind.
     *
     * @param kind The kind, or null for all
     * @return This query
     */
    public EntryQuery kind(final String kind) {
        this.kind = kind;
        return this;
    }

    /**
     * Only entries about the entry with this ID.
     *
     * @param about ID of the entry, or null
     * @return This query
     */
    public EntryQuery about(final String about) {
        this.about = about;
        return this;
    }

    /**
     * Only entries whose name starts with this.
     *
     * @param namePrefix The start of the name, or null
     * @return This query
     */
    public EntryQuery namePrefix(final String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    /**
     * Only entries after this position.
     *
     * @param after The position, or null
     * @return This query
     */
    public EntryQuery after(final Long after) {
        this.after = after;
        return this;
    }

    /**
     * Only entries before this position.
     *
     * @param before The position, or null
     * @return This query
     */
    public EntryQuery before(final Long before) {
        this.before = before;
        return this;
    }

    /**
     * The order of the entries.
     *
     * @param oldestFirst True for the oldest first; null or false for the newest first
     * @return This query
     */
    public EntryQuery oldestFirst(final Boolean oldestFirst) {
        this.oldestFirst = Boolean.TRUE.equals(oldestFirst);
        return this;
    }

    /**
     * The most entries to return.
     *
     * @param limit The number, or null for the default
     * @return This query
     */
    public EntryQuery limit(final Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Whether the content of the entries comes along.
     *
     * @param content False to leave the content out; null or true to include it
     * @return This query
     */
    public EntryQuery content(final Boolean content) {
        this.content = !Boolean.FALSE.equals(content);
        return this;
    }

    /**
     * @return The kind asked for, or null for all
     */
    public String kind() {
        return kind;
    }

    /**
     * @return ID of the entry the entries are about, or null
     */
    public String about() {
        return about;
    }

    /**
     * @return The start of the names asked for, or null
     */
    public String namePrefix() {
        return namePrefix;
    }

    /**
     * @return The position the entries come after, or null
     */
    public Long after() {
        return after;
    }

    /**
     * @return The position the entries come before, or null
     */
    public Long before() {
        return before;
    }

    /**
     * @return True for the oldest first
     */
    public boolean oldestFirst() {
        return oldestFirst;
    }

    /**
     * @return The most entries to return, or null for the default
     */
    public Integer limit() {
        return limit;
    }

    /**
     * @return True if the content of the entries comes along
     */
    public boolean content() {
        return content;
    }
}
