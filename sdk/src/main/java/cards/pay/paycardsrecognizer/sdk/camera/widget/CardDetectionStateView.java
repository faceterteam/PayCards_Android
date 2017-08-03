package cards.pay.paycardsrecognizer.sdk.camera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionConstants;
import cards.pay.paycardsrecognizer.sdk.ndk.RecognitionResult;
import cards.pay.paycardsrecognizer.sdk.utils.CardUtils;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;
import cards.pay.paycardsrecognizer.sdk.utils.Fonts;

/**
 * This view is overlaid on top of the camera preview. It adds the card rectangle and partial
 * transparency outside it
 */
public class CardDetectionStateView extends View {

    private static final boolean DBG = Constants.DEBUG;
    private static final String TAG = "CardDetectionStateView";

    private static final float RECT_CORNER_PADDING_LEFT = 1;

    private static final float RECT_CORNER_PADDING_TOP = 1;

    private static final float RECT_CORNER_LINE_STROKE_WIDTH = 5f;

    private static final float RECT_CORNER_RADIUS = 8;

    private static final int TOP_EDGE = RecognitionConstants.DETECTED_BORDER_TOP;
    private static final int BOTTOM_EDGE = RecognitionConstants.DETECTED_BORDER_BOTTOM;
    private static final int LEFT_EDGE = RecognitionConstants.DETECTED_BORDER_LEFT;
    private static final int RIGHT_EDGE = RecognitionConstants.DETECTED_BORDER_RIGHT;

    private volatile int mDetectionState;

    private volatile String mRecognitionResultDate;
    private volatile String mRecognitionResultCardNumber;
    private volatile String mRecognitionResultHolder;

    private CardRectCoordsMapper mCardFrame;

    private float mDisplayDensity;

    private Typeface mCardTypeface;

    private final Rect mCardRectInvalidation = new Rect();

    private float mCornerPaddingLeft;

    private float mCornerPaddingTop;

    private float mCornerLineWidth;

    private float mCornerRadius;

    private Drawable mCardGradientDrawable;

    private BitmapDrawable mCornerTopLeftDrawable, mCornerTopRightDrawable,
            mCornerBottomLeftDrawable, mCornerBottomRightDrawable;

    private BitmapDrawable mLineTopDrawable, mLineLeftDrawable,
            mLineRightDrawable, mLineBottomDrawable;

    private Paint mBackgroundPaint;

    private Paint mCardNumberPaint, mCardDatePaint, mCardHolderPaint;

    public CardDetectionStateView(Context context) {
        this(context, null);
    }

    public CardDetectionStateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CardDetectionStateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        float density = getResources().getDisplayMetrics().density;
        mDisplayDensity = density;

        mCardFrame = new CardRectCoordsMapper();

        int mBackgroundDrawableColor = context.getResources().getColor(R.color.wocr_card_shadow_color);

        mCornerPaddingTop = density * RECT_CORNER_PADDING_TOP;
        mCornerPaddingLeft = density * RECT_CORNER_PADDING_LEFT;
        mCornerLineWidth = density * RECT_CORNER_LINE_STROKE_WIDTH;
        mCornerRadius = density * RECT_CORNER_RADIUS;

        mCardGradientDrawable = context.getResources().getDrawable(R.drawable.wocr_frame_rect_gradient);

        initCornerDrawables(context);
        initLineDrawables(context);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(mBackgroundDrawableColor);

        mCardTypeface = Fonts.getCardFont(context);
        mCardNumberPaint = createCardTextPaint();
        mCardDatePaint = createCardTextPaint();
        mCardHolderPaint = createCardTextPaint();

        if (isInEditMode()) {
            mDetectionState = TOP_EDGE | BOTTOM_EDGE | LEFT_EDGE | RIGHT_EDGE;
            mRecognitionResultCardNumber = CardUtils.prettyPrintCardNumber("1234567890123456");
            mRecognitionResultDate = "05/18";
            mRecognitionResultHolder = "CARDHOLDER NAME";
        }
    }

    private Paint createCardTextPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
        paint.setTypeface(mCardTypeface);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(6, 3.0f, 3.0f, Color.BLACK);
        paint.setTextSize(12 * mDisplayDensity);
        return paint;
    }

    private void initCornerDrawables(Context context) {
        mCornerTopLeftDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.wocr_card_frame_rect_corner_top_left);

        Matrix m = new Matrix();
        Bitmap bitmap = mCornerTopLeftDrawable.getBitmap();

        m.setRotate(90);
        mCornerTopRightDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));

        m.setRotate(180);
        mCornerBottomRightDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));

        m.setRotate(270);
        mCornerBottomLeftDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));

    }

    private void initLineDrawables(Context context) {
        mLineTopDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.wocr_card_frame_rect_line_top);
        Matrix m = new Matrix();
        Bitmap bitmap = mLineTopDrawable.getBitmap();

        m.setRotate(90);
        mLineRightDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));

        m.setRotate(180);
        mLineBottomDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));

        m.setRotate(270);
        mLineLeftDrawable = new BitmapDrawable(context.getResources(),
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true));
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DBG) Log.d("CameraActivity", "onSizeChanged w,h: " + w + "," + h);
        boolean changed = mCardFrame.setViewSize(w, h);
        if (changed) refreshCardRectCoords();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        if (mCardGradientDrawable.getBounds().width() == 0) return;
        drawBackground(canvas);
        drawCorners(canvas);
        drawRecognitionResult(canvas);
    }

    private void drawBackground(Canvas canvas) {
        Rect rect = mCardFrame.getCardRect();
        // top
        canvas.drawRect(0, 0, getWidth(), rect.top, mBackgroundPaint);
        // bottom
        canvas.drawRect(0, rect.bottom, getWidth(), getHeight(), mBackgroundPaint);
        // left
        canvas.drawRect(0, rect.top, rect.left, rect.bottom, mBackgroundPaint);
        // right
        canvas.drawRect(rect.right, rect.top, getWidth(), rect.bottom, mBackgroundPaint);
    }

    private void drawCorners(Canvas canvas) {
        final int detectionState = mDetectionState;
        mCardGradientDrawable.draw(canvas);
        mCornerTopLeftDrawable.draw(canvas);
        mCornerTopRightDrawable.draw(canvas);
        mCornerBottomLeftDrawable.draw(canvas);
        mCornerBottomRightDrawable.draw(canvas);

        // Detected edges
        if (0 != (detectionState & TOP_EDGE)) {
            mLineTopDrawable.draw(canvas);
        }
        if (0 != (detectionState & LEFT_EDGE)) {
            mLineLeftDrawable.draw(canvas);
        }
        if (0 != (detectionState & RIGHT_EDGE)) {
            mLineRightDrawable.draw(canvas);
        }
        if (0 != (detectionState & BOTTOM_EDGE)) {
            mLineBottomDrawable.draw(canvas);
        }
    }

    private void drawRecognitionResult(Canvas canvas) {
        final String resultDate = mRecognitionResultDate;
        final String resultNumber = mRecognitionResultCardNumber;
        final String resultHolder = mRecognitionResultHolder;

        if (!TextUtils.isEmpty(resultNumber)) {
            canvas.drawText(resultNumber,
                    mCardFrame.getCardNumberPos().x,
                    mCardFrame.getCardNumberPos().y,
                    mCardNumberPaint);
        }

        if (!TextUtils.isEmpty(resultDate)) {
            canvas.drawText(resultDate,
                    mCardFrame.getCardDatePos().x,
                    mCardFrame.getCardDatePos().y,
                    mCardDatePaint);
        }

        if (!TextUtils.isEmpty(resultHolder)) {
            canvas.drawText(resultHolder,
                    mCardFrame.getCardHolderPos().x,
                    mCardFrame.getCardHolderPos().y,
                    mCardHolderPaint);
        }
    }

    private void refreshCardRectCoords() {
        refreshCardRectInvalidation();
        refreshDrawableBounds();
        refreshTextSize();
    }

    private void refreshCardRectInvalidation() {
        Rect cardRect = mCardFrame.getCardRect();
        int border = (int)(0.5f + mCornerPaddingLeft) + (int)(0.5f + mCornerLineWidth / 2f);
        mCardRectInvalidation.left = cardRect.left - border;
        mCardRectInvalidation.top = cardRect.top - border;
        mCardRectInvalidation.right = cardRect.right + border;
        mCardRectInvalidation.bottom = cardRect.bottom + border;
    }

    private void refreshDrawableBounds() {
        Rect cardRect = mCardFrame.getCardRect();
        mCardGradientDrawable.setBounds(cardRect);

        int rectWidth = mCornerTopLeftDrawable.getIntrinsicWidth();
        int rectHeight = mCornerTopLeftDrawable.getIntrinsicHeight();
        int cornerStroke = (int)(0.5f + mCornerLineWidth / 2f);

        int left1 = Math.round(cardRect.left - mCornerPaddingLeft - cornerStroke);
        int left2 = Math.round(cardRect.right - rectWidth + mCornerPaddingLeft + cornerStroke);
        int top1 = Math.round(cardRect.top - mCornerPaddingTop - cornerStroke);
        int top2 = Math.round(cardRect.bottom - rectHeight + mCornerPaddingTop + cornerStroke);

        // Corners
        mCornerTopLeftDrawable.setBounds(left1, top1, left1 + rectWidth, top1 + rectHeight);
        mCornerTopRightDrawable.setBounds(left2, top1, left2 + rectWidth, top1 + rectWidth);
        mCornerBottomLeftDrawable.setBounds(left1, top2, left1 + rectWidth, top2 + rectHeight);
        mCornerBottomRightDrawable.setBounds(left2, top2, left2 + rectWidth, top2 + rectHeight);

        // Lines
        int offset = (int)mCornerRadius;
        mLineTopDrawable.setBounds(
                left1 + offset,
                top1,
                left2 + rectWidth - offset,
                top1 + mLineTopDrawable.getIntrinsicHeight());
        mLineLeftDrawable.setBounds(left1, top1 + offset,
                left1 + mLineLeftDrawable.getIntrinsicWidth(), top2 + rectHeight - offset);
        mLineRightDrawable.setBounds(
                left2 + rectWidth - mLineRightDrawable.getIntrinsicWidth(),
                top1 + offset,
                left2 + rectWidth,
                top2 + rectHeight - offset
        );
        mLineBottomDrawable.setBounds(
                left1 + offset,
                top2 + rectHeight - mLineBottomDrawable.getIntrinsicHeight(),
                left2 + rectWidth - offset,
                top2 + rectHeight
        );
    }

    private void refreshTextSize() {
        mCardNumberPaint.setTextSize(mCardFrame.getCardNumberFontSize());
        mCardDatePaint.setTextSize(mCardFrame.getCardDateFontSize());
        mCardHolderPaint.setTextSize(mCardFrame.getCardHolderFontSize());
    }

    public synchronized void setDetectionState(final int detectionState) {
        if (mDetectionState != detectionState) {
            mDetectionState = detectionState;
            postInvalidate(mCardRectInvalidation.left,
                    mCardRectInvalidation.top,
                    mCardRectInvalidation.right,
                    mCardRectInvalidation.bottom
                    );
        }
    }

    public synchronized void setRecognitionResult(RecognitionResult result) {
        if (DBG) Log.d(TAG, "setRecognitionResult() called with: " +  "result = [" + result + "]");

        if (!TextUtils.isEmpty(result.getNumber())) {
            mRecognitionResultCardNumber = CardUtils.prettyPrintCardNumber(result.getNumber());
        } else {
            mRecognitionResultCardNumber = null;
        }

        if (!TextUtils.isEmpty(result.getDate())) {
            mRecognitionResultDate = result.getDate().substring(0, 2) + '/' + result.getDate().substring(2);
        } else {
            mRecognitionResultDate = null;
        }

        mRecognitionResultHolder = result.getName();

        postInvalidate(mCardRectInvalidation.left,
                mCardRectInvalidation.top,
                mCardRectInvalidation.right,
                mCardRectInvalidation.bottom
        );
    }

    void setCameraParameters(int previewSizeWidth,
                                    int previewSizeHeight,
                                    int rotation,
                                    Rect cardFrame) {
        boolean changed = mCardFrame.setCameraParameters(previewSizeWidth, previewSizeHeight, rotation, cardFrame);
        if (changed) {
            refreshCardRectCoords();
            invalidate();
        }
    }
}