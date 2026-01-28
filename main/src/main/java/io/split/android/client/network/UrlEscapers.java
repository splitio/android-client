package io.split.android.client.network;

/**
 * Based on Guava UrlEscapers
 */
final class UrlEscapers {
    private UrlEscapers() {}

    private static final String URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS =
            "-._~" // Unreserved characters.
                    + "!$'()*,;&=" // The subdelim characters (excluding '+').
                    + "@:"; // The gendelim characters permitted in paths.

    /**
     * Returns an instance that escapes strings so they can be safely included in <a
     * href="https://goo.gl/m2MIf0">URL path segments</a>. The returned escaper escapes all non-ASCII
     * characters, even though <a href="https://goo.gl/e7E0In">many of these are accepted in modern
     * URLs</a>. (<a href="https://goo.gl/jfVxXW">If the escaper were to leave these characters
     * unescaped, they would be escaped by the consumer at parse time, anyway.</a>) Additionally, the
     * escaper escapes the slash character ("/"). While slashes are acceptable in URL paths, they are
     * considered by the specification to be separators between "path segments." This implies that, if
     * you wish for your path to contain slashes, you must escape each segment separately and then
     * join them.
     *
     * <p>When escaping a String, the following rules apply:
     *
     * <ul>
     *   <li>The alphanumeric characters "a" through "z", "A" through "Z" and "0" through "9" remain
     *       the same.
     *   <li>The unreserved characters ".", "-", "~", and "_" remain the same.
     *   <li>The general delimiters "@" and ":" remain the same.
     *   <li>The subdelimiters "!", "$", "&amp;", "'", "(", ")", "*", "+", ",", ";", and "=" remain
     *       the same.
     *   <li>The space character " " is converted into %20.
     *   <li>All other characters are converted into one or more bytes using UTF-8 encoding and each
     *       byte is then represented by the 3-character string "%XY", where "XY" is the two-digit,
     *       uppercase, hexadecimal representation of the byte value.
     * </ul>
     *
     * <p><b>Note:</b> Unlike other escapers, URL escapers produce <a
     * href="https://url.spec.whatwg.org/#percent-encode">uppercase</a> hexadecimal sequences.
     */
    public static PercentEscaper urlPathSegmentEscaper() {
        return URL_PATH_SEGMENT_ESCAPER;
    }

    private static final PercentEscaper URL_PATH_SEGMENT_ESCAPER =
            new PercentEscaper(URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS + "+", false);

}
