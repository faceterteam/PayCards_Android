package cards.pay.sample.demo.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cards.pay.sample.demo.R;
import cards.pay.sample.demo.utils.StringUtil;

import static cards.pay.sample.demo.validation.FieldValidationResult.fail;
import static cards.pay.sample.demo.validation.FieldValidationResult.valid;

public class CardExpiryDateValidator implements FieldValidator<CharSequence> {

    private static Pattern sExpiryDatePattern = Pattern.compile("([0-9]{2})/([0-9]{2})");

    @Override
    public FieldValidationResult validate(CharSequence value) {
        if (StringUtil.isBlank(value)) {
            return fail(R.string.validation_error_fill_in_expiry_date);
        }

        Matcher matcher = sExpiryDatePattern.matcher(value);
        if (!matcher.matches()) {
            return fail(R.string.validation_error_invalid_expiry_date);
        }

        if (!isValidMm(matcher.group(1))
                || !isValidYy(matcher.group(2))) {
            return fail(R.string.validation_error_invalid_expiry_date);
        }

        return valid();
    }

    private static boolean isValidMm(String mm) {
        int month = Integer.valueOf(mm);
        return (month <= 12 && month > 0);
    }

    private static boolean isValidYy(String yy) {
        int year = Integer.valueOf(yy);
        return (year >= 0 && year <= 99);
    }

}
