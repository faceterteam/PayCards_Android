package cards.pay.paycardsrecognizer.sdk.utils;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CardUtils {

    private CardUtils() {}

    public static String prettyPrintCardNumber(@Nullable CharSequence cardNumber) {
        if (TextUtils.isEmpty(cardNumber)) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder(20);
        for (int i = 0, size = cardNumber.length(); i < size; ++i) {
            if (size == 16) {
                if (i != 0 && i % 4 == 0) {
                    stringBuilder.append('\u00a0');
                }
            } else if (size == 15) {
                if (i == 4 || i == 10) {
                    stringBuilder.append('\u00a0');
                }
            }
            stringBuilder.append(cardNumber.charAt(i));
        }
        return stringBuilder.toString();
    }

    public static String getCardNumberRedacted(@Nullable String cardNumber) {
        if (null == cardNumber) {
            return "";
        }
        if (cardNumber.length() == 16) {
            final String beginNumber = cardNumber.substring(0, 6);
            final String endNumber = cardNumber.substring(cardNumber.length() - 2, cardNumber.length());
            final StringBuilder stringBuilder = new StringBuilder(beginNumber + "********" + endNumber);
            stringBuilder.insert(4, " ");
            stringBuilder.insert(9, " ");
            stringBuilder.insert(14, " ");
            return stringBuilder.toString();
        } else if (cardNumber.length() == 15) {
            final String beginNumber = cardNumber.substring(0, 6);
            final String endNumber = cardNumber.substring(cardNumber.length() - 1, cardNumber.length());
            final StringBuilder stringBuilder = new StringBuilder(beginNumber + "********" + endNumber);
            stringBuilder.insert(4, " ");
            stringBuilder.insert(11, " ");
            return stringBuilder.toString();
        }
        return "";
    }

}
