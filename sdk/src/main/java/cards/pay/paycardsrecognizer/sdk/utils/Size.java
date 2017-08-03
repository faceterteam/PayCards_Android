package cards.pay.paycardsrecognizer.sdk.utils;

public class Size implements Comparable<Size> {

    public final int width;

    public final int height;

    public Size(final int width, final int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (width != size.width) return false;
        return height == size.height;

    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    @Override
    public int compareTo(final Size another) {
        if (Math.max(width, height) > Math.max(another.width, another.height) &&
                Math.min(width, height) > Math.min(another.width, another.height)) {
            return -1;
        } else if (Math.max(width, height) < Math.max(another.width, another.height) &&
                Math.min(width, height) < Math.min(another.width, another.height)) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }
}
