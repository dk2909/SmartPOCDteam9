package com.team9.smartpocd;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

//import android.support.v4.content.FileProvider;
//import android.support.v7.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GalleryActivity Class
 */
public class GalleryActivity extends AppCompatActivity {
    /** Properties */
    /* List of image paths */
    private final List<String> imagePaths = new ArrayList<>();
    /* Adapter for image file */
    private ImageFileAdapter imageFileAdapter;

    /** onCreate() method */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_gallery);
        imageFileAdapter = new ImageFileAdapter(imagePaths);
        GridView galleryView = findViewById(R.id.gallery);
        galleryView.setAdapter(imageFileAdapter);
        galleryView.setOnItemClickListener(((parent, view, position, id) -> {
            String imagePath = imageFileAdapter.getItem(position);
            showImage(imagePath);
        }));
    }

    /** onResume() override */
    @Override
    protected void onResume() {
        super.onResume();

        File home = new File(MainActivity.mediaStorageDir.getPath());
        if (home.isDirectory()) {
            imagePaths.clear();
            File[] files = home.listFiles();
            if (files == null) {
                return;
            }

            for (File file : files) {
                imagePaths.add(file.getAbsolutePath());
            }

            imageFileAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Show a image by specified path
     */
    private void showImage(String imagePath) {
        File file = new File(imagePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uriForFile = FileProvider.getUriForFile(GalleryActivity.this, getApplicationContext().getPackageName() + ".provider", file);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uriForFile = Uri.fromFile(file);
        }
        intent.setDataAndType(uriForFile, "image/png");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;

        if (isIntentSafe) {
            startActivity(intent);
        } else {
            Toast.makeText(GalleryActivity.this, "No media viewer found", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method is to scale down the image
     */
    public Bitmap decodeURI(String filePath){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        BitmapFactory.decodeFile(filePath, options);

        // Only scale if we need to
        // (16384 buffer for img processing)
        boolean scaleByHeight = Math.abs(options.outHeight - 100) >= Math.abs(options.outWidth - 100);
        if(options.outHeight * options.outWidth * 2 >= 16384){
            // Load, scaling to smallest power of 2 that'll get it <= desired dimensions
            double sampleSize = scaleByHeight ? options.outHeight / 100f : options.outWidth / 100f;
            options.inSampleSize = (int) Math.pow(2d, Math.floor(Math.log(sampleSize) / Math.log(2d)));
        }

        // Do the actual decoding
        options.inJustDecodeBounds = false;
        options.inTempStorage = new byte[512];
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * This class loads the image gallery in grid view.
     *
     */
    public class ImageFileAdapter extends BaseAdapter {
        private final List<String> imagePaths;

        /** Constructor */
        public ImageFileAdapter(List<String> imagePaths) {
            this.imagePaths = imagePaths;
        }

        public int getCount() {
            return imagePaths.size();
        }

        public String getItem(int position) {
            return imagePaths.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        /** create a new ImageView for each item referenced by the Adapter */
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_image, parent, false);
            }

            imageView = convertView.findViewById(R.id.item_image);
            try {
                Bitmap bmp = decodeURI(imagePaths.get(position));
                imageView.setImageBitmap(bmp);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }
}
