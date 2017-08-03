package cards.pay.paycardsrecognizer.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cards.pay.paycardsrecognizer.sdk.ui.ScanCardActivity;
import cards.pay.paycardsrecognizer.sdk.ui.ScanCardRequest;

import static cards.pay.paycardsrecognizer.sdk.ui.ScanCardRequest.DEFAULT_ENABLE_SOUND;
import static cards.pay.paycardsrecognizer.sdk.ui.ScanCardRequest.DEFAULT_GRAB_CARD_IMAGE;
import static cards.pay.paycardsrecognizer.sdk.ui.ScanCardRequest.DEFAULT_SCAN_CARD_HOLDER;
import static cards.pay.paycardsrecognizer.sdk.ui.ScanCardRequest.DEFAULT_SCAN_EXPIRATION_DATE;

public final class ScanCardIntent {

    public static final int RESULT_CODE_ERROR = Activity.RESULT_FIRST_USER;

    public static final String RESULT_PAYCARDS_CARD = "RESULT_PAYCARDS_CARD";
    public static final String RESULT_CARD_IMAGE = "RESULT_CARD_IMAGE";
    public static final String RESULT_CANCEL_REASON = "RESULT_CANCEL_REASON";

    public static final int BACK_PRESSED = 1;
    public static final int ADD_MANUALLY_PRESSED = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {BACK_PRESSED, ADD_MANUALLY_PRESSED})
    public @interface CancelReason {}

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String KEY_SCAN_CARD_REQUEST = "cards.pay.paycardsrecognizer.sdk.ui.ScanCardActivity.SCAN_CARD_REQUEST";

    private ScanCardIntent() {
    }

    public final static class Builder {

        private final Context mContext;

        private boolean mEnableSound = DEFAULT_ENABLE_SOUND;

        private boolean mScanExpirationDate = DEFAULT_SCAN_EXPIRATION_DATE;

        private boolean mScanCardHolder = DEFAULT_SCAN_CARD_HOLDER;

        private boolean mGrabCardImage = DEFAULT_GRAB_CARD_IMAGE;


        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Scan expiration date. Default: <b>true</b>
         */
        public Builder setScanExpirationDate(boolean scanExpirationDate) {
            mScanExpirationDate = scanExpirationDate;
            return this;
        }

        /**
         * Scan expiration date. Default: <b>true</b>
         */
        public Builder setScanCardHolder(boolean scanCardHolder) {
            mScanCardHolder = scanCardHolder;
            return this;
        }


        /**
         * Enables or disables sounds in the library.<Br>
         * Default: <b>enabled</b>
         */
        public Builder setSoundEnabled(boolean enableSound) {
            mEnableSound = enableSound;
            return this;
        }


        /**
         * Defines if the card image will be captured.
         * @param enable Defines if the card image will be captured. Default: <b>false</b>
         */
        public Builder setSaveCard(boolean enable) {
            mGrabCardImage = enable;
            return this;
        }

        public Intent build() {
            Intent intent = new Intent(mContext, ScanCardActivity.class);
            ScanCardRequest request = new ScanCardRequest(mEnableSound, mScanExpirationDate,
                    mScanCardHolder, mGrabCardImage);
            intent.putExtra(KEY_SCAN_CARD_REQUEST, request);
            return intent;
        }
    }
}
