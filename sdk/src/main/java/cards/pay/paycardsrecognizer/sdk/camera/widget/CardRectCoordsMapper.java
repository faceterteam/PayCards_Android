package cards.pay.paycardsrecognizer.sdk.camera.widget;

import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;
import cards.pay.paycardsrecognizer.sdk.camera.OrientationHelper;

class CardRectCoordsMapper {

    private static final int[] DEFAULT_CAMERA_RESOLUTION = {1280, 720};

    private static final int DEFAULT_CAMERA_ROTATION = 90;

    private static final Rect DEFAULT_CAMERA_RECT = new Rect(432, 30, 432 + 416, 30 + 660);

    private static final Rect DEFAULT_CAMERA_RECT_LANDSCAPE = new Rect(310, 152, 970, 568);

    private static final PointF DEFAULT_CARD_NUMBER_POS = new PointF(60f,268f);

    private static final PointF DEFAULT_CARD_DATE_POS = new PointF(289f,321f);

    private static final PointF DEFAULT_CARD_HOLDER_POS = new PointF(33f,364f);

    private static final float DEFAULT_CARD_NUMBER_FONT_SIZE = 40;

    private static final float DEFAULT_CARD_DATE_FONT_SIZE = 27;

    private static final float DEFAULT_CARD_HOLDER_FONT_SIZE = 27;

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String TAG = "CardRectCoordsMapper";

    /**
     * Card rect (in camera coordinates)
     */
    private final Rect mCardCameraRectRaw = new Rect();

    /**
     * Camera resolution
     */
    private final int mCameraPreviewSize[] = new int[]{DEFAULT_CAMERA_RESOLUTION[0], DEFAULT_CAMERA_RESOLUTION[1]};

    /**
     * Camera rotation
     */
    private int mCameraRotation = DEFAULT_CAMERA_ROTATION;

    /**
     * Card rect (in view coordinates)
     */
    private final Rect mCardRect = new Rect();

    private final Rect mCardCameraRect = new Rect();

    private final PointF mCardNumberPos = new PointF();

    private final PointF mCardDatePos = new PointF();

    private final PointF mCardHolderPos = new PointF();

    private int mViewWidth = 1280;

    private int mViewHeight = 720;

    private int mTranslateX = 0;
    private int mTranslateY = 0;
    private float mScale = 1;

    private boolean mCameraRectInitialized;


    CardRectCoordsMapper() {
        mCardCameraRectRaw.set(DEFAULT_CAMERA_RECT);
    }

    public Rect getCardRect() {
        return mCardRect;
    }

    public PointF getCardNumberPos() {
        return mCardNumberPos;
    }

    public float getCardNumberFontSize() {
        return DEFAULT_CARD_NUMBER_FONT_SIZE * mScale;
    }

    public PointF getCardDatePos() {
        return mCardDatePos;
    }

    public float getCardDateFontSize() {
        return DEFAULT_CARD_DATE_FONT_SIZE * mScale;
    }


    public PointF getCardHolderPos() {
        return mCardHolderPos;
    }

    public float getCardHolderFontSize() {
        return DEFAULT_CARD_HOLDER_FONT_SIZE * mScale;
    }

    public boolean setViewSize(int width, int height) {
        if (width != 0
                && height != 0
                && mViewWidth != width
                && mViewHeight != height) {
            mViewWidth = width;
            mViewHeight = height;
            if (!mCameraRectInitialized) refreshCameraDefaults();
            sync();
            return true;
        } else {
            return false;
        }
    }

    private void refreshCameraDefaults() {
        if (mViewHeight > mViewWidth) {
            mCameraRotation = DEFAULT_CAMERA_ROTATION;
            mCameraPreviewSize[0] = DEFAULT_CAMERA_RESOLUTION[0];
            mCameraPreviewSize[1] = DEFAULT_CAMERA_RESOLUTION[1];
            mCardCameraRectRaw.set(DEFAULT_CAMERA_RECT);
        } else {
            mCameraRotation = 0;
            mCameraPreviewSize[0] = DEFAULT_CAMERA_RESOLUTION[0];
            mCameraPreviewSize[1] = DEFAULT_CAMERA_RESOLUTION[1];
            mCardCameraRectRaw.set(DEFAULT_CAMERA_RECT_LANDSCAPE);
        }
    }

    public boolean setCameraParameters(int previewSizeWidth,
                                       int previewSizeHeight,
                                       int rotation,
                                       Rect cardFrame) {
        boolean resolutionEq = (previewSizeWidth == mCameraPreviewSize[0])
                && (previewSizeHeight == mCameraPreviewSize[1]);
        boolean rotationEq = mCameraRotation == rotation;
        boolean cardFrameEq = mCardCameraRectRaw.equals(cardFrame);

        if (resolutionEq && rotationEq && cardFrameEq && mCameraRectInitialized) {
            return false;
        }

        mCameraPreviewSize[0] = previewSizeWidth;
        mCameraPreviewSize[1] = previewSizeHeight;
        mCameraRotation = rotation;
        mCardCameraRectRaw.set(cardFrame);
        mCameraRectInitialized = true;
        sync();
        return true;
    }

    private void sync() {
        refreshTransform();
        refreshCardRect();
        refreshCardTextPositions();
    }

    private void refreshTransform() {
        float scale;
        int translateY = 0;
        int translateX = 0;
        float cameraHeight = getCameraHeightRotated();
        float cameraWidth = getCameraWidthRotated();

        // Center crop
        if (cameraWidth * mViewHeight > cameraHeight * mViewWidth) {
            scale = mViewHeight / cameraHeight;
            translateX = (int) ((mViewWidth - cameraWidth * scale) / 2f);
        } else {
            scale = mViewWidth / cameraWidth;
            translateY = (int) ((mViewHeight - cameraHeight * scale) / 2f);
        }

        mScale = scale;
        mTranslateX = translateX;
        mTranslateY = translateY;

        OrientationHelper.rotateRect(mCardCameraRectRaw, mCameraPreviewSize[0], mCameraPreviewSize[1], mCameraRotation, mCardCameraRect);

        if (DBG) Log.d(TAG, "refreshTransform() widthXheight: " + mViewWidth + "x" + mViewHeight
                +"; translateXY: [" + translateX +"," + mTranslateY + "], scale: " + mScale);
    }

    private void refreshCardRect() {
        mCardRect.left = (int) (0.5f + mScale * mCardCameraRect.left) + mTranslateX;
        mCardRect.top = (int) (0.5f + mScale * mCardCameraRect.top) + mTranslateY;
        mCardRect.right = (int) (0.5f + mScale * mCardCameraRect.right) + mTranslateX;
        mCardRect.bottom = (int) (0.5f + mScale * mCardCameraRect.bottom) + mTranslateY;
    }

    private void refreshCardTextPositions() {
        mapToViewCoordinates(DEFAULT_CARD_NUMBER_POS, mCardNumberPos);
        mapToViewCoordinates(DEFAULT_CARD_DATE_POS, mCardDatePos);
        mapToViewCoordinates(DEFAULT_CARD_HOLDER_POS, mCardHolderPos);
    }

    public void mapToViewCoordinates(PointF src, PointF dst) {
        dst.x = mScale * src.x + mCardRect.left;
        dst.y = mScale * src.y + mCardRect.top;
    }

    private int getCameraWidthRotated() {
        return (mCameraRotation == 0) || (mCameraRotation == 180) ? mCameraPreviewSize[0] : mCameraPreviewSize[1];
    }

    private int getCameraHeightRotated() {
        return (mCameraRotation == 0) || (mCameraRotation == 180) ? mCameraPreviewSize[1] : mCameraPreviewSize[0];
    }
}
