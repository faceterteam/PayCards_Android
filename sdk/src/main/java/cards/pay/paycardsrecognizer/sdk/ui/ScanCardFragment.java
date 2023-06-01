package cards.pay.paycardsrecognizer.sdk.ui;

import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_DATE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NAME;
import static cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NUMBER;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cards.pay.paycardsrecognizer.sdk.Card;
import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.camera.CameraUtils;
import cards.pay.paycardsrecognizer.sdk.camera.ImageAnalyzer;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionResult;
import cards.pay.paycardsrecognizer.sdk.ui.views.ProgressBarIndeterminate;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

public class ScanCardFragment extends Fragment {

    public static final String TAG = "ScanCardFragment";

    private static final float RATIO_4_3_VALUE = 4.0f / 3.0f;
    private static final float RATIO_16_9_VALUE = 16.0f / 9.0f;

    private CameraPreviewLayout mCameraPreviewLayout;
    private ViewGroup mMainContent;
    private ProgressBarIndeterminate mProgressBar;
    private PreviewView mCameraPreview;
    private View mFlashButton;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private ProcessCameraProvider mCameraProvider;
    private Camera mCamera;
    private ImageAnalysis mImageAnalysis;
    private ImageAnalyzer mImageAnalyzer;
    private ExecutorService mCameraExecutor;
    private InteractionListener mListener;
    private ScanCardRequest mRequest;
    private SoundPool mSoundPool;
    private int mCapturedSoundId = -1;
    private byte[] mLastCardImage = null;
    private Boolean flashEnabled = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (ScanCardFragment.InteractionListener) getActivity();
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var root = inflater.inflate(R.layout.wocr_fragment_scan_card, container, false);

        initViews(root);
        mCameraPreviewLayout.setClipToOutline(true);
        showMainContent();
        mProgressBar.setVisibility(View.VISIBLE);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mImageAnalyzer != null) {
            mImageAnalyzer.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mImageAnalyzer != null) mImageAnalyzer.onPause();
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!isTablet()) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mCameraPreviewLayout.setBackgroundColor(Color.BLACK);
        }

        int recognitionMode = RECOGNIZER_MODE_NUMBER;
        if (mRequest.isScanCardHolderEnabled()) recognitionMode |= RECOGNIZER_MODE_NAME;
        if (mRequest.isScanExpirationDateEnabled()) recognitionMode |= RECOGNIZER_MODE_DATE;
        if (mRequest.isGrabCardImageEnabled()) recognitionMode |= RECOGNIZER_MODE_GRAB_CARD_IMAGE;

        mImageAnalyzer = new ImageAnalyzer(recognitionMode, requireActivity(), mCameraPreviewLayout, new ImageAnalyzer.Callbacks() {
            @Override
            public void onRecognitionComplete(RecognitionResult result) {
                if (result.isFirst()) {
                    mCameraProvider.unbindAll();
                    playCaptureSound();
                }
                if (result.isFinal()) {
                    String date;
                    if (TextUtils.isEmpty(result.getDate())) {
                        date = null;
                    } else {
                        date = result.getDate().substring(0, 2) + '/' + result.getDate().substring(2);
                    }

                    Card card = new Card(result.getNumber(), result.getName(), date);
                    var cardImage = mLastCardImage;
                    mLastCardImage = null;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finishWithResult(card, cardImage), 500);
                }
            }

            @Override
            public void onCardImageReceived(Bitmap cardImage) {
                mLastCardImage = compressCardImage(cardImage);
            }
        });

        mCameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        mCameraExecutor = Executors.newSingleThreadExecutor();

        mCameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = mCameraProviderFuture.get();
                bindPreview(mCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                mProgressBar.hideSlow();
                hideMainContent();
                finishWithError(e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        new Handler(requireContext().getMainLooper()).post(() -> {

            if (mImageAnalysis != null) {
                mImageAnalysis.clearAnalyzer();
            }
            if (mCameraProvider != null) {
                mCameraProvider.unbindAll();
            }
        });

        if (mCameraExecutor != null) {
            mCameraExecutor.shutdown();
        }
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

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        if (getContext() instanceof Activity &&
                (((Activity)getContext()).isDestroyed() || ((Activity)getContext()).isFinishing())) {
            //This check is to avoid an exception when trying to re-bind use cases but user closes the activity.
            //java.lang.IllegalArgumentException: Trying to create use case mediator with destroyed lifecycle.
            return;
        }

        cameraProvider.unbindAll();

//        var metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity()).getBounds();
//        var screenAspectRatio = aspectRatio(metrics.width(), metrics.height());

        var metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        var screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);


        @SuppressLint("RestrictedApi") Preview preview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(mCameraPreview.getDisplay().getRotation())
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        mImageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(mCameraPreview.getDisplay().getRotation())
                .setTargetAspectRatio(screenAspectRatio)
                .build();

        var orientationEventListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation >= 45 && orientation <= 124) {
                    mImageAnalysis.setTargetRotation(Surface.ROTATION_270);
                } else if (orientation >= 135 && orientation <= 224) {
                    mImageAnalysis.setTargetRotation(Surface.ROTATION_180);
                } else if (orientation >= 225 && orientation <= 314) {
                    mImageAnalysis.setTargetRotation(Surface.ROTATION_90);
                } else {
                    mImageAnalysis.setTargetRotation(Surface.ROTATION_0);
                }
            }
        };

        orientationEventListener.enable();

        mImageAnalysis.setAnalyzer(mCameraExecutor, mImageAnalyzer);

        preview.setSurfaceProvider(mCameraPreview.getSurfaceProvider());

        mCamera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, mImageAnalysis, preview);

        cameraOpened();
    }

    private void initViews(View root) {
        mCameraPreview = root.findViewById(R.id.cameraPreview);
        mCameraPreviewLayout = root.findViewById(R.id.wocr_card_recognition_view);
        mMainContent = root.findViewById(R.id.wocr_main_content);
        mProgressBar = root.findViewById(R.id.wocr_progress_bar);
        mFlashButton = root.findViewById(R.id.wocr_iv_flash_id);

        if (mFlashButton != null) {
            mFlashButton.setOnClickListener(v -> mCamera.getCameraControl().enableTorch(!flashEnabled));
        }

        root.findViewById(R.id.wocr_tv_enter_card_number_id).setOnClickListener(v -> {
            if (v.isEnabled()) {
                v.setEnabled(false);
                if (mListener != null)
                    mListener.onScanCardCanceled(ScanCardIntent.ADD_MANUALLY_PRESSED);
            }
        });

        var paycardsLink = (TextView) root.findViewById(R.id.wocr_powered_by_paycards_link);
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

    private int aspectRatio(int width, int height) {
        var previewRatio = (float) Math.max(width, height) / (float) Math.min(width, height);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private void cameraOpened() {
        mImageAnalyzer.setSensorRotation(mCamera.getCameraInfo().getSensorRotationDegrees());
        mImageAnalyzer.setupCardDetectionCameraParameters(CameraUtils.CAMERA_RESOLUTION.size.width, CameraUtils.CAMERA_RESOLUTION.size.height, mCamera.getCameraInfo().getSensorRotationDegrees());
        mProgressBar.hideSlow();
        mCameraPreviewLayout.setBackground(null);

        if (mCamera != null) {
            var isFlashSupported = mCamera.getCameraInfo().hasFlashUnit();
            if (mFlashButton != null)
                mFlashButton.setVisibility(isFlashSupported ? View.VISIBLE : View.GONE);

            mCamera.getCameraInfo().getTorchState().observe(getViewLifecycleOwner(), torchState -> flashEnabled = torchState == TorchState.ON);
        }

        innitSoundPool();
    }

    private void innitSoundPool() {
        if (mRequest.isSoundEnabled()) {
            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_SYSTEM).build())
                    .build();
            mCapturedSoundId = mSoundPool.load(getActivity(), R.raw.wocr_capture_card, 0);
        }
    }

    private boolean isTablet() {
        return getResources().getBoolean(R.bool.wocr_is_tablet);
    }

    private void playCaptureSound() {
        if (mCapturedSoundId >= 0) mSoundPool.play(mCapturedSoundId, 1, 1, 0, 0, 1);
    }

    @Nullable
    private byte[] compressCardImage(Bitmap img) {
        byte[] result;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (img.compress(Bitmap.CompressFormat.JPEG, 80, stream)) {
            result = stream.toByteArray();
        } else {
            result = null;
        }
        return result;
    }

    private void finishWithError(Exception exception) {
        if (mListener != null) mListener.onScanCardFailed(exception);
    }

    private void finishWithResult(Card card, @Nullable byte[] cardImage) {
        if (mListener != null) mListener.onScanCardFinished(card, cardImage);
    }

    public interface InteractionListener {
        void onScanCardCanceled(@ScanCardIntent.CancelReason int cancelReason);

        void onScanCardFailed(Exception e);

        void onScanCardFinished(Card card, byte[] cardImage);
    }
}
