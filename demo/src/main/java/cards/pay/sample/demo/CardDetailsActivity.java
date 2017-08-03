package cards.pay.sample.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.lang.reflect.Method;

import cards.pay.paycardsrecognizer.sdk.Card;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent.CancelReason;
import cards.pay.sample.demo.validation.CardExpiryDateValidator;
import cards.pay.sample.demo.validation.CardHolderValidator;
import cards.pay.sample.demo.validation.CardNumberValidator;
import cards.pay.sample.demo.validation.ValidationResult;
import cards.pay.sample.demo.widget.CardNumberEditText;

public class CardDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CardDetailsActivity";

    private static final int REQUEST_CODE_SCAN_CARD = 1;

    private Toolbar mToolbar;

    private TextInputLayout mCardNumberField;

    private TextInputLayout mCardholderField;

    private TextInputLayout mExpiryField;

    private CardNumberValidator mCardNumberValidator;
    private CardHolderValidator mCardHolderValidator;
    private CardExpiryDateValidator mExpiryDateValidator;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_card_details);

        mToolbar = findViewById(R.id.toolbar);
        mCardNumberField = findViewById(R.id.card_number_field);
        mCardholderField = findViewById(R.id.cardholder_field);
        mExpiryField = findViewById(R.id.expiry_date_field);
        setupToolbar();

        findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCard();
            }
        });

        if (savedInstanceState == null) {
            scanCard();
        }
    }


    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Card card = readForm();
                ValidationResult validationResult = validateForm(card);
                setValidationResult(validationResult);
                if (validationResult.isValid()) {
                    goToFinalScreen(view);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_CARD) {
            if (resultCode == Activity.RESULT_OK) {
                Card card = data.getParcelableExtra(ScanCardIntent.RESULT_PAYCARDS_CARD);
                if (BuildConfig.DEBUG) Log.i(TAG, "Card info: " + card);
                setCard(card);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                @CancelReason final int reason;
                if (data != null) {
                    reason = data.getIntExtra(ScanCardIntent.RESULT_CANCEL_REASON, ScanCardIntent.BACK_PRESSED);
                } else {
                    reason = ScanCardIntent.BACK_PRESSED;
                }

                if (reason == ScanCardIntent.ADD_MANUALLY_PRESSED) {
                    showIme(mCardNumberField.getEditText());
                }
            } else if (resultCode == ScanCardIntent.RESULT_CODE_ERROR) {
                Log.i(TAG, "Scan failed");
            }
        }
    }

    private Card readForm() {
        String cardNumber = ((CardNumberEditText)mCardNumberField.getEditText()).getCardNumber();
        String holder = mCardholderField.getEditText().getText().toString();
        String expiryDate = mExpiryField.getEditText().getText().toString();
        return new Card(cardNumber, holder, expiryDate);
    }

    private ValidationResult validateForm(Card card) {
        if (mCardNumberValidator == null) {
            mCardNumberValidator = new CardNumberValidator();
            mExpiryDateValidator = new CardExpiryDateValidator();
            mCardHolderValidator = new CardHolderValidator();
        }


        ValidationResult results = new ValidationResult(3);
        results.put(R.id.card_number_field, mCardNumberValidator.validate(card.getCardNumber()));
        results.put(R.id.cardholder_field, mCardHolderValidator.validate(card.getCardHolderName()));
        results.put(R.id.expiry_date_field, mExpiryDateValidator.validate(card.getExpirationDate()));
        return results;
    }

    private void setValidationResult(ValidationResult result) {
        mCardNumberField.setError(result.getMessage(R.id.card_number_field, getResources()));
        mCardholderField.setError(result.getMessage(R.id.cardholder_field, getResources()));
        mExpiryField.setError(result.getMessage(R.id.expiry_date_field, getResources()));
    }

    private void goToFinalScreen(View root) {
        Intent intent = new Intent(this, FinalActivity.class);
        startActivity(intent);
    }

    private void setCard(@NonNull Card card) {
        mCardNumberField.getEditText().setText(card.getCardNumber());
        mCardholderField.getEditText().setText(card.getCardHolderName());
        mExpiryField.getEditText().setText(card.getExpirationDate());
        setValidationResult(ValidationResult.empty());
    }

    private void scanCard() {
        Intent intent = new ScanCardIntent.Builder(this).build();
        startActivityForResult(intent, REQUEST_CODE_SCAN_CARD);
    }

    private static void showIme(@Nullable View view) {
        if (view == null) return;
        if (view instanceof EditText) view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService
                (Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        try {
            Method showSoftInputUnchecked = InputMethodManager.class.getMethod(
                    "showSoftInputUnchecked", int.class, ResultReceiver.class);
            showSoftInputUnchecked.setAccessible(true);
            showSoftInputUnchecked.invoke(imm, 0, null);
        } catch (Exception e) {
            // ho hum
            imm.showSoftInput(view, 0);
        }
    }

}
