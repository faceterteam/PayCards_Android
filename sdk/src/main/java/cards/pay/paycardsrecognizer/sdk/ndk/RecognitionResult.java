package cards.pay.paycardsrecognizer.sdk.ndk;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecognitionResult implements Parcelable {

    @Nullable
    private final String number;

    @Nullable
    private final String date;

    @Nullable
    private final String name;

    @Nullable
    private final String nameRaw;

    @Nullable
    private final Rect numberImageRect;

    @Nullable
    private final Bitmap cardImage;

    private final boolean isFirst;

    private final boolean isFinal;

    private static final RecognitionResult sEmpty = new RecognitionResult.Builder().setIsFirst(true).build();

    public static RecognitionResult empty() {
        return sEmpty;
    }

    public RecognitionResult(@Nullable String number,
                             @Nullable String name,
                             @Nullable String date,
                             @Nullable Rect numberImageRect,
                             @Nullable String nameRaw,
                             @Nullable Bitmap cardImage,
                             boolean isFirst,
                             boolean isFinal) {
        this.number = number;
        this.name = name;
        this.date = date;
        this.nameRaw = nameRaw;
        this.cardImage = cardImage;
        this.numberImageRect = numberImageRect;
        this.isFirst = isFirst;
        this.isFinal = isFinal;
    }

    private RecognitionResult(Builder builder) {
        cardImage = builder.cardImage;
        number = builder.number;
        date = builder.date;
        name = builder.name;
        nameRaw = builder.nameRaw;
        numberImageRect = builder.numberImageRect;
        isFirst = builder.isFirst;
        isFinal = builder.isFinal;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    @Nullable
    public String getNumber() {
        return number;
    }

    @Nullable
    public String getDate() {
        return date;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getNameRaw() {
        return nameRaw;
    }

    @Nullable
    public Bitmap getCardImage() {
        return cardImage;
    }

    @Nullable
    public Rect getNumberImageRect() {return numberImageRect; }

    public boolean isFirst() {
        return isFirst;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public int getCardImageWidth() {
        return getCardImage() == null ? 0 : getCardImage().getWidth();
    }

    public int getCardImageHeight() {
        return getCardImage() == null ? 0 : getCardImage().getHeight();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecognitionResult that = (RecognitionResult) o;

        if (isFirst != that.isFirst) return false;
        if (isFinal != that.isFinal) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (nameRaw != null ? !nameRaw.equals(that.nameRaw) : that.nameRaw != null) return false;
        if (numberImageRect != null ? !numberImageRect.equals(that.numberImageRect) : that.numberImageRect != null)
            return false;
        return cardImage != null ? cardImage.equals(that.cardImage) : that.cardImage == null;
    }

    @Override
    public int hashCode() {
        int result = number != null ? number.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (nameRaw != null ? nameRaw.hashCode() : 0);
        result = 31 * result + (numberImageRect != null ? numberImageRect.hashCode() : 0);
        result = 31 * result + (cardImage != null ? cardImage.hashCode() : 0);
        result = 31 * result + (isFirst ? 1 : 0);
        result = 31 * result + (isFinal ? 1 : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.isFirst ? 1 : 0);
        dest.writeInt(this.isFinal ? 1 : 0);
        dest.writeString(this.number);
        dest.writeString(this.date);
        dest.writeString(this.name);
        dest.writeString(this.nameRaw);
        dest.writeParcelable(this.numberImageRect, 0);
        dest.writeParcelable(this.cardImage, 0);
    }

    protected RecognitionResult(Parcel in) {
        this.isFirst = in.readInt() != 0;
        this.isFinal = in.readInt() != 0;
        this.number = in.readString();
        this.date = in.readString();
        this.name = in.readString();
        this.nameRaw = in.readString();
        this.numberImageRect = in.readParcelable(Rect.class.getClassLoader());
        this.cardImage = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Creator<RecognitionResult> CREATOR = new Creator<RecognitionResult>() {
        public RecognitionResult createFromParcel(Parcel source) {
            return new RecognitionResult(source);
        }

        public RecognitionResult[] newArray(int size) {
            return new RecognitionResult[size];
        }
    };

    public static final class Builder {
        private boolean isFirst = true;
        private boolean isFinal = true;
        private Bitmap cardImage;
        private String number;
        private String date;
        private String name;
        private String nameRaw;
        private Rect numberImageRect;

        public Builder() {
        }

        public Builder(RecognitionResult copy) {
            this.isFirst = copy.isFirst;
            this.isFinal = copy.isFinal;
            this.cardImage = copy.cardImage;
            this.number = copy.number;
            this.date = copy.date;
            this.name = copy.name;
            this.nameRaw = copy.nameRaw;
            this.numberImageRect = copy.numberImageRect;
        }

        public Builder setCardImage(Bitmap val) {
            cardImage = val;
            return this;
        }

        public Builder setNumber(String val) {
            number = val;
            return this;
        }

        public Builder setDate(String val) {
            date = val;
            return this;
        }

        public Builder setName(String val) {
            name = val;
            return this;
        }

        public Builder setNameRaw(String val) {
            nameRaw = val;
            return this;
        }

        public Builder setNumberImageRect(Rect val) {
            numberImageRect = val;
            return this;
        }

        public Builder setIsFinal(boolean val) {
            isFinal = val;
            return this;
        }

        public Builder setIsFirst(boolean val) {
            isFirst = val;
            return this;
        }

        public RecognitionResult build() {
            return new RecognitionResult(this);
        }
    }
}
