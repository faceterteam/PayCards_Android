package cards.pay.sample.demo.widget;

import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;

public class CardNumberEditText extends TextInputEditText {

    public CardNumberEditText(Context context) {
        super(context);
        initialize(context);
    }

    public CardNumberEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public CardNumberEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        setKeyListener(DigitsKeyListener.getInstance("0123456789 "));
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(23)});
        setSingleLine();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter){
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        String cardNumber = text.toString();
        cardNumber = cardNumber.replaceAll(" ", "");
        cardNumber = formatCardNumber(cardNumber);
        if(!cardNumber.equals(text.toString())) {
            setText(cardNumber);
            setSelection(length());
        }
    }

    private String formatCardNumber(String cardNumber) {
        StringBuilder formatted = new StringBuilder(cardNumber);

        for (int i = 1; i <= Math.floor(cardNumber.length() / 4f); i++) {
            formatted.insert(i * 5 - 1, " ");
        }
        return formatted.toString().trim();
    }

    public String getCardNumber() {
        return getText().toString().replaceAll(" ", "");
    }

}
