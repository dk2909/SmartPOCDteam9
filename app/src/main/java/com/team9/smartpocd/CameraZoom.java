package com.team9.smartpocd;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.math.MathUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

/**
 * CameraZoom Class
 */
public final class CameraZoom
{
    /** Constants */
    /* Default zoom factor */
    public static final float DEFAULT_ZOOM_FACTOR = 1.0f;

    /** Properties */
    /* Maximum zoom factor */
    public float maxZoom;
    /* Check if zoom has supported */
    public boolean hasSupport;
    /* Crop region rect */
    @NonNull
    private final Rect cropRegion = new Rect();
    /* Size of camera sensor */
    @Nullable
    private final Rect sensorSize;

    /** Constructor with characteristics arg */
    public CameraZoom(@NonNull final CameraCharacteristics characteristics) {
        this.sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        if (this.sensorSize == null) {
            this.maxZoom = CameraZoom.DEFAULT_ZOOM_FACTOR;
            this.hasSupport = false;
            return;
        }

        final Float value = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

        this.maxZoom = ((value == null) || (value < CameraZoom.DEFAULT_ZOOM_FACTOR))
                ? CameraZoom.DEFAULT_ZOOM_FACTOR
                : value;

        this.hasSupport = (Float.compare(this.maxZoom, CameraZoom.DEFAULT_ZOOM_FACTOR) > 0);
    }

    /** Set the zoom of camera by factor */
    public void setZoom(@NonNull final CaptureRequest.Builder builder, final float zoom) {
        if (!this.hasSupport) {
            return;
        }

        final float newZoom = MathUtils.clamp(zoom, CameraZoom.DEFAULT_ZOOM_FACTOR, this.maxZoom);

        assert this.sensorSize != null;
        final int centerX = this.sensorSize.width() / 2;
        final int centerY = this.sensorSize.height() / 2;
        final int deltaX  = (int)((0.5f * this.sensorSize.width()) / newZoom);
        final int deltaY  = (int)((0.5f * this.sensorSize.height()) / newZoom);

        this.cropRegion.set(centerX - deltaX,
                centerY - deltaY,
                centerX + deltaX,
                centerY + deltaY);

        builder.set(CaptureRequest.SCALER_CROP_REGION, this.cropRegion);
    }

    /** Retrieve the current crop rect */
    public final Rect getCropRect() {
        return new Rect(cropRegion.left, cropRegion.top, cropRegion.right, cropRegion.bottom);
    }
}
