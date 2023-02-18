package com.acme;

import java.nio.ByteBuffer;

/**
 * UTF-8 decoder.
 */
class UTF8Decoder {

    private int count = 0; // byte count
    private byte b1; // first byte
    private byte b2; // second byte

    /**
     * Decode the valid UTF-8 characters in the given buffers into a single string.
     * Poorly inspired from {@code sun.nio.cs.UTF_8}.
     *
     * @param byteBuffers buffers
     * @return string
     */
    public String decode(Iterable<ByteBuffer> byteBuffers) {
        StringBuilder sb = new StringBuilder();
        for (ByteBuffer byteBuffer : byteBuffers) {
            int remaining = byteBuffer.remaining();
            while (remaining > 0) {
                if (count == 0) {
                    b1 = byteBuffer.get();
                    count++;
                    remaining--;
                }
                char c = '?';
                if (b1 >= 0) {
                    c = (char) b1;
                } else if ((b1 >> 5) == -2 && (b1 & 0x1e) != 0) {
                    // 2 bytes, 11 bits: 110xxxxx 10xxxxxx
                    if (remaining > 0) {
                        int b2 = byteBuffer.get();
                        if ((b2 & 0xc0) == 0x80) {
                            c = (char) (((b1 << 6) ^ b2)
                                    ^ (((byte) 0xC0 << 6)
                                    ^ ((byte) 0x80)));
                        }
                    } else {
                        break;
                    }
                } else if ((b1 >> 4) == -2) {
                    // 3 bytes, 16 bits: 1110xxxx 10xxxxxx 10xxxxxx
                    if (count == 1) {
                        if (remaining > 0) {
                            b2 = byteBuffer.get();
                            remaining--;
                            count++;
                        } else {
                            break;
                        }
                    }
                    if (remaining > 0) {
                        int b3 = byteBuffer.get();
                        if (!((b1 == (byte) 0xe0 && (b2 & 0xe0) == 0x80)
                                || (b2 & 0xc0) != 0x80 || (b3 & 0xc0) != 0x80)) {
                            c = (char) ((b1 << 12)
                                    ^ (b2 << 6)
                                    ^ (b3
                                    ^ (((byte) 0xE0 << 12)
                                    ^ ((byte) 0x80 << 6)
                                    ^ ((byte) 0x80))));
                        }
                    } else {
                        break;
                    }
                }
                sb.append(c);
                count = 0;
            }
        }
        return sb.toString();
    }
}
