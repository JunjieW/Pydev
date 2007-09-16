/*
 * Created on 03/09/2005
 */
package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    public static String format(String str, Object... args) {
        StringBuffer buffer = new StringBuffer();
        int j = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '%' && i + 1 < str.length()) {
                char nextC = str.charAt(i + 1);
                if (nextC == 's') {
                    buffer.append(args[j]);
                    j++;
                    i++;
                } else if (nextC == '%') {
                    buffer.append('%');
                    j++;
                    i++;
                }
            } else {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    public static int countPercS(String str) {
        int j = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '%' && i + 1 < str.length()) {
                char nextC = str.charAt(i + 1);
                if (nextC == 's') {
                    j++;
                    i++;
                }
            }
        }
        return j;
    }

    public static String rightTrim(String input) {
        int len = input.length();
        int st = 0;
        int off = 0;
        char[] val = input.toCharArray();

        while ((st < len) && (val[off + len - 1] <= ' ')) {
            len--;
        }
        return input.substring(0, len);
    }

    public static String leftTrim(String input) {
        int len = input.length();
        int off = 0;
        char[] val = input.toCharArray();

        while ((off < len) && (val[off] <= ' ')) {
            off++;
        }
        return input.substring(off, len);
    }
    
    /**
     * Given a string remove all from the rightmost '.' onwards.
     * 
     * E.g.: bbb.t would return bbb
     * 
     * If it has no '.', returns the original string unchanged.
     */
    public static String stripExtension(String input) {
        return stripFromRigthCharOnwards(input, '.');
    }

    private static String stripFromRigthCharOnwards(String input, char ch) {
        int len = input.length();
        int st = 0;
        int off = 0;
        char[] val = input.toCharArray();
        
        while ((st < len) && (val[off + len - 1] != ch)) {
            len--;
        }
        len--;
        if(len == -1){
            return input;
        }
        return input.substring(0, len);
    }

    public static String stripFromLastSlash(String input) {
        return stripFromRigthCharOnwards(input, '/');
    }

    public static String rightTrim(String input, char charToTrim) {
        int len = input.length();
        int st = 0;
        int off = 0;
        char[] val = input.toCharArray();
        
        while ((st < len) && (val[off + len - 1] == charToTrim)) {
            len--;
        }
        return input.substring(0, len);
    }
    
    public static String leftTrim(String input, char charToTrim) {
        int len = input.length();
        int off = 0;
        char[] val = input.toCharArray();
        
        while ((off < len) && (val[off] == charToTrim)) {
            off++;
        }
        return input.substring(off, len);
    }
    
    /**
     * Changes all backward slashes (\) for forward slashes (/)
     * 
     * @return the replaced string
     */
    public static String replaceAllSlashes(String string) {
        int len = string.length();
        char c = 0;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            if (c == '\\') { // only do some processing if there is a
                                // backward slash
                char[] ds = string.toCharArray();
                ds[i] = '/';
                for (int j = i; j < len; j++) {
                    if (ds[j] == '\\') {
                        ds[j] = '/';
                    }
                }
                return new String(ds);
            }

        }
        return string;
    }

    public static List<String> splitInLines(String string) {
        ArrayList<String> ret = new ArrayList<String>();
        int len = string.length();

        char c;
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);

            buf.append(c);

            if (c == '\r') {
                if (i < len - 1 && string.charAt(i + 1) == '\n') {
                    i++;
                    buf.append('\n');
                }
                ret.add(buf.toString());
                buf = new StringBuffer();
            }
            if (c == '\n') {
                ret.add(buf.toString());
                buf = new StringBuffer();

            }
        }
        if (buf.length() != 0) {
            ret.add(buf.toString());
        }
        return ret;

    }

    public static boolean isSingleWord(String string) {
        for (char c : string.toCharArray()) {
            if (!Character.isJavaIdentifierStart(c)) {
                return false;
            }
        }
        return true;
    }

    public static String replaceAll(String string, String replace, String with) {
        StringBuffer ret = new StringBuffer();
        int len = string.length();
        int replaceLen = replace.length();
        
        for (int i = 0; i < len; i++) {
            if(i+replaceLen > len){
                ret.append(string.charAt(i));
                continue;
            }
            String s = string.substring(i, i+replaceLen);
            if(s.equals(replace)){
                ret.append(with);
                i = i+replaceLen-1;
            }else{
                ret.append(s.charAt(0));
            }
        }

        return ret.toString();
    }

    public static String removeWhitespaceColumnsToLeft(String hoverInfo) {
        StringBuffer buf = new StringBuffer();
        int firstCharPosition = Integer.MAX_VALUE;
        
        List<String> splitted = splitInLines(hoverInfo);
        for(String line:splitted){
            if(line.trim().length() > 0){
                int found = PySelection.getFirstCharPosition(line);
                firstCharPosition = Math.min(found, firstCharPosition);
            }
        }
        
        if(firstCharPosition != Integer.MAX_VALUE){
            for(String line:splitted){
                if(line.length() > firstCharPosition){
                    buf.append(line.substring(firstCharPosition));
                }
            }
            return buf.toString();
        }else{
            return hoverInfo;//return initial
        }
    }

    
}
