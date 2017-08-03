package cards.pay.paycardsrecognizer.sdk.ui.views;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import cards.pay.paycardsrecognizer.sdk.R;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TabletCardRecognitionHolderLinearLayout extends LinearLayout {

    private View mSurfaceView;

    public TabletCardRecognitionHolderLinearLayout(final Context context) {
        super(context);
    }

    public TabletCardRecognitionHolderLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TabletCardRecognitionHolderLinearLayout(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSurfaceView = findViewById(R.id.wocr_card_recognition_view);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int surfaceWidth = mSurfaceView.getMeasuredWidth();
        final int surfaceHeight = mSurfaceView.getMeasuredHeight();

        final int resultWidth;
        final int resultHeight;
        if (Configuration.ORIENTATION_LANDSCAPE == getResources().getConfiguration().orientation) {
            resultWidth = (int) (surfaceHeight * 1.3f);
            resultHeight = surfaceHeight;
        } else {
            resultWidth = surfaceWidth;
            resultHeight = (int) (surfaceWidth * 1.1f);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(resultWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(resultHeight, MeasureSpec.EXACTLY));
    }
}