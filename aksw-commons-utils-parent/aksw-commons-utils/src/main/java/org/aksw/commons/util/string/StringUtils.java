package org.aksw.commons.util.string;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.function.Predicate;

public class StringUtils
{
    /**
     * Return the number of end-of-line characters at the current position - if any.
     * For non-eol positions the result is 0
     *
     * @param str
     * @param charPos
     * @return
     */
    public static int getEolCharCount(String str, int charPos) {
        int result = 0;
        boolean newLineEncountered = false;
        for(int i = charPos; i < str.length(); ++i) {
            char c = str.charAt(i);
            if(c == '\n') {
                if(newLineEncountered) {
                    break;
                } else {
                    ++result;
                    newLineEncountered = true;
                }
            } else if(c == '\r') {
                ++result;
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Convert line and column index to the char position
     * line and column indexes are 1-based.
     * Using 0 for either argument yields -1
     *
     * @param str
     * @param line
     * @param col the column
     * @return the char pos or -1 if there was none w.r.t. line and col
     */
    public static int findCharPos(String str, int line, int col) {
        int result = -1;

        if(col > 0 && line > 0) {
            int charPos = 0;
            int x = 0;
            int y = 1;
            for(charPos = 0; charPos < str.length(); ++charPos) {
                char c = str.charAt(charPos);
                //int eolCharCount = getEolCharCount(str, charPos);
                ++x;

                if(y == line && x == col) {
                    result = charPos;
//        			if(eolCharCount > 0) {
//        				--result;
//        			}
                    break;
                }

                if(c == '\n') {
                    ++y;
                    x = 0;
                } else if(c == '\r') {
                    x = 0;
                }
            }
        }

        return result;
    }


    public static <T> String itemPerLine(T[] array) {
        return itemPerLine(Arrays.asList(array));
    }

    public static <T> String itemPerLine(Iterable<T> iterable) {
        String result = "";
        Iterator<T> it = iterable.iterator();
        int i = 0;
        while(it.hasNext()) {
            T item = it.next();
            result += "[" + (++i) + "] " + item;

            if(it.hasNext()) {
                result += "\n";
            }
        }

        return result;
    }


    /**
     * Removes prefixes that are also suffixes from a given string
     * e.g. strip('Hi', ') -> Hi
     *
     * @param str
     * @param chars
     * @return
     */
    public static String strip(String str, String ... chars)
    {
        for(String c : chars) {
            if(str.length() < 2)
                return str;

            if(str.startsWith(c) && str.endsWith(c))
                str = str.substring(1, str.length() - 1);
        }

        return str;
    }


    /**
     * Returns first non-null argument
     *
     * @param args
     * @param <T>
     * @return
     */
    public static <T> T coalesce(T ...args)
    {
        for(T arg : args) {
            if(arg != null) {
                return arg;
            }
        }

        return null;
    }

    public static String ucFirst(String str)
    {
        return str.isEmpty()
            ? ""
            : str.substring(0,1).toUpperCase() + str.substring(1);
    }

    public static String lcFirst(String str)
    {
        return str.isEmpty()
            ? ""
            : str.substring(0,1).toLowerCase() + str.substring(1);
    }

    public static String toLowerCamelCase(String s)
    {
        return toCamelCase(s, false);
    }

    public static String toUpperCamelCase(String s)
    {
        return toCamelCase(s, true);
    }

    public static String toCamelCase(String s, boolean upper)
    {
        String result = "";
        for(String part : s.split("_")) {
            result += ucFirst(part);
        }

        result = upper ? result : lcFirst(result);

        return result;
        /*
        int offset = 0;
        String result = "";
        for(;;) {
            int newOffset = s.indexOf('_', offset);
            if(newOffset == -1) {
                result += ucFirst(s.substring(offset));
                break;
            }

            result += ucFirst(s.substring(offset + 1));
            offset = newOffset;
        }

        return result;
        */
    }

    /**
     * Cuts a string after nMax bytes - unless the remaining bytes are less
     * than tolerance.
     * In case of a cut appends "... (# more bytes)".
     * (# cannot be less than tolerance)
     *
     * @param str
     * @param nMax
     * @param nTolerance
     * @return
     */
    public static String cropString(String str, int nMax, int nTolerance)
    {
        String result = str;
        int nGiven = str.length();

        if(nGiven > nMax) {
            int tooMany = nGiven - nMax;

            if(tooMany > nTolerance)
                result = str.substring(0, nMax) +
                    "... (" + tooMany + " more bytes)";
        }
        return result;
    }


    /**
     * Returns the common prefix of the given strings
     *
     * @return
     */
    public static String commonPrefix(String sa, String sb, boolean skipLast)
    {
        char[] a = sa.toCharArray();
        char[] b = sb.toCharArray();
        int n = Math.min(a.length, b.length);

        char[] tmp = new char[n];


        int i;
        for(i = 0; i < n; i++) {
            if(a[i] != b[i]) {
                tmp[i] = '\0';
                break;
            }

            tmp[i] = a[i];
        }

        if(skipLast) {
           if(i == 0) {
               return null;
           } else {
               tmp[i - 1] = '\0';
           }
        }


        return new String(tmp);
    }



    public static <T> String longestPrefixLookup(String lookup, NavigableSet<String> prefixes)
    {
        return longestPrefixLookup(lookup, true, prefixes);
    }

    public static <T> String longestPrefixLookup(String lookup, boolean inclusive, NavigableSet<String> prefixes)
    {
        String current = lookup;
        while(true) {
            NavigableSet<String> candidates = prefixes.headSet(current, true).descendingSet();
            if(candidates.isEmpty()) {
                return null;
            }

            String candidate = candidates.first();
            if(candidate == null) {
                return null;
            }

            if(current.equals(candidate)) {
                if(inclusive) {
                    return candidate;
                } else {
                    if(current.equals(lookup)) {
                        current = StringUtils.commonPrefix(current, candidate, true);
                        if(current == null) {
                            return null;
                        }
                    } else {
                        return candidate;
                    }
                }
            } else {
                current = StringUtils.commonPrefix(current, candidate, false);
            }
        }
    }


    public static <T> Map.Entry<String, T> longestPrefixLookup(String lookup, NavigableMap<String, T> prefixMap)
    {
        return longestPrefixLookup(lookup, true, prefixMap);
    }


    public static <T> Map<String, T> getAllPrefixes(String lookup, boolean inclusive, NavigableMap<String, T> prefixMap) {
        Map<String, T> result = new HashMap<String, T>();

        Map.Entry<String, T> entry = longestPrefixLookup(lookup, inclusive, prefixMap);
        if(entry != null) {
            result.put(entry.getKey(), entry.getValue());

            String current = entry.getKey();
            while((entry = longestPrefixLookup(entry.getKey(), false, prefixMap)) != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public static <T> NavigableSet<String> getAllPrefixes(String lookup, boolean inclusive, NavigableSet<String> prefixMap) {
        NavigableSet<String> result = new TreeSet<String>();

        String entry = longestPrefixLookup(lookup, inclusive, prefixMap);
        if(entry != null) {
            result.add(entry);

            while((entry = longestPrefixLookup(entry, false, prefixMap)) != null) {
                result.add(entry);
            }
        }

        return result;
    }


    public static <T> Map<String, T> getAllPrefixes(String lookup, boolean inclusive, SortedMap<String, T> prefixMap) {
        Map<String, T> result = new HashMap<String, T>();

        Map.Entry<String, T> entry = longestPrefixLookup(lookup, inclusive, prefixMap);
        if(entry != null) {
            result.put(entry.getKey(), entry.getValue());

            String current = entry.getKey();
            while((entry = longestPrefixLookup(entry.getKey(), false, prefixMap)) != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }



    public static <T> Map<String, T> getAllPrefixedEntries(String prefix, boolean inclusive, SortedMap<String, T> prefixMap) {
        Map<String, T> result = new HashMap<String, T>();

        SortedMap<String, T> candidates = prefixMap.tailMap(prefix);

        boolean isFirst = true;
        for(Map.Entry<String, T> entry : candidates.entrySet()) {

            // Skip the entry if non-exclusive
            if(isFirst && !inclusive) {
                if(entry.getKey().equals(prefix)) {
                    isFirst = false;
                    continue;
                }
            }

            if(entry.getKey() != null && entry.getKey().startsWith(prefix) ) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                break;
            }
        }

        return result;
    }

    public static <T> Map<String, T> getAllPrefixedEntries(String prefix, boolean inclusive, NavigableMap<String, T> prefixMap) {
        Map<String, T> result = new HashMap<String, T>();


        NavigableMap<String, T> candidates = prefixMap.tailMap(prefix, inclusive);
        for(Map.Entry<String, T> entry : candidates.entrySet()) {
            if(entry.getKey() != null && entry.getKey().startsWith(prefix) ) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                break;
            }
        }

        /*
        if(candidates.isEmpty()) {
            return null;
        }

        Map.Entry<String, V> candidate = candidates.lastEntry();
        return candidate.getKey().startsWith(prefix) ? candidate : null;



        Map.Entry<String, T> entry = shortestMatchLookup(prefix, inclusive, prefixMap);
        if(entry != null) {
            result.put(entry.getKey(), entry.getValue());

            String current = entry.getKey();
            while((entry = shortestMatchLookup(entry.getKey(), false, prefixMap)) != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }*/

        return result;
    }


    /**
     * Looks up an element in the given map that is the longest prefix of the given lookup key.
     *
     * @param lookup
     * @param prefixMap
     * @return
     */
    public static <T> Map.Entry<String, T> longestPrefixLookup(String lookup, boolean inclusive, NavigableMap<String, T> prefixMap)
    {
        String current = lookup;
        while(true) {
            NavigableMap<String, T> candidates = prefixMap.headMap(current, true).descendingMap();
            Map.Entry<String, T> candidate = candidates.firstEntry();

            if(candidate == null) {
                return null;
            }

            String key = candidate.getKey();

            if(current.equals(key)) {
                if(inclusive) {
                    return candidate;
                } else {
                    if(current.equals(lookup)) {
                        current = StringUtils.commonPrefix(current, key, true);
                        if(current == null) {
                            return null;
                        }
                    } else {
                        return candidate;
                    }
                }
            } else {
                current = StringUtils.commonPrefix(current, key, false);
            }
        }
    }

    public static <T> Map.Entry<String, T> longestPrefixLookup(String lookup, boolean inclusive, SortedMap<String, T> prefixMap)
    {
        String current = lookup;
        while(true) {
            SortedMap<String, T> candidates = prefixMap.headMap(current);
            if(candidates.isEmpty()) {
                return null;
            }

            Map.Entry<String, T> candidate = candidates.entrySet().iterator().next();
            String key = candidate.getKey();

            if(current.equals(key)) {
                if(inclusive) {
                    return candidate;
                } else {
                    if(current.equals(lookup)) {
                        current = StringUtils.commonPrefix(current, key, true);
                        if(current == null) {
                            return null;
                        }
                    } else {
                        return candidate;
                    }
                }
            } else {
                current = StringUtils.commonPrefix(current, key, false);
            }
        }
    }



    public static <V> Map.Entry<String, V> shortestMatchLookup(String prefix, boolean inclusive, NavigableMap<String, V> items) {
        NavigableMap<String, V> candidates = items.tailMap(prefix, inclusive).descendingMap();
        if(candidates.isEmpty()) {
            return null;
        }

        Map.Entry<String, V> candidate = candidates.lastEntry();
        return candidate.getKey().startsWith(prefix) ? candidate : null;
    }


    public static String shortestMatchLookup(String prefix, boolean inclusive, NavigableSet<String> items) {
        NavigableSet<String> candidates = items.tailSet(prefix, inclusive).descendingSet();
        if(candidates.isEmpty()) {
            return null;
        }

        String candidate = candidates.last();
        return candidate.startsWith(prefix) ? candidate : null;
    }


    public static <T> Map.Entry<String, T> getMatchBySuffix(String str, Map<String, T> map)
    {
        Map.Entry<String, T> bestMatch = null;
        for(Map.Entry<String, T> entry : map.entrySet()) {
            String key = entry.getKey();

            if(str.endsWith(key)) {
                bestMatch = (bestMatch == null)
                    ? entry
                    : (key.length() > bestMatch.getKey().length())
                        ? entry
                        : bestMatch;
            }
        }

        return bestMatch;
    }


    /**
     * Helper functions to get rid of that exception.
     * Afaik UTF8 en/de-coding cannot fail (read it somewhere, not confirmed)
     *
     * @param str
     * @return
     */
    public static String urlEncode(String str)
    {
        try {
            return URLEncoder.encode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String urlDecode(String str)
    {
        try {
            return URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    /*
    public static void main(String[] args) {
        NavigableMap<String, String> m = new TreeMap<String, String>();
        m.put("m", "2");
        m.put("malta", "3");
        m.put("mali", "4");
        m.put("malibu", "5");
        m.put("macedonien", "6");

        System.out.println(longestPrefixLookup("malibuu", m));
        System.out.println(longestPrefixLookup("malibu", m));
        System.out.println(longestPrefixLookup("malib", m));
        System.out.println(longestPrefixLookup("mali", m));
        System.out.println(longestPrefixLookup("mal", m));
        System.out.println(longestPrefixLookup("ma", m));
        System.out.println(longestPrefixLookup("m", m));
        System.out.println(longestPrefixLookup("", m));
    }*/


    /**
     * calculate md5 hash of the string
     *
     * @param bytes
     * @return
     */
    public static String md5Hash(byte[] bytes)
    {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md5.reset();
        md5.update(bytes);
        byte[] rawResult = md5.digest();

        return bytesToHexString(rawResult);
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            int value = 0xff & bytes[i];

            if(value < 16) {
                result += "0";
            }

            result += Integer.toHexString(value);
        }

        return result;
    }

    public static String md5Hash(String string) {
        return md5Hash(string.getBytes());
    }


    /**
     * Return the substring of a string that only consists of digits.
     * <p>
     * Examples:
     * <pre>
     *   "abc123" -&gt; "123"
     *   "abc" -&gt; ""
     *   "abc123.456" -&gt; "456"
     * </pre>
     */
    public static String numberSuffix(String base) {
        int l = base.length();
        int i;
        for (i = l - 1; i >= 0; --i) {
            char c = base.charAt(i);
            if (!Character.isDigit(c)) {
                break;
            }
        }
        String result = base.substring(i + 1, l);
        return result;
    }

    public static String allocateName(String base, boolean forceNumberSuffix, Predicate<String> skip) {
        String result = null;
        if (!forceNumberSuffix) {
            if (!skip.test(base)) {
                result = base;
            }
        }

        if (result == null) {
            String numberStr = numberSuffix(base);
            String prefix = base.substring(0, base.length() - numberStr.length());

            BigInteger current = numberStr.isEmpty()
                    ? BigInteger.valueOf(0)
                    : new BigInteger(numberStr);

            BigInteger one = BigInteger.valueOf(1);

            while (true) {
                current = current.add(one);
                result = prefix + current;
                if (!skip.test(result)) {
                    break;
                }
            }
        }
        return result;
    }
}
