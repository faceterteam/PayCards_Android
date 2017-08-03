package cards.pay.paycardsrecognizer.sdk.ui.views;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ProgressBarIndeterminate extends ProgressBar {

    public ProgressBarIndeterminate(Context context) {
        super(context);
    }

    public ProgressBarIndeterminate(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressBarIndeterminate(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setVisibility(int v) {
        super.setVisibility(v);
        clearAnimation();
        if (v == View.VISIBLE) {
            setAlpha(1);
        }
    }

    public void hideSlow() {
        if (getVisibility() != View.VISIBLE) return;
        animate()
                .alpha(0)
                .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(View.GONE);
                        setAlpha(1);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
    }
}
