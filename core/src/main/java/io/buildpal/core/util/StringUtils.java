///*
// * Copyright 2017 Buildpal
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.buildpal.core.util;
//
//public class StringUtils {
//    /**
//     * A String for a space character.
//     *
//     * @since 3.2
//     */
//    public static final String SPACE = " ";
//
//    /**
//     * The empty String {@code ""}.
//     * @since 2.0
//     */
//    public static final String EMPTY = "";
//
//    /**
//     * Represents a failed index search.
//     * @since 2.1
//     */
//    public static final int INDEX_NOT_FOUND = -1;
//
//    /**
//     * <p>Checks if a CharSequence is empty ("") or null.</p>
//     *
//     * <pre>
//     * StringUtils.isEmpty(null)      = true
//     * StringUtils.isEmpty("")        = true
//     * StringUtils.isEmpty(" ")       = false
//     * StringUtils.isEmpty("bob")     = false
//     * StringUtils.isEmpty("  bob  ") = false
//     * </pre>
//     *
//     * <p>NOTE: This method changed in Lang version 2.0.
//     * It no longer trims the CharSequence.
//     * That functionality is available in isBlank().</p>
//     *
//     * @param cs  the CharSequence to check, may be null
//     * @return {@code true} if the CharSequence is empty or null
//     * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
//     */
//    public static boolean isEmpty(final CharSequence cs) {
//        return cs == null || cs.length() == 0;
//    }
//
//    /**
//     * <p>Checks if a CharSequence is not empty ("") and not null.</p>
//     *
//     * <pre>
//     * StringUtils.isNotEmpty(null)      = false
//     * StringUtils.isNotEmpty("")        = false
//     * StringUtils.isNotEmpty(" ")       = true
//     * StringUtils.isNotEmpty("bob")     = true
//     * StringUtils.isNotEmpty("  bob  ") = true
//     * </pre>
//     *
//     * @param cs  the CharSequence to check, may be null
//     * @return {@code true} if the CharSequence is not empty and not null
//     * @since 3.0 Changed signature from isNotEmpty(String) to isNotEmpty(CharSequence)
//     */
//    public static boolean isNotEmpty(final CharSequence cs) {
//        return !isEmpty(cs);
//    }
//
//    /**
//     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
//     *
//     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
//     *
//     * <pre>
//     * StringUtils.isBlank(null)      = true
//     * StringUtils.isBlank("")        = true
//     * StringUtils.isBlank(" ")       = true
//     * StringUtils.isBlank("bob")     = false
//     * StringUtils.isBlank("  bob  ") = false
//     * </pre>
//     *
//     * @param cs  the CharSequence to check, may be null
//     * @return {@code true} if the CharSequence is null, empty or whitespace only
//     * @since 2.0
//     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
//     */
//    public static boolean isBlank(final CharSequence cs) {
//        int strLen;
//        if (cs == null || (strLen = cs.length()) == 0) {
//            return true;
//        }
//        for (int i = 0; i < strLen; i++) {
//            if (Character.isWhitespace(cs.charAt(i)) == false) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only.</p>
//     *
//     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
//     *
//     * <pre>
//     * StringUtils.isNotBlank(null)      = false
//     * StringUtils.isNotBlank("")        = false
//     * StringUtils.isNotBlank(" ")       = false
//     * StringUtils.isNotBlank("bob")     = true
//     * StringUtils.isNotBlank("  bob  ") = true
//     * </pre>
//     *
//     * @param cs  the CharSequence to check, may be null
//     * @return {@code true} if the CharSequence is
//     *  not empty and not null and not whitespace only
//     * @since 2.0
//     * @since 3.0 Changed signature from isNotBlank(String) to isNotBlank(CharSequence)
//     */
//    public static boolean isNotBlank(final CharSequence cs) {
//        return !isBlank(cs);
//    }
//
//    /**
//     * <p>Removes control characters (char &lt;= 32) from both
//     * ends of this String, handling {@code null} by returning
//     * {@code null}.</p>
//     *
//     * <p>The String is trimmed using {@link String#trim()}.
//     * Trim removes start and end characters &lt;= 32.
//     * To strip whitespace use {@link #strip(String)}.</p>
//     *
//     * <p>To trim your choice of characters, use the
//     * {@link #strip(String, String)} methods.</p>
//     *
//     * <pre>
//     * StringUtils.trim(null)          = null
//     * StringUtils.trim("")            = ""
//     * StringUtils.trim("     ")       = ""
//     * StringUtils.trim("abc")         = "abc"
//     * StringUtils.trim("    abc    ") = "abc"
//     * </pre>
//     *
//     * @param str  the String to be trimmed, may be null
//     * @return the trimmed string, {@code null} if null String input
//     */
//    public static String trim(final String str) {
//        return str == null ? null : str.trim();
//    }
//
//    /**
//     * <p>Checks if the CharSequence contains only uppercase characters.</p>
//     *
//     * <p>{@code null} will return {@code false}.
//     * An empty String (length()=0) will return {@code false}.</p>
//     *
//     * <pre>
//     * StringUtils.isAllUpperCase(null)   = false
//     * StringUtils.isAllUpperCase("")     = false
//     * StringUtils.isAllUpperCase("  ")   = false
//     * StringUtils.isAllUpperCase("ABC")  = true
//     * StringUtils.isAllUpperCase("aBC")  = false
//     * StringUtils.isAllUpperCase("A C")  = false
//     * StringUtils.isAllUpperCase("A1C")  = false
//     * StringUtils.isAllUpperCase("A/C")  = false
//     * </pre>
//     *
//     * @param cs the CharSequence to check, may be null
//     * @return {@code true} if only contains uppercase characters, and is non-null
//     * @since 2.5
//     * @since 3.0 Changed signature from isAllUpperCase(String) to isAllUpperCase(CharSequence)
//     */
//    public static boolean isAllUpperCase(final CharSequence cs) {
//        if (cs == null || isEmpty(cs)) {
//            return false;
//        }
//        final int sz = cs.length();
//        for (int i = 0; i < sz; i++) {
//            if (Character.isUpperCase(cs.charAt(i)) == false) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * <p>Replaces all occurrences of a String within another String.</p>
//     *
//     * <p>A {@code null} reference passed to this method is a no-op.</p>
//     *
//     * <pre>
//     * StringUtils.replace(null, *, *)        = null
//     * StringUtils.replace("", *, *)          = ""
//     * StringUtils.replace("any", null, *)    = "any"
//     * StringUtils.replace("any", *, null)    = "any"
//     * StringUtils.replace("any", "", *)      = "any"
//     * StringUtils.replace("aba", "a", null)  = "aba"
//     * StringUtils.replace("aba", "a", "")    = "b"
//     * StringUtils.replace("aba", "a", "z")   = "zbz"
//     * </pre>
//     *
//     * @see #replace(String text, String searchString, String replacement, int max)
//     * @param text  text to search and replace in, may be null
//     * @param searchString  the String to search for, may be null
//     * @param replacement  the String to replace it with, may be null
//     * @return the text with any replacements processed,
//     *  {@code null} if null String input
//     */
//    public static String replace(final String text, final String searchString, final String replacement) {
//        return replace(text, searchString, replacement, -1);
//    }
//
//    /**
//     * <p>Replaces a String with another String inside a larger String,
//     * for the first {@code max} values of the search String.</p>
//     *
//     * <p>A {@code null} reference passed to this method is a no-op.</p>
//     *
//     * <pre>
//     * StringUtils.replace(null, *, *, *)         = null
//     * StringUtils.replace("", *, *, *)           = ""
//     * StringUtils.replace("any", null, *, *)     = "any"
//     * StringUtils.replace("any", *, null, *)     = "any"
//     * StringUtils.replace("any", "", *, *)       = "any"
//     * StringUtils.replace("any", *, *, 0)        = "any"
//     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
//     * StringUtils.replace("abaa", "a", "", -1)   = "b"
//     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
//     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
//     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
//     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
//     * </pre>
//     *
//     * @param text  text to search and replace in, may be null
//     * @param searchString  the String to search for, may be null
//     * @param replacement  the String to replace it with, may be null
//     * @param max  maximum number of values to replace, or {@code -1} if no maximum
//     * @return the text with any replacements processed,
//     *  {@code null} if null String input
//     */
//    public static String replace(final String text, final String searchString, final String replacement, final int max) {
//        return replace(text, searchString, replacement, max, false);
//    }
//
//    /**
//     * <p>Replaces a String with another String inside a larger String,
//     * for the first {@code max} values of the search String,
//     * case sensitively/insensisitively based on {@code ignoreCase} value.</p>
//     *
//     * <p>A {@code null} reference passed to this method is a no-op.</p>
//     *
//     * <pre>
//     * StringUtils.replace(null, *, *, *, false)         = null
//     * StringUtils.replace("", *, *, *, false)           = ""
//     * StringUtils.replace("any", null, *, *, false)     = "any"
//     * StringUtils.replace("any", *, null, *, false)     = "any"
//     * StringUtils.replace("any", "", *, *, false)       = "any"
//     * StringUtils.replace("any", *, *, 0, false)        = "any"
//     * StringUtils.replace("abaa", "a", null, -1, false) = "abaa"
//     * StringUtils.replace("abaa", "a", "", -1, false)   = "b"
//     * StringUtils.replace("abaa", "a", "z", 0, false)   = "abaa"
//     * StringUtils.replace("abaa", "A", "z", 1, false)   = "abaa"
//     * StringUtils.replace("abaa", "A", "z", 1, true)   = "zbaa"
//     * StringUtils.replace("abAa", "a", "z", 2, true)   = "zbza"
//     * StringUtils.replace("abAa", "a", "z", -1, true)  = "zbzz"
//     * </pre>
//     *
//     * @param text  text to search and replace in, may be null
//     * @param searchString  the String to search for (case insensitive), may be null
//     * @param replacement  the String to replace it with, may be null
//     * @param max  maximum number of values to replace, or {@code -1} if no maximum
//     * @param ignoreCase if true replace is case insensitive, otherwise case sensitive
//     * @return the text with any replacements processed,
//     *  {@code null} if null String input
//     */
//    private static String replace(final String text, String searchString, final String replacement, int max, final boolean ignoreCase) {
//        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
//            return text;
//        }
//        String searchText = text;
//        if (ignoreCase) {
//            searchText = text.toLowerCase();
//            searchString = searchString.toLowerCase();
//        }
//        int start = 0;
//        int end = searchText.indexOf(searchString, start);
//        if (end == INDEX_NOT_FOUND) {
//            return text;
//        }
//        final int replLength = searchString.length();
//        int increase = replacement.length() - replLength;
//        increase = increase < 0 ? 0 : increase;
//        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
//        final StringBuilder buf = new StringBuilder(text.length() + increase);
//        while (end != INDEX_NOT_FOUND) {
//            buf.append(text.substring(start, end)).append(replacement);
//            start = end + replLength;
//            if (--max == 0) {
//                break;
//            }
//            end = searchText.indexOf(searchString, start);
//        }
//        buf.append(text.substring(start));
//        return buf.toString();
//    }
//}
