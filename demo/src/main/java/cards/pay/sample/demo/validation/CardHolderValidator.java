package cards.pay.sample.demo.validation;

import android.text.TextUtils;

import cards.pay.sample.demo.R;

import static cards.pay.sample.demo.validation.FieldValidationResult.fail;
import static cards.pay.sample.demo.validation.FieldValidationResult.valid;

public class CardHolderValidator implements FieldValidator<CharSequence> {
    @Override
    public FieldValidationResult validate(CharSequence value) {
        if (TextUtils.isEmpty(value)) {
            return fail(R.string.validation_error_fill_in_card_holder_name);
        }
        if (!value.toString().trim().contains(" ")) {
            return fail(R.string.validation_error_invalid_card_holder_name);
        }
        return valid();
    }
}
