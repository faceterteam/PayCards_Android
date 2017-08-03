package cards.pay.paycardsrecognizer.sdk.ndk;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

final class NeuroDataHelper {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "RecognitionCore";

    private final File mDataBasePath;

    private final AssetManager mAssetManager;

    public NeuroDataHelper(Context context) {
        Context appContext = context.getApplicationContext();
        mAssetManager = appContext.getAssets();
        mDataBasePath = new File(context.getCacheDir(), Constants.MODEL_DIR + "/" + String.valueOf(Constants.NEURO_DATA_VERSION));
    }

    public void unpackAssets() throws IOException {
        unpackFileOrDir("");
    }

    private void unpackFileOrDir(String assetsPath) throws IOException {
        String assets[];
        assets = mAssetManager.list(Constants.MODEL_DIR + assetsPath);
        if (assets.length == 0) {
            copyAssetToCacheDir(assetsPath);
        } else {
            File dir = getDstPath(assetsPath);
            if (!dir.exists()) {
                if (DBG) Log.v(TAG, "Create cache dir " + dir.getAbsolutePath());
                dir.mkdirs();
            }
            for (int i = 0; i < assets.length; ++i) {
                unpackFileOrDir(assetsPath + "/" + assets[i]);
            }
        }
    }

    private String copyAssetToCacheDir(final String assetsPath) throws IOException {
        File f = getDstPath(assetsPath);

        InputStream is = null;
        OutputStream os = null;

        try {
            is = mAssetManager.open(Constants.MODEL_DIR + assetsPath);
            int fileSize = is.available();
            if (f.length() != fileSize) {
                if (DBG) Log.d(TAG, "copyAssetToCacheDir() rewrite file " + assetsPath);
                os = new FileOutputStream(f, false);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
            }
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // IGNORE
            }
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException ioe) {
                // IGNORE
            }
        }

        return f.getPath();
    }

    public File getDataBasePath() {
        return mDataBasePath;
    }

    private File getDstPath(String assetsPath) {
        return new File(mDataBasePath, assetsPath);
    }

}
