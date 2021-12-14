package com.team9.smartpocd;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
//import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.codekidlabs.storagechooser.StorageChooser;

public class Diagnostics_Report extends AppCompatActivity {

    // Firebase server object
    private FirebaseStorage storage;
    // Reference to access server
    private StorageReference storageReference;
    // widget to download files from server
    Button downlink;
    // reference directly to file on server
    StorageReference ref;
    // Directory to store results
    String downloadDir = "/AA_SMARTPOCD_RESULTS/";
    // text box to update report
    TextView textViewBox;

    //TextView diagnosticsTextView;
    public static final int FILEPICKER_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics_report);
        System.out.println("Diagnostics Report called");
        textViewBox = (TextView) findViewById(R.id.resultText);

        try {
            // get name of most recent image result (if new image taken in that session
            final Singleton singleton = (Singleton) getApplicationContext();
            final String fileName = singleton.getData();
            System.out.println(fileName);
            // download image
            download(fileName); // download latest result

        } catch (NullPointerException e){

        }

        // handle button to pick result
        Button filepickerBtn = findViewById(R.id.pick_result_button);

        filepickerBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            //On click function
            public void onClick(View view) {
                String[] PERMISSIONS = {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if(hasPermissions(Diagnostics_Report.this, PERMISSIONS)){
                    ShowFilepicker();
                }else{
                    ActivityCompat.requestPermissions(Diagnostics_Report.this, PERMISSIONS, FILEPICKER_PERMISSIONS);
                }
            }
        });
    }

    void print_result(String filePath){
        String responseJson = "";
        try {
            responseJson = readJSONfile(filePath);
            String fileName = filePath.substring(20);
            if(responseJson.contains("iat")){
                textViewBox.setText("Tumor is present.\n\n" + fileName + "\n\n" + responseJson);
            } else if (responseJson.contains("nat")){
                textViewBox.setText("Tumor is not present.\n\n" + fileName + "\n\n" + responseJson);
            }

            System.out.println(responseJson);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // get handle on server storage
    // storage = FirebaseStorage.getInstance();
    // storageReference = storage.getReference();
    public void download(String fileName) {
        storageReference = storage.getInstance().getReference();
        // reference directly to file on server
        ref = storageReference.child("responses/" + fileName);
        // static URL needed if using DownloadManager
        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();
               // downloadFile(Diagnostics_Report.this, fileName.substring(0, fileName.length()-5), ".json", DIRECTORY_DOWNLOADS, url);
                File myDir = Environment.getExternalStorageDirectory();
                String folder = myDir.getAbsolutePath() + "AA_SMARTPOCD_RESULT";
                downloadFile(Diagnostics_Report.this, fileName.substring(0, fileName.length()-5), ".json", folder, url);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    // context = context of program/activity
    public void downloadFile(Context context, String fileName, String fileExtension, String destinationDirectory, String url){
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        //request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension);
        request.setDestinationInExternalPublicDir("/AA_SMARTPOCD_RESULTS", fileName + fileExtension);

        System.out.println("Path: " + destinationDirectory );

        downloadManager.enqueue(request);
    }

    public String readJSONfile(String fileName) throws IOException {

        //Context context = Diagnostics_Report.this;
        //File jsonFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
        System.out.println("PATH of JSON: " + fileName);
        File jsonFile = new File(fileName);

        FileReader jsonFileReader = new FileReader(jsonFile);
        BufferedReader bufferedReader = new BufferedReader(jsonFileReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        stringBuilder.append(line);
        String jsonContent = stringBuilder.toString();

        return  jsonContent;
    }

    public void ShowFilepicker(){
        // 1. Initialize dialog
        final StorageChooser chooser = new StorageChooser.Builder()
                .withActivity(Diagnostics_Report.this)
                .withFragmentManager(getFragmentManager())
                .withMemoryBar(true)
                .allowCustomPath(true)
                .setType(StorageChooser.FILE_PICKER)
                .build();
        // 2. Retrieve the selected path by the user and show in a toast !
        chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
            @Override
            public void onSelect(String path) {
                print_result(path);
                Toast.makeText(Diagnostics_Report.this, "The selected path is : " + path, Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Display File Picker !
        chooser.show();
    }

    /**
     * Helper method that verifies whether the permissions of a given array are granted or not.
     *
     * @param context
     * @param permissions
     * @return {Boolean}
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Callback that handles the status of the permissions request.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FILEPICKER_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                            Diagnostics_Report.this,
                            "Permission granted! Please click on pick a file once again.",
                            Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                            Diagnostics_Report.this,
                            "Permission denied to read your External storage :(",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                return;
            }
        }
    }

}

