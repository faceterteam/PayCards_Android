package cards.pay.paycardsrecognizer.sdk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import cards.pay.paycardsrecognizer.sdk.Card;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionAvailabilityChecker;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionCoreUtils;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionUnavailableException;

public class ScanCardActivity extends AppCompatActivity implements ScanCardFragment.InteractionListener,
        InitLibraryFragment.InteractionListener {

    private static final String TAG = "ScanCardActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        getDelegate().onPostCreate(null);

        if (savedInstanceState == null) {
            RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(this);
            if (checkResult.isFailed()
                    && !checkResult.isFailedOnCameraPermission()) {
                onScanCardFailed(new RecognitionUnavailableException(checkResult.getMessage()));
            } else {
                if (RecognitionCoreUtils.isRecognitionCoreDeployRequired(this)
                        || checkResult.isFailedOnCameraPermission()) {
                    showInitLibrary();
                } else {
                    showScanCard();
                }
            }
        }
    }

    private void showInitLibrary() {
        Fragment fragment = new InitLibraryFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, InitLibraryFragment.TAG)
                .setCustomAnimations(0, 0)
                .commitNow();
    }

    private void showScanCard() {
        Fragment fragment = new ScanCardFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(ScanCardIntent.KEY_SCAN_CARD_REQUEST, getScanRequest());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment, ScanCardFragment.TAG)
                .setCustomAnimations(0, 0)
                .commitNow();

        ViewCompat.requestApplyInsets(findViewById(android.R.id.content));
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onScanCardFailed(Exception e) {
        Log.e(TAG, "Scan card failed", new RuntimeException("onScanCardFinishedWithError()", e));
        setResult(ScanCardIntent.RESULT_CODE_ERROR);
        finish();
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onScanCardFinished(Card card, @Nullable byte cardImage[]) {
        Intent intent = new Intent();
        intent.putExtra(ScanCardIntent.RESULT_PAYCARDS_CARD, (Parcelable) card);
        if (cardImage != null) intent.putExtra(ScanCardIntent.RESULT_CARD_IMAGE, cardImage);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onInitLibraryFailed(Throwable e) {
        Log.e(TAG, "Init library failed", new RuntimeException("onInitLibraryFailed()", e));
        setResult(ScanCardIntent.RESULT_CODE_ERROR);
        finish();
    }

    @Override
    public void onScanCardCanceled(@ScanCardIntent.CancelReason int actionId) {
        Intent intent = new Intent();
        intent.putExtra(ScanCardIntent.RESULT_CANCEL_REASON, actionId);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void onInitLibraryComplete() {
        if (isFinishing()) return;
        showScanCard();
    }

    private ScanCardRequest getScanRequest() {
        ScanCardRequest request = getIntent().getParcelableExtra(ScanCardIntent.KEY_SCAN_CARD_REQUEST);
        if (request == null) {
            request = ScanCardRequest.getDefault();
        }
        return request;
    }
}
