package cards.pay.paycardsrecognizer.sdk.utils;

import android.support.annotation.RestrictTo;

import cards.pay.paycardsrecognizer.sdk.BuildConfig;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Constants {

    private Constants() { }

    public static final boolean DEBUG = BuildConfig.DEBUG;

    public static final String ASSETS_DIR = "cardrecognizer";

    public static final String MODEL_DIR = "cardrecognizer/model";

    public static final int NEURO_DATA_VERSION = 9;

    public static final String PAYCARDS_URL = "https://pay.cards";
}
