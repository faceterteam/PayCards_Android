package cards.pay.sample.demo.validation;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

public class FieldValidationResult {

    @Nullable
    private static FieldValidationResult sValidInstance;

    private final boolean mIsValid;

    @StringRes
    private final int mErrorResId;

    public static FieldValidationResult valid() {
        if (sValidInstance == null) {
            sValidInstance = new FieldValidationResult(true, 0);
        }
        return sValidInstance;
    }

    public static FieldValidationResult fail(@StringRes int errorResId) {
        return new FieldValidationResult(false, errorResId);
    }

    private FieldValidationResult(boolean mIsValid, int errorResId) {
        this.mIsValid = mIsValid;
        this.mErrorResId = errorResId;
    }

    public boolean  isValid() {
        return mIsValid;
    }

    @Nullable
    public CharSequence getMessage(Resources resources) {
        if (isValid()) {
            return null;
        } else {
            return resources.getText(mErrorResId);
        }
    }

}
