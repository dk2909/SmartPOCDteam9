package com.team9.smartpocd;

//import android.support.annotation.NonNull;
import androidx.annotation.NonNull;

/**
 * Immutable class for describing width and height dimensions in pixels.
 */
public class OrderedSize implements Comparable<OrderedSize> {

    private final int width;
    private final int height;

    /**
     * Create a new immutable Size instance.
     *
     * @param width  The width of the size, in pixels
     * @param height The height of the size, in pixels
     */
    OrderedSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof OrderedSize) {
            OrderedSize size = (OrderedSize) o;
            return width == size.width && height == size.height;
        }
        return false;
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

    @Override
    public int hashCode() {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return height ^ ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));
    }

    @Override
    public int compareTo(@NonNull OrderedSize another) {
        int area = getAreaSize();
        int anotherArea = another.getAreaSize();
        if (area != anotherArea) {
            return area - anotherArea;
        } else {
            return width - another.width;
        }
    }

    int getAreaSize() {
        return width * height;
    }
}
