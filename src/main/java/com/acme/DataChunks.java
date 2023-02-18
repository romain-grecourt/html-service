package com.acme;

import java.nio.ByteBuffer;
import java.util.function.Function;

import io.helidon.common.http.DataChunk;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@link DataChunk} utility.
 */
public class DataChunks {

    private DataChunks() {
    }

    /**
     * Create a function that automatically releases a chunk.
     *
     * @param mapper a function that converts a data-chunk
     * @param <T>    the converted item type
     * @return function
     */
    public static <T> Function<DataChunk, T> release(Function<DataChunk, T> mapper) {
        return chunk -> {
            try {
                return mapper.apply(chunk);
            } finally {
                chunk.release();
            }
        };
    }

    /**
     * Create a new {@link DataChunk} for the given string.
     *
     * @param s string
     * @return chunk
     */
    public static DataChunk create(String s) {
        return DataChunk.create(true, ByteBuffer.wrap(s.getBytes(UTF_8)));
    }
}
