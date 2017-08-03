package cards.pay.sample.demo.validation;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.util.SparseArray;

public class ValidationResult extends SparseArray<FieldValidationResult> {

    private static final ValidationResult sEmptyInstance = new ValidationResult(0);

    public static ValidationResult empty() {
        return sEmptyInstance;
    }

    public ValidationResult() {
    }


    public ValidationResult(int initialCapacity) {
        super(initialCapacity);
    }

    public boolean isValid() {
        for (int i = size() - 1; i >= 0; i--) {
            if (!valueAt(i).isValid()) return false;
        }
        return true;
    }

    @Nullable
    public CharSequence getMessage(int fieldId, Resources resources) {
        FieldValidationResult result = get(fieldId);
        return result != null ? result.getMessage(resources) : null;
    }

}
