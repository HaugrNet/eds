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
package net.haugr.eds.core.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>A part of the encrypted data of a Data Object, a row of
 * {@code eds_data_parts} (PLAN.md, Streaming). The data is stored as a row
 * for each part, as no database reads a range of one large value the same
 * way, and some read all of it to give a part. A part is a range of the
 * bytes and nothing more: where one ends and the next starts says nothing
 * about the content.</p>
 *
 * <p>Each part tells where it starts and how long it is, so a range is found
 * by these two alone, and the size of the parts may change without anything
 * stored having to be written anew. The first part is short
 * ({@link #HEAD_SIZE}), so the start of the data, its header, is read
 * without the rest.</p>
 *
 * @author Kim Jensen
 * @since EDS 2.0
 */
@Embeddable
public class DataPart {

    /** The most bytes of the first part, which holds the start of the data. */
    public static final int HEAD_SIZE = 4096;
    /** The bytes of a part after the first, unless another size is given. */
    public static final int DEFAULT_SIZE = 4 * 1024 * 1024;
    /** The fewest bytes a part after the first is given room for. */
    public static final int MIN_SIZE = 64 * 1024;
    /** The most bytes of a part: no larger value is written to a row. */
    public static final int MAX_SIZE = 16 * 1024 * 1024;

    @Column(name = "part_offset", nullable = false, updatable = false)
    private long start = 0;

    @Column(name = "part_length", nullable = false, updatable = false)
    private int length = 0;

    @Column(name = "part", nullable = false, updatable = false)
    private byte[] bytes = null;

    /**
     * Default Constructor, for JPA.
     */
    public DataPart() {
        // Required by JPA.
    }

    private DataPart(final long start, final byte[] bytes) {
        this.start = start;
        this.length = bytes.length;
        this.bytes = bytes;
    }

    /**
     * The size of the parts after the first, for a wanted size: within
     * {@link #MIN_SIZE} and {@link #MAX_SIZE}.
     *
     * @param wanted The wanted size, e.g. from a setting
     * @return The size to use
     */
    public static int size(final int wanted) {
        return Math.clamp(wanted, MIN_SIZE, MAX_SIZE);
    }

    /**
     * The size of the part which starts at the given offset: the first part
     * is short, the others have the given size.
     *
     * @param offset   Where the part starts, from 0
     * @param partSize The size of the parts after the first
     * @return The most bytes of the part
     */
    public static int sizeAt(final long offset, final int partSize) {
        return (offset == 0) ? Math.min(HEAD_SIZE, partSize) : partSize;
    }

    /**
     * Splits data which is held in memory into its parts.
     *
     * @param data The data, which may be null or empty
     * @return The parts in their order; none for no data
     */
    public static List<DataPart> split(final byte[] data) {
        final List<DataPart> parts = new ArrayList<>();
        if (data != null) {
            int offset = 0;
            while (offset < data.length) {
                final int size = Math.min(sizeAt(offset, DEFAULT_SIZE), data.length - offset);
                final byte[] bytes = new byte[size];
                System.arraycopy(data, offset, bytes, 0, size);
                parts.add(new DataPart(offset, bytes));
                offset += size;
            }
        }

        return parts;
    }

    /**
     * Puts the parts of data which is held in memory together again.
     *
     * @param parts The parts, in any order
     * @return The data; empty for no parts
     */
    public static byte[] join(final List<DataPart> parts) {
        final List<DataPart> ordered = new ArrayList<>(parts);
        ordered.sort(Comparator.comparingLong(DataPart::getStart));
        final long total = ordered.stream().mapToLong(part -> part.bytes.length).sum();
        final byte[] data = new byte[Math.toIntExact(total)];
        int offset = 0;
        for (final DataPart part : ordered) {
            System.arraycopy(part.bytes, 0, data, offset, part.bytes.length);
            offset += part.bytes.length;
        }

        return data;
    }

    public long getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }
}
