package com.team9.smartpocd;

//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * MainActivity Class
 */
public class MainActivity extends AppCompatActivity
{
    /** Constants */
    /* TAG for logging */
    public static final String TAG = "MainActivity";
    /* Media storage directory file */
    public static final File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory (Environment.DIRECTORY_PICTURES), "MyPictures");
    /* Request code for camera permission */
    public static final int REQUEST_CAMERA_PERMISSION = 200;
    private StorageReference storageReference;
    public UploadTask uploadTask;
    /* Orientations */

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    public static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    public static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }
    private Switch focusSwitch;
    private boolean autoFocus;
    private View.OnTouchListener focusListner = new View.OnTouchListener() {
        @SuppressLint("DefaultLocale")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!autoFocus) {
                int cx = (int)event.getX();
                int cy = (int)event.getY();

                Rect rect = new Rect(Math.max(cx - 50, 0), Math.max(cy - 50, 0), Math.min(cx + 50, v.getWidth()), Math.min(cy + 50, v.getHeight()));
                focusArea = new MeteringRectangle[]{new MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_DONT_CARE)};

                updatePreview();
            }
            return true;
        }
    };
    private MeteringRectangle[] focusArea;

    static int getOrientation(int sensorOrientation, int displayRotation) {
        int degree = DEFAULT_ORIENTATIONS.get(displayRotation);
        switch (sensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                degree = DEFAULT_ORIENTATIONS.get(displayRotation);
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                degree = INVERSE_ORIENTATIONS.get(displayRotation);
                break;
        }
        return degree;
    }

    /** Components */
    /* Camera device */
    protected CameraDevice cameraDevice;
    /* Capture session */
    protected CameraCaptureSession cameraCaptureSessions;
    /* Builder for capture request */
    protected CaptureRequest.Builder captureRequestBuilder;
    /* Zoom control */
    protected CameraZoom cameraZoom;
    /* Texture view for previewing */
    protected TextureView textureView;

    /** Properties */
    /* Image size */
    protected Size imageDimension;
    /* Zoom step */
    protected float zoomStep = 1.0f;
    /* Factor value of current zoom */
    protected float zoomFactor = CameraZoom.DEFAULT_ZOOM_FACTOR;

    /** Internals */
    private File imageFile;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    /** Create a texture listener for surface texture */
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /** Create a state callback for camera device */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "onOpened");

            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();

            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /** onCreate() override */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Initialize the widgets
        super.onCreate(savedInstanceState);
        storageReference = FirebaseStorage.getInstance().getReference();
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.texture);
        assert textureView != null;
        ////
        textureView.setOnTouchListener(focusListner);
        focusSwitch = findViewById(R.id.switchFocus);
        focusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> autoFocus = isChecked);
        ////
        Button takePictureButton = findViewById(R.id.btn_capture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(v -> takePicture());
        Button galleryButton = findViewById(R.id.btn_gallery);
        assert galleryButton != null;
        galleryButton.setOnClickListener(v -> showGallery());
        ZoomControls zoomControls = findViewById(R.id.zoomControls);
        assert zoomControls != null;
        zoomControls.setOnZoomInClickListener(v -> zoomIn());
        zoomControls.setOnZoomOutClickListener(v -> zoomOut());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.zoomStepMenu) {
            final Dialog zoomStepDlg = new Dialog(MainActivity.this);
            zoomStepDlg.setContentView(R.layout.zoom_step_dialog);
            zoomStepDlg.setTitle("Zoom step");
            TextView zoomStepText = zoomStepDlg.findViewById(R.id.txtZoomStep);
            final String[] currZoomStepText = {"Zoom step: " + zoomStep};
            zoomStepText.setText(currZoomStepText[0]);
            SeekBar stepSeeker = zoomStepDlg.findViewById(R.id.seekerZoomStep);
            stepSeeker.setProgress((int)(zoomStep * 10));
            stepSeeker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    zoomStep = progress / 10.f;
                    currZoomStepText[0] = "Zoom step: " + zoomStep;
                    zoomStepText.setText(currZoomStepText[0]);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            Button okButton = zoomStepDlg.findViewById(R.id.btnOK);
            okButton.setOnClickListener(v -> zoomStepDlg.dismiss());

            zoomStepDlg.show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /** onRequestPermissionsResult() override */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /** onResume() override */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    /** onPause() override */
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");

        stopBackgroundThread();
        super.onPause();
    }

    /** Start up the background thread */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /** Stop the background thread */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return true if the given array contains the given integer */
    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    /** Open a default camera */
    private void openCamera() {
        Log.e(TAG, "-> camera open");

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;

        try {
            // Ready for opening
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            if (!contains(characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES), CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                Log.e(TAG, "Not supported RAW image format.");
                //Boast.makeText(MainActivity.this, "Not supported RAW format on this camera.").show();
                return;
            }

            cameraZoom = new CameraZoom(characteristics);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            SortedSet<OrderedSize> imageSizes = new TreeSet<>();

                    //for (Size size : map.getOutputSizes(SurfaceTexture.class))
                    for (Size size : map.getOutputSizes(ImageFormat.YUV_420_888))
                    {
                        OrderedSize s = new OrderedSize(size.getWidth(), size.getHeight());
                        imageSizes.add(s);
                    }


            imageDimension = new Size(imageSizes.last().getWidth(), imageSizes.last().getHeight());

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }

            // Open camera by id
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "<- camera open");
    }

    /** Create a camera preview */
    private void createCameraPreview() {
        Log.e(TAG, "-> camera preview create");

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    cameraZoom.setZoom(captureRequestBuilder, zoomFactor);
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "<- camera preview create");
    }

    /** Update the camera preview */
    private void updatePreview() {
        Log.e(TAG, "-> camera preview update");

        if (null == cameraDevice) {
            Log.e(TAG, "Failed to update the preview error, camera device is null.");
            return;
        }
        if (autoFocus) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, null);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        } else {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, focusArea);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

        }

        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "<- camera preview update");
    }

    /** Take the picture from camera device */
    private void takePicture() {
        Log.e(TAG, "-> still picture take");

        if (null == cameraDevice) {
            Log.e(TAG, "Failed to take the picture, camera device is null.");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        assert manager != null;

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (sensorOrientation == null) {
                sensorOrientation = 90;
            }

            SortedSet<OrderedSize> imageSizes = new TreeSet<>();
            for (Size size : map.getOutputSizes(ImageFormat.YUV_420_888))
            {
                OrderedSize s = new OrderedSize(size.getWidth(), size.getHeight());
                imageSizes.add(s);
            }


            ImageReader reader;
            // The largest image size for high quality
            int width = imageSizes.last().getWidth();
            int height = imageSizes.last().getHeight();
            reader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2);


            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());

            // Best quality for JPEG format
//            if (x == 1) {
//                captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
//            }

            // For auto-focus
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            // For zoomed picture taking
            captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, cameraZoom.getCropRect());
            // For long exposure time
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF); // For long exposure time, this is needed to disabled the default value
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long) 1e9 / 25);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            rotation = getOrientation(sensorOrientation, rotation);
            //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            imageFile = getOutputImageFile();
            assert imageFile != null;
            ImageReader.OnImageAvailableListener readerListener;
            int finalRotation = rotation;
            readerListener = new ImageReader.OnImageAvailableListener()
            {

                @Override
                public void onImageAvailable(ImageReader reader)
                {

                    try (Image image = reader.acquireLatestImage()) {
                        final ByteBuffer yuvBytes = this.imageToByteBuffer(image);

                        // Convert YUV to RGB
                        final RenderScript rs = RenderScript.create(MainActivity.this);
                        final Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                        final Allocation allocationRgb = Allocation.createFromBitmap(rs, bitmap);

                        final Allocation allocationYuv = Allocation.createSized(rs, Element.U8(rs), yuvBytes.array().length);
                        allocationYuv.copyFrom(yuvBytes.array());

                        ScriptIntrinsicYuvToRGB scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
                        scriptYuvToRgb.setInput(allocationYuv);
                        scriptYuvToRgb.forEach(allocationRgb);

                        allocationRgb.copyTo(bitmap);
                        save(bitmap);
                        notifyToGallery();

                        // Release
                        bitmap.recycle();
                        allocationYuv.destroy();
                        allocationRgb.destroy();
                        rs.destroy();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                /**
                 * Notify system to scan new media file
                 * This will prevent from showing image on gallery only when restarted.
                 */
                private void notifyToGallery() {
                    Intent i = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    i.setData(Uri.fromFile(imageFile));
                    sendBroadcast(i);
                }

                /* Convert the YUV image to ARGB bitmap */
                private ByteBuffer imageToByteBuffer(final Image image) {
                    final Rect crop = image.getCropRect();
                    final int width = crop.width();
                    final int height = crop.height();

                    final Image.Plane[] planes = image.getPlanes();
                    final byte[] rowData = new byte[planes[0].getRowStride()];
                    final int bufferSize = width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                    final ByteBuffer output = ByteBuffer.allocateDirect(bufferSize);

                    int channelOffset = 0;
                    int outputStride = 0;

                    for (int planeIndex = 0; planeIndex < 3; planeIndex++) {
                        if (planeIndex == 0) {
                            channelOffset = 0;
                            outputStride = 1;
                        } else if (planeIndex == 1) {
                            channelOffset = width * height + 1;
                            outputStride = 2;
                        } else if (planeIndex == 2) {
                            channelOffset = width * height;
                            outputStride = 2;
                        }

                        final ByteBuffer buffer = planes[planeIndex].getBuffer();
                        final int rowStride = planes[planeIndex].getRowStride();
                        final int pixelStride = planes[planeIndex].getPixelStride();

                        final int shift = (planeIndex == 0) ? 0 : 1;
                        final int widthShifted = width >> shift;
                        final int heightShifted = height >> shift;

                        buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));

                        for (int row = 0; row < heightShifted; row++) {
                            final int length;

                            if (pixelStride == 1 && outputStride == 1) {
                                length = widthShifted;
                                buffer.get(output.array(), channelOffset, length);
                                channelOffset += length;
                            } else {
                                length = (widthShifted - 1) * pixelStride + 1;
                                buffer.get(rowData, 0, length);

                                for (int col = 0; col < widthShifted; col++) {
                                    output.array()[channelOffset] = rowData[col * pixelStride];
                                    channelOffset += outputStride;
                                }
                            }

                            if (row < heightShifted - 1) {
                                buffer.position(buffer.position() + rowStride - length);
                            }
                        }
                    }

                    return output;
                }

                /* Save the image with PNG format */
                private void save(Bitmap originalBitmap) throws IOException
                {
                    OutputStream output = null;
                    try {
                        Bitmap rotatedBitmap = rotateImage(originalBitmap, finalRotation);
                        output = new FileOutputStream(imageFile);
                        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                        output.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }

                /* Rotate the image by degree */
                private Bitmap rotateImage(Bitmap source, float degree) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degree);
                    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
                }
            };

            reader.setOnImageAvailableListener(readerListener, backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Boast.makeText(MainActivity.this, "Saved:" + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    Uri file = Uri.fromFile(new File(imageFile.getAbsolutePath()));
                    StorageReference riversRef = storageReference.child(file.getLastPathSegment());
                    uploadTask = riversRef.putFile(file);
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "<- still picture take");
    }

    /** Show the phone gallery */
    private void showGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    /** Zoom in the preview */
    private void zoomIn() {
        zoomFactor += zoomStep;
        if (zoomFactor >= cameraZoom.maxZoom) {
            zoomFactor = cameraZoom.maxZoom;
        }

        cameraZoom.setZoom(captureRequestBuilder, zoomFactor);
        updatePreview();
        Boast.makeText(MainActivity.this, "Zoom In: " + zoomFactor, Toast.LENGTH_SHORT).show();
    }

    /** Zoom out the preview */
    protected void zoomOut() {
        zoomFactor -= zoomStep;
        if (zoomFactor <= CameraZoom.DEFAULT_ZOOM_FACTOR) {
            zoomFactor = CameraZoom.DEFAULT_ZOOM_FACTOR;
        }

        cameraZoom.setZoom(captureRequestBuilder, zoomFactor);
        updatePreview();
        Boast.makeText(MainActivity.this, "Zoom Out: " + zoomFactor, Toast.LENGTH_SHORT).show();
    }

    /** Retrieve the output image file */
    private static File getOutputImageFile()
    {
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyPictures", "Failed to create directory"); //if there is no directory, this is the error
                return null;
            }
        }
        // Create a image file name
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".png");
    }
}
