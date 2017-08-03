package cards.pay.sample.demo.utils;

import android.text.TextUtils;

public final class StringUtil {

    private StringUtil() {
    }

    /**
     * Returns whether the given CharSequence contains only ASCII digits.
     */
    public static boolean isAsciiDigitsOnly(CharSequence str) {
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            if (!isAsciiDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAsciiDigit(char codePoint) {
        return '0' <= codePoint && codePoint <= '9';
    }

    public static boolean isBlank(CharSequence text) {
        return text == null || TextUtils.isEmpty(text.toString().trim());
    }

}
