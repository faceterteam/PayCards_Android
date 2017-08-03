package cards.pay.paycardsrecognizer.sdk.ui;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ScanCardRequest implements Parcelable {

    public static final boolean DEFAULT_ENABLE_SOUND = true;

    public static final boolean DEFAULT_SCAN_EXPIRATION_DATE = true;

    public static final boolean DEFAULT_SCAN_CARD_HOLDER = true;

    public static final boolean DEFAULT_GRAB_CARD_IMAGE = false;

    private final boolean mEnableSound;

    private final boolean mScanExpirationDate;

    private final boolean mScanCardHolder;

    private final boolean mGrabCardImage;

    private static final ScanCardRequest sDefaultInstance = new ScanCardRequest(
            DEFAULT_ENABLE_SOUND, DEFAULT_SCAN_EXPIRATION_DATE, DEFAULT_SCAN_CARD_HOLDER,
            DEFAULT_GRAB_CARD_IMAGE);

    public static ScanCardRequest getDefault() {
        return sDefaultInstance;
    }

    public ScanCardRequest(boolean enableSound,
                           boolean scanExpirationDate,
                           boolean scanCardHolder,
                           boolean grabCardImage) {
        this.mEnableSound = enableSound;
        this.mScanExpirationDate = scanExpirationDate;
        this.mScanCardHolder = scanCardHolder;
        this.mGrabCardImage = grabCardImage;
    }

    public boolean isSoundEnabled() {
        return mEnableSound;
    }

    public boolean isScanExpirationDateEnabled() {
        return mScanExpirationDate;
    }

    public boolean isScanCardHolderEnabled() {
        return mScanCardHolder;
    }

    public boolean isGrabCardImageEnabled() {
        return mGrabCardImage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScanCardRequest that = (ScanCardRequest) o;

        if (mEnableSound != that.mEnableSound) return false;
        if (mScanExpirationDate != that.mScanExpirationDate) return false;
        if (mScanCardHolder != that.mScanCardHolder) return false;
        return mGrabCardImage == that.mGrabCardImage;
    }

    @Override
    public int hashCode() {
        int result = (mEnableSound ? 1 : 0);
        result = 31 * result + (mScanExpirationDate ? 1 : 0);
        result = 31 * result + (mScanCardHolder ? 1 : 0);
        result = 31 * result + (mGrabCardImage ? 1 : 0);
        return result;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.mEnableSound ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mScanExpirationDate ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mScanCardHolder ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mGrabCardImage ? (byte) 1 : (byte) 0);
    }

    protected ScanCardRequest(Parcel in) {
        this.mEnableSound = in.readByte() != 0;
        this.mScanExpirationDate = in.readByte() != 0;
        this.mScanCardHolder = in.readByte() != 0;
        this.mGrabCardImage = in.readByte() != 0;
    }

    public static final Parcelable.Creator<ScanCardRequest> CREATOR = new Parcelable.Creator<ScanCardRequest>() {
        @Override
        public ScanCardRequest createFromParcel(Parcel source) {
            return new ScanCardRequest(source);
        }

        @Override
        public ScanCardRequest[] newArray(int size) {
            return new ScanCardRequest[size];
        }
    };
}
