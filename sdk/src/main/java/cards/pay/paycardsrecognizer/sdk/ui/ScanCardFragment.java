package cards.pay.paycardsrecognizer.sdk.ui;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

import cards.pay.paycardsrecognizer.sdk.Card;
import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.camera.ScanManager;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionResult;
import cards.pay.paycardsrecognizer.sdk.ui.views.ProgressBarIndeterminate;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_DATE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NAME;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NUMBER;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ScanCardFragment extends Fragment {
    @SuppressWarnings("unused")
    public static final String TAG = "ScanCardFragment";

    private CameraPreviewLayout mCameraPreviewLayout;

    private ProgressBarIndeterminate mProgressBar;

    private ViewGroup mMainContent;

    @Nullable
    private View mFlashButton;

    @Nullable
    private ScanManager mScanManager;

    private SoundPool mSoundPool;

    private int mCapturedSoundId = -1;

    private InteractionListener mListener;

    private ScanCardRequest mRequest;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (InteractionListener) getActivity();
        } catch (ClassCastException ex) {
            throw new RuntimeException("Parent must implement " + InteractionListener.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRequest = null;
        if (getArguments() != null) {
            mRequest = getArguments().getParcelable(ScanCardIntent.KEY_SCAN_CARD_REQUEST);
        }
        if (mRequest == null) mRequest = ScanCardRequest.getDefault();
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (Constants.DEBUG) Log.d(TAG, "onCreateAnimation() called with: " +  "transit = [" + transit + "], enter = [" + enter + "], nextAnim = [" + nextAnim + "]");
        // SurfaceView is hard to animate
        Animation a = new Animation() {};
        a.setDuration(0);
        return a;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.wocr_fragment_scan_card, container, false);

        mProgressBar = root.findViewById(R.id.wocr_progress_bar);

        mCameraPreviewLayout = root.findViewById(R.id.wocr_card_recognition_view);
        mMainContent = root.findViewById(R.id.wocr_main_content);
        mFlashButton = root.findViewById(R.id.wocr_iv_flash_id);

        initView(root);

        showMainContent();
        mProgressBar.setVisibility(View.VISIBLE);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!isTablet()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mCameraPreviewLayout.setBackgroundColor(Color.BLACK);
        }

        int recognitionMode = RECOGNIZER_MODE_NUMBER;
        if (mRequest.isScanCardHolderEnabled()) recognitionMode |=  RECOGNIZER_MODE_NAME;
        if (mRequest.isScanExpirationDateEnabled()) recognitionMode |= RECOGNIZER_MODE_DATE;
        if (mRequest.isGrabCardImageEnabled()) recognitionMode |= RECOGNIZER_MODE_GRAB_CARD_IMAGE;

        mScanManager = new ScanManager(recognitionMode, getActivity(), mCameraPreviewLayout, new ScanManager.Callbacks() {

            private byte mLastCardImage[] = null;

            @Override
            public void onCameraOpened(Camera.Parameters cameraParameters) {
                boolean isFlashSupported = (cameraParameters.getSupportedFlashModes() != null
                        && !cameraParameters.getSupportedFlashModes().isEmpty());
                if (getView() == null) return;
                mProgressBar.hideSlow();
                mCameraPreviewLayout.setBackgroundDrawable(null);
                if (mFlashButton != null) mFlashButton.setVisibility(isFlashSupported ? View.VISIBLE : View.GONE);

                innitSoundPool();
            }

            @Override
            public void onOpenCameraError(Exception exception) {
                mProgressBar.hideSlow();
                hideMainContent();
                finishWithError(exception);
            }

            @Override
            public void onRecognitionComplete(RecognitionResult result) {
                if (result.isFirst()) {
                    if (mScanManager != null) mScanManager.freezeCameraPreview();
                    playCaptureSound();
                }
                if (result.isFinal()) {
                    String date;
                    if (TextUtils.isEmpty(result.getDate())) {
                        date = null;
                    } else {
                        date =  result.getDate().substring(0, 2) + '/' + result.getDate().substring(2);
                    }

                    Card card = new Card(result.getNumber(), result.getName(), date);
                    byte cardImage[] = mLastCardImage;
                    mLastCardImage = null;
                    finishWithResult(card, cardImage);
                }
            }

            @Override
            public void onCardImageReceived(Bitmap cardImage) {
                mLastCardImage = compressCardImage(cardImage);
            }

            @Override
            public void onFpsReport(String report) {}

            @Override
            public void onAutoFocusMoving(boolean start, String cameraFocusMode) {}

            @Override
            public void onAutoFocusComplete(boolean success, String cameraFocusMode) {}

            @Nullable
            private byte[] compressCardImage(Bitmap img) {
                byte result[];
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (img.compress(Bitmap.CompressFormat.JPEG, 80, stream)) {
                    result = stream.toByteArray();
                } else {
                    result = null;
                }
                return result;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mScanManager != null) mScanManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScanManager != null) mScanManager.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        mCapturedSoundId = -1;
    }

    private void innitSoundPool() {
        if (mRequest.isSoundEnabled()) {
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
            mCapturedSoundId = mSoundPool.load(getActivity(), R.raw.wocr_capture_card, 0);
        }
    }

    private void initView(View view) {
        view.findViewById(R.id.wocr_tv_enter_card_number_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (v.isEnabled()) {
                    v.setEnabled(false);
                    if (mListener != null) mListener.onScanCardCanceled(ScanCardIntent.ADD_MANUALLY_PRESSED);
                }
            }
        });
        if(mFlashButton != null) {
            mFlashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mScanManager != null) mScanManager.toggleFlash();
                }
            });
        }

        TextView paycardsLink = (TextView)view.findViewById(R.id.wocr_powered_by_paycards_link);
        SpannableString link = new SpannableString(getText(R.string.wocr_powered_by_pay_cards));
        link.setSpan(new URLSpan(Constants.PAYCARDS_URL), 0, link.length(), SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        paycardsLink.setText(link);
        paycardsLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMainContent() {
        mMainContent.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.setVisibility(View.VISIBLE);
    }

    private void hideMainContent() {
        mMainContent.setVisibility(View.INVISIBLE);
        mCameraPreviewLayout.setVisibility(View.INVISIBLE);
    }

    private void finishWithError(Exception exception) {
        if (mListener != null) mListener.onScanCardFailed(exception);
    }

    private void finishWithResult(Card card, @Nullable byte cardImage[]) {
        if (mListener != null) mListener.onScanCardFinished(card, cardImage);
    }

    private boolean isTablet() {
        return getResources().getBoolean(R.bool.wocr_is_tablet);
    }

    private void playCaptureSound() {
        if (mCapturedSoundId >= 0) mSoundPool.play(mCapturedSoundId, 1, 1, 0, 0, 1);
    }

    public interface InteractionListener {
        void onScanCardCanceled(@ScanCardIntent.CancelReason int cancelReason);
        void onScanCardFailed(Exception e);
        void onScanCardFinished(Card card, byte cardImage[]);
    }
}
