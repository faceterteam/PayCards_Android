package cards.pay.sample.demo.widget;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

public class CardDateEditText extends TextInputEditText {
    public CardDateEditText(Context context) {
        super(context);
        initialize(context);
    }

    public CardDateEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public CardDateEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        setKeyListener(DigitsKeyListener.getInstance("0123456789/"));
        setFilters(new InputFilter[]{
                new CardDateInputFilter(),
                new InputFilter.LengthFilter(5)

        });
        addTextChangedListener(new CardDateEditText.CardDateTextWatcher());
        setSingleLine();
    }

    private static class CardDateTextWatcher implements TextWatcher {
        boolean isAdded;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            isAdded = before < count;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isAdded) {
                if (s.length() == 2) {
                    s.append('/');
                } else if (s.length() == 3 && s.charAt(s.length() - 1) != '/') {
                    s.insert(2, "/");
                }
            }
        }
    }

    private static class CardDateInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            final StringBuilder filteredStringBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char currentChar = source.charAt(i);
                if (dest.length() >= 5) {
                    continue;
                }
                final boolean isSlashPosition = dstart == 2 || filteredStringBuilder.length() == 2;
                if (isSlashPosition && currentChar != '/') {
                    currentChar = '/';
                }
                final boolean isSlashOnCorrectPosition = currentChar == '/' && isSlashPosition;
                if (Character.isDigit(currentChar) || isSlashOnCorrectPosition) {
                    filteredStringBuilder.append(currentChar);
                }
            }
            return filteredStringBuilder.toString();
        }
    }


}
