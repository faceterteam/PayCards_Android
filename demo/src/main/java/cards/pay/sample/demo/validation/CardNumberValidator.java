package cards.pay.sample.demo.validation;

import android.text.TextUtils;

import cards.pay.sample.demo.R;
import cards.pay.sample.demo.utils.StringUtil;

public class CardNumberValidator implements FieldValidator<CharSequence> {

    public CardNumberValidator() {
    }

    @Override
    public FieldValidationResult validate(CharSequence value) {
        if (TextUtils.isEmpty(value)) {
            return FieldValidationResult.fail(R.string.validation_error_fill_in_card_number);
        }

        if (!isValidCreditCardNumber(value)) {
            return FieldValidationResult.fail(R.string.validation_error_invalid_card_number);
        }
        return FieldValidationResult.valid();
    }

    public static boolean isValidCreditCardNumber(CharSequence number) {
        return number.length() >= 12 && number.length() <= 19
                && StringUtil.isAsciiDigitsOnly(number)
                && luhnTest(number.toString());
    }

    public static boolean luhnTest(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

}
