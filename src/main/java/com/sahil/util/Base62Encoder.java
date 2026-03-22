package com.sahil.util;

/**
 * Base62 encoder/decoder for converting long IDs to short alphanumeric codes
 * This ensures minimal short URL length while maintaining uniqueness
 */
public class Base62Encoder {
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    /**
     * Encodes a long ID to a base62 string
     * @param num The numeric ID to encode
     * @return Base62 encoded string
     */
    public static String encode(long num) {
        if (num == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(ALPHABET.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a base62 string to a long ID
     * @param str The base62 encoded string
     * @return The decoded long value
     */
    public static long decode(String str) {
        long num = 0;
        for (char c : str.toCharArray()) {
            num = num * BASE + ALPHABET.indexOf(c);
        }
        return num;
    }

    /**
     * Generates a short code from an ID ensuring it's within expected length
     * @param id The unique ID
     * @return A short code suitable for URLs (typically 6-10 chars)
     */
    public static String generateShortCode(long id) {
        String encoded = encode(id);
        // Ensure reasonable length for URLs
        return encoded.length() > 10 ? encoded.substring(0, 10) : encoded;
    }
}
