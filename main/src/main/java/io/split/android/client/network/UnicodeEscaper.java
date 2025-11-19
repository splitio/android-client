package io.split.android.client.network;

import static io.split.android.client.utils.Utils.checkNotNull;

/**
 * Based on Guava UnicodeEscaper
 */
abstract class UnicodeEscaper {

    private static final int DEST_PAD = 32;

    protected UnicodeEscaper() {}

    protected abstract char[] escape(int cp);

    public String escape(String string) {
        checkNotNull(string);
        int end = string.length();
        int index = nextEscapeIndex(string, 0, end);
        return index == end ? string : escapeSlow(string, index);
    }

    protected int nextEscapeIndex(CharSequence csq, int start, int end) {
        int index = start;
        while (index < end) {
            int cp = codePointAt(csq, index, end);
            if (cp < 0 || escape(cp) != null) {
                break;
            }
            index += Character.isSupplementaryCodePoint(cp) ? 2 : 1;
        }
        return index;
    }

    /**
     * Returns the escaped form of a given literal string, starting at the given index. This method is
     * called by the {@link #escape(String)} method when it discovers that escaping is required. It is
     * protected to allow subclasses to override the fastpath escaping function to inline their
     * escaping test.
     *
     * <p>This method is not reentrant and may only be invoked by the top level {@link
     * #escape(String)} method.
     *
     * @param s the literal string to be escaped
     * @param index the index to start escaping from
     * @return the escaped form of {@code string}
     * @throws NullPointerException if {@code string} is null
     * @throws IllegalArgumentException if invalid surrogate characters are encountered
     */
    protected final String escapeSlow(String s, int index) {
        int end = s.length();

        // Get a destination buffer and setup some loop variables.
        char[] dest = charBufferFromThreadLocal();
        int destIndex = 0;
        int unescapedChunkStart = 0;

        while (index < end) {
            int cp = codePointAt(s, index, end);
            if (cp < 0) {
                throw new IllegalArgumentException("Trailing high surrogate at end of input");
            }
            // It is possible for this to return null because nextEscapeIndex() may
            // (for performance reasons) yield some false positives but it must never
            // give false negatives.
            char[] escaped = escape(cp);
            int nextIndex = index + (Character.isSupplementaryCodePoint(cp) ? 2 : 1);
            if (escaped != null) {
                int charsSkipped = index - unescapedChunkStart;

                // This is the size needed to add the replacement, not the full
                // size needed by the string. We only regrow when we absolutely must.
                int sizeNeeded = destIndex + charsSkipped + escaped.length;
                if (dest.length < sizeNeeded) {
                    int destLength = sizeNeeded + (end - index) + DEST_PAD;
                    dest = growBuffer(dest, destIndex, destLength);
                }
                // If we have skipped any characters, we need to copy them now.
                if (charsSkipped > 0) {
                    s.getChars(unescapedChunkStart, index, dest, destIndex);
                    destIndex += charsSkipped;
                }
                if (escaped.length > 0) {
                    System.arraycopy(escaped, 0, dest, destIndex, escaped.length);
                    destIndex += escaped.length;
                }
                // If we dealt with an escaped character, reset the unescaped range.
                unescapedChunkStart = nextIndex;
            }
            index = nextEscapeIndex(s, nextIndex, end);
        }

        // Process trailing unescaped characters - no need to account for escaped
        // length or padding the allocation.
        int charsSkipped = end - unescapedChunkStart;
        if (charsSkipped > 0) {
            int endIndex = destIndex + charsSkipped;
            if (dest.length < endIndex) {
                dest = growBuffer(dest, destIndex, endIndex);
            }
            s.getChars(unescapedChunkStart, end, dest, destIndex);
            destIndex = endIndex;
        }
        return new String(dest, 0, destIndex);
    }

    /**
     * Returns the Unicode code point of the character at the given index.
     *
     * <p>Unlike {@link Character#codePointAt(CharSequence, int)} or {@link String#codePointAt(int)}
     * this method will never fail silently when encountering an invalid surrogate pair.
     *
     * <p>The behaviour of this method is as follows:
     *
     * <ol>
     *   <li>If {@code index >= end}, {@link IndexOutOfBoundsException} is thrown.
     *   <li><b>If the character at the specified index is not a surrogate, it is returned.</b>
     *   <li>If the first character was a high surrogate value, then an attempt is made to read the
     *       next character.
     *       <ol>
     *         <li><b>If the end of the sequence was reached, the negated value of the trailing high
     *             surrogate is returned.</b>
     *         <li><b>If the next character was a valid low surrogate, the code point value of the
     *             high/low surrogate pair is returned.</b>
     *         <li>If the next character was not a low surrogate value, then {@link
     *             IllegalArgumentException} is thrown.
     *       </ol>
     *   <li>If the first character was a low surrogate value, {@link IllegalArgumentException} is
     *       thrown.
     * </ol>
     *
     * @param seq the sequence of characters from which to decode the code point
     * @param index the index of the first character to decode
     * @param end the index beyond the last valid character to decode
     * @return the Unicode code point for the given index or the negated value of the trailing high
     *     surrogate character at the end of the sequence
     */
    protected static int codePointAt(CharSequence seq, int index, int end) {
        checkNotNull(seq);
        if (index < end) {
            char c1 = seq.charAt(index++);
            if (c1 < Character.MIN_HIGH_SURROGATE || c1 > Character.MAX_LOW_SURROGATE) {
                // Fast path (first test is probably all we need to do)
                return c1;
            } else if (c1 <= Character.MAX_HIGH_SURROGATE) {
                // If the high surrogate was the last character, return its inverse
                if (index == end) {
                    return -c1;
                }
                // Otherwise look for the low surrogate following it
                char c2 = seq.charAt(index);
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(c1, c2);
                }
                throw new IllegalArgumentException(
                        "Expected low surrogate but got char '"
                                + c2
                                + "' with value "
                                + (int) c2
                                + " at index "
                                + index
                                + " in '"
                                + seq
                                + "'");
            } else {
                throw new IllegalArgumentException(
                        "Unexpected low surrogate character '"
                                + c1
                                + "' with value "
                                + (int) c1
                                + " at index "
                                + (index - 1)
                                + " in '"
                                + seq
                                + "'");
            }
        }
        throw new IndexOutOfBoundsException("Index exceeds specified range");
    }

    private static char[] growBuffer(char[] dest, int index, int size) {
        if (size < 0) { // overflow - should be OutOfMemoryError but GWT/j2cl don't support it
            throw new AssertionError("Cannot increase internal buffer any further");
        }
        char[] copy = new char[size];
        if (index > 0) {
            System.arraycopy(dest, 0, copy, 0, index);
        }
        return copy;
    }

    static char[] charBufferFromThreadLocal() {
        return DEST_TL.get();
    }

    private static final ThreadLocal<char[]> DEST_TL =
            new ThreadLocal<char[]>() {
                @Override
                protected char[] initialValue() {
                    return new char[1024];
                }
            };
}
