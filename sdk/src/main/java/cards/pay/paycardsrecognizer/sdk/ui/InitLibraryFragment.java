package cards.pay.paycardsrecognizer.sdk.ui;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionAvailabilityChecker;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionCoreUtils;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionUnavailableException;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;
import cards.pay.paycardsrecognizer.sdk.utils.AsyncTaskV2;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class InitLibraryFragment extends Fragment {

    public static final String TAG = "InitLibraryFragment";

    private InteractionListener mListener;

    private View mProgressBar;
    private CameraPreviewLayout mCameraPreviewLayout;
    private ViewGroup mMainContent;
    private @Nullable View mFlashButton;

    private DeployCoreTask mDeployCoreTask;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        // PERMISSION GRANTED
                        subscribeToInitCore();
                    } else {
                        // PERMISSION NOT GRANTED
                        if (mListener != null) mListener.onInitLibraryFailed(
                                new RecognitionUnavailableException(RecognitionUnavailableException.ERROR_NO_CAMERA_PERMISSION));
                    }
                }
            }
    );

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (InteractionListener) getActivity();
        } catch (ClassCastException ex) {
            throw new RuntimeException("Parent must implement " + ScanCardFragment.InteractionListener.class.getSimpleName());
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.wocr_fragment_scan_card, container, false);

        mMainContent = root.findViewById(R.id.wocr_main_content);
        mProgressBar = root.findViewById(R.id.wocr_progress_bar);
        mCameraPreviewLayout = root.findViewById(R.id.wocr_card_recognition_view);
        mFlashButton = root.findViewById(R.id.wocr_iv_flash_id);

        View enterManuallyButton = root.findViewById(R.id.wocr_tv_enter_card_number_id);
        enterManuallyButton.setVisibility(View.VISIBLE);
        enterManuallyButton.setOnClickListener(clickView -> {
            if (mListener != null)
                mListener.onScanCardCanceled(ScanCardIntent.ADD_MANUALLY_PRESSED);
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar.setVisibility(View.GONE);
        mMainContent.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.getPreviewView().setVisibility(View.GONE);
        mCameraPreviewLayout.setBackgroundColor(Color.BLACK);
        if (mFlashButton != null) mFlashButton.setVisibility(View.GONE);

        RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(getContext());
        if (checkResult.isFailedOnCameraPermission()) {
            if (savedInstanceState == null) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        } else {
            subscribeToInitCore();
        }
    }

    private void subscribeToInitCore() {
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        if (mDeployCoreTask != null) mDeployCoreTask.cancel(false);
        mDeployCoreTask = new DeployCoreTask(this);
        mDeployCoreTask.execute((Void) null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDeployCoreTask != null) {
            mDeployCoreTask.cancel(false);
            mDeployCoreTask = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mProgressBar = null;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface InteractionListener {
        void onScanCardCanceled(@ScanCardIntent.CancelReason int actionId);

        void onInitLibraryFailed(Throwable e);

        void onInitLibraryComplete();
    }

    private static class DeployCoreTask extends AsyncTaskV2<Void, Void, Throwable> {

        private final WeakReference<InitLibraryFragment> fragmentRef;

        @Nullable
        private Context appContext;

        DeployCoreTask(InitLibraryFragment parent) {
            this.fragmentRef = new WeakReference<>(parent);
            if (parent.getContext() != null) {
                this.appContext = parent.getContext().getApplicationContext();
            }
        }

        @Override
        protected Throwable doInBackground(Void... voids) {
            try {
                RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(appContext);
                if (checkResult.isFailed()) {
                    throw new RecognitionUnavailableException();
                }
                RecognitionCoreUtils.deployRecognitionCoreSync(appContext);
                if (!RecognitionCore.getInstance(appContext).isDeviceSupported()) {
                    throw new RecognitionUnavailableException();
                }
                return null;
            } catch (RecognitionUnavailableException e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Throwable lastError) {
            super.onPostExecute(lastError);
            InitLibraryFragment fragment = fragmentRef.get();
            if (fragment == null
                    || fragment.mProgressBar == null
                    || fragment.mListener == null) return;

            fragment.mProgressBar.setVisibility(View.GONE);
            if (lastError == null) {
                fragment.mListener.onInitLibraryComplete();
            } else {
                fragment.mListener.onInitLibraryFailed(lastError);
            }
        }
    }
}
