package cards.pay.paycardsrecognizer.sdk.camera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.RestrictTo;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import cards.pay.paycardsrecognizer.sdk.R;
import cards.pay.paycardsrecognizer.sdk.utils.Constants;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CameraPreviewLayout extends FrameLayout {

    private static final String TAG = "CameraPreviewLayout";
    private static final boolean DBG = Constants.DEBUG;

    private SurfaceView mSurfaceView;

    private CardDetectionStateView mDetectionStateOverlay;

    private OnWindowFocusChangedListener mWindowFocusChangedListener;

    private final CardRectCoordsMapper mCardFrame;

    /**
     * These are used for computing child frames based on their gravity.
     */
    private final Rect mTmpCard = new Rect();
    private final Rect mTmp = new Rect();

    public CameraPreviewLayout(Context context) {
        this(context, null);
    }

    public CameraPreviewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCardFrame = new CardRectCoordsMapper();
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public CardDetectionStateView getDetectionStateOverlay() {
        return mDetectionStateOverlay;
    }

    public void setOnWindowFocusChangedListener(OnWindowFocusChangedListener listener) {
        mWindowFocusChangedListener = listener;
    }

    public void setCameraParameters(int previewSizeWidth,
                                    int previewSizeHeight,
                                    int rotation,
                                    Rect cardFrame) {
        if (DBG) Log.d(TAG, "setCameraParameters() called with: " +  "previewSizeWidth = [" + previewSizeWidth + "], previewSizeHeight = [" + previewSizeHeight + "], rotation = [" + rotation + "], cardFrame = [" + cardFrame + "]");
        mDetectionStateOverlay.setCameraParameters(previewSizeWidth, previewSizeHeight, rotation, cardFrame);

        boolean changed = mCardFrame.setCameraParameters(previewSizeWidth, previewSizeHeight, rotation, cardFrame);
        if (changed && !ViewCompat.isInLayout(this)) requestLayout();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mCardFrame.setViewSize(w, h)) {
            if (!ViewCompat.isInLayout(this)) requestLayout();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSurfaceView = (SurfaceView) getChildAt(0);
        mDetectionStateOverlay = (CardDetectionStateView) getChildAt(1);
    }

    /**
     * Any layout manager that doesn't scroll will want this.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (DBG)
            Log.d(TAG, "onWindowFocusChanged() called with: " + "hasWindowFocus = [" + hasWindowFocus + "]");
        if (mWindowFocusChangedListener != null)
            mWindowFocusChangedListener.onWindowFocusChanged(this, hasWindowFocus);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        final int count = getChildCount();

        Rect cardRect = mCardFrame.getCardRect();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE
                    || lp.cardGravity == LayoutParams.UNSPECIFIED_CARD_GRAVITY) continue;

            final int layoutDirection = ViewCompat.getLayoutDirection(this);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            getChildRect(layoutDirection, cardRect, mTmp, lp, childWidth, childHeight);
            constrainChildRect(lp, mTmp, childWidth, childHeight);
            child.layout(mTmp.left, mTmp.top, mTmp.right, mTmp.bottom);
        }
    }


    @SuppressLint("RtlHardcoded")
    private void getChildRect(int layoutDirection,
                              Rect cardRect, Rect out, LayoutParams lp, int childWidth, int childHeight) {
        final int absCardGravity = GravityCompat.getAbsoluteGravity(
                resolveGravity(lp.cardGravity),
                layoutDirection);

        final int cardHgrav = absCardGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        final int cardVgrav = absCardGravity & Gravity.VERTICAL_GRAVITY_MASK;

        int left;
        int top;

        switch (cardHgrav) {
            default:
            case Gravity.LEFT:
                left = cardRect.left + lp.leftMargin;
                break;
            case Gravity.RIGHT:
                left = cardRect.right  - childWidth - lp.rightMargin;
                break;
            case Gravity.CENTER_HORIZONTAL:
                left = cardRect.left + cardRect.width() / 2 - childWidth / 2 + lp.leftMargin - lp.rightMargin;
                break;
        }

        switch (cardVgrav) {
            default:
            case Gravity.TOP:
                top = cardRect.top - childHeight - lp.bottomMargin;
                break;
            case Gravity.BOTTOM:
                top = cardRect.bottom + lp.topMargin;
                break;
            case Gravity.CENTER_VERTICAL:
                top = cardRect.top + cardRect.height() / 2 - childHeight / 2 + lp.topMargin - lp.bottomMargin;
                break;
        }

        out.set(left, top, left + childWidth, top + childHeight);
    }

    private static int resolveGravity(int gravity) {
        if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
            gravity |= GravityCompat.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
            gravity |= Gravity.TOP;
        }
        return gravity;
    }

    private void constrainChildRect(LayoutParams lp, Rect out, int childWidth, int childHeight) {
        final int width = getWidth();
        final int height = getHeight();

        // Obey margins and padding
        int left = Math.max(getPaddingLeft() + lp.leftMargin,
                Math.min(out.left,
                        width - getPaddingRight() - childWidth - lp.rightMargin));
        int top = Math.max(getPaddingTop() + lp.topMargin,
                Math.min(out.top,
                        height - getPaddingBottom() - childHeight - lp.bottomMargin));

        out.set(left, top, left + childWidth, top + childHeight);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CameraPreviewLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new CameraPreviewLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CameraPreviewLayout.LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CameraPreviewLayout.LayoutParams;
    }



    public static class LayoutParams extends FrameLayout.LayoutParams {
        public static final int UNSPECIFIED_CARD_GRAVITY = -1;

        /**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         */
        public int cardGravity = UNSPECIFIED_CARD_GRAVITY;


        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            // Pull the layout param values from the layout XML during
            // inflation.  This is not needed if you don't care about
            // changing the layout behavior in XML.
            @SuppressLint("CustomViewStyleable")
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.wocr_CameraPreviewLayout_Layout);
            if (a.hasValue(R.styleable.wocr_CameraPreviewLayout_Layout_wocr_layout_cardAlignGravity)) {
                cardGravity = a.getInt(R.styleable.wocr_CameraPreviewLayout_Layout_wocr_layout_cardAlignGravity, Gravity.CENTER);
            }
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
