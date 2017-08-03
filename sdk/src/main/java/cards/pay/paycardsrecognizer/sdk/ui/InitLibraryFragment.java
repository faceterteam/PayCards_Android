package cards.pay.paycardsrecognizer.sdk.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.ScanCardIntent;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionAvailabilityChecker;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionCoreUtils;
import cards.pay.paycardsrecognizer.sdk.camera.RecognitionUnavailableException;
import cards.pay.paycardsrecognizer.sdk.camera.widget.CameraPreviewLayout;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class InitLibraryFragment extends Fragment {

    public static final String TAG = "InitLibraryFragment";

    private InteractionListener mListener;

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;

    private View mProgressBar;
    private CameraPreviewLayout mCameraPreviewLayout;
    private ViewGroup mMainContent;
    private @Nullable View mFlashButton;

    private DeployCoreTask mDeployCoreTask;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (InteractionListener) getActivity();
        } catch (ClassCastException ex) {
            throw new RuntimeException("Parent must implement " + ScanCardFragment.InteractionListener.class.getSimpleName());
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.wocr_fragment_scan_card, container, false);

        mMainContent = root.findViewById(R.id.wocr_main_content);
        mProgressBar = root.findViewById(R.id.wocr_progress_bar);
        mCameraPreviewLayout = root.findViewById(R.id.wocr_card_recognition_view);
        mFlashButton = root.findViewById(R.id.wocr_iv_flash_id);

        View enterManuallyButton = root.findViewById(R.id.wocr_tv_enter_card_number_id);
        enterManuallyButton.setVisibility(View.VISIBLE);
        enterManuallyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickview) {
                if (mListener != null) mListener.onScanCardCanceled(ScanCardIntent.ADD_MANUALLY_PRESSED);
            }
        });
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar.setVisibility(View.GONE);
        mMainContent.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.getSurfaceView().setVisibility(View.GONE);
        mCameraPreviewLayout.setBackgroundColor(Color.BLACK);
        if (mFlashButton != null) mFlashButton.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(getContext());
        if (checkResult.isFailedOnCameraPermission()) {
            if (savedInstanceState == null) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
            }
        } else {
            subscribeToInitCore(getActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    subscribeToInitCore(getActivity());
                } else {
                    if (mListener != null ) mListener.onInitLibraryFailed(
                            new RecognitionUnavailableException(RecognitionUnavailableException.ERROR_NO_CAMERA_PERMISSION));
                }
                return;
            default:
                break;
        }
    }

    private void subscribeToInitCore(Context context) {
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
        if (mDeployCoreTask != null) mDeployCoreTask.cancel(false);
        mDeployCoreTask = new DeployCoreTask(this);
        mDeployCoreTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

    private static class DeployCoreTask extends AsyncTask<Void, Void, Throwable> {

        private final WeakReference<InitLibraryFragment> fragmentRef;

        @SuppressLint("StaticFieldLeak")
        private final Context appContext;

        DeployCoreTask(InitLibraryFragment parent) {
            this.fragmentRef = new WeakReference<InitLibraryFragment>(parent);
            this.appContext = parent.getContext().getApplicationContext();
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
