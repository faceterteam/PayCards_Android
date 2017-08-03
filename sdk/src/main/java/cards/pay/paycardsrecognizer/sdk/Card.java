package cards.pay.paycardsrecognizer.sdk;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.Serializable;

import cards.pay.paycardsrecognizer.sdk.utils.CardUtils;

public class Card implements Serializable, Parcelable {

    private static final long serialVersionUID = 1L;
    private final String mCardNumber;
    @Nullable
    private final String mCardHolder;
    @Nullable
    private final String mExpirationDate;

    public Card(String number, @Nullable String holder, @Nullable String expirationDate) {
        this.mCardNumber = number;
        this.mCardHolder = holder;
        this.mExpirationDate = expirationDate;
    }

    /**
     * @return card number (only digits)
     */
    public String getCardNumber() {
        return mCardNumber;
    }

    public String getCardNumberRedacted() {
        return CardUtils.getCardNumberRedacted(mCardNumber);
    }

    /**
     * @return card holder name
     */
    @Nullable
    public String getCardHolderName() {
        return mCardHolder;
    }

    /**
     * @return card expiration date in "MM/yy" format
     */
    @Nullable
    public String getExpirationDate() {
        return mExpirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;

        if (mCardNumber != null ? !mCardNumber.equals(card.mCardNumber) : card.mCardNumber != null)
            return false;
        if (mCardHolder != null ? !mCardHolder.equals(card.mCardHolder) : card.mCardHolder != null)
            return false;
        return mExpirationDate != null ? mExpirationDate.equals(card.mExpirationDate) : card.mExpirationDate == null;
    }

    @Override
    public int hashCode() {
        int result = mCardNumber != null ? mCardNumber.hashCode() : 0;
        result = 31 * result + (mCardHolder != null ? mCardHolder.hashCode() : 0);
        result = 31 * result + (mExpirationDate != null ? mExpirationDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Card{" +
                "mCardNumber='" + getCardNumberRedacted() + '\'' +
                ", mCardHolder='" + mCardHolder + '\'' +
                ", mExpirationDate='" + mExpirationDate + '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mCardNumber);
        dest.writeString(this.mCardHolder);
        dest.writeString(this.mExpirationDate);
    }

    protected Card(Parcel in) {
        this.mCardNumber = in.readString();
        this.mCardHolder = in.readString();
        this.mExpirationDate = in.readString();
    }

    public static final Creator<Card> CREATOR = new Creator<Card>() {
        @Override
        public Card createFromParcel(Parcel source) {
            return new Card(source);
        }

        @Override
        public Card[] newArray(int size) {
            return new Card[size];
        }
    };
}
