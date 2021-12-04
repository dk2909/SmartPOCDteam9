package com.team9.smartpocd;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Diagnostics_Report extends AppCompatActivity {

    //TextView resultTextView = (resultTextView);

    // Firebase server object
    private FirebaseStorage storage;
    // Reference to access server
    private StorageReference storageReference;
    // widget to download files from server
    Button downlink;
    // reference directly to file on server
    StorageReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics_report);
        EditText tde = (EditText)findViewById(R.id.editTextTextPersonName);
        tde.setText("test5");


        download();


    }

    // get handle on server storage
    // storage = FirebaseStorage.getInstance();
    // storageReference = storage.getReference();
    public void download() {
        storageReference = storage.getInstance().getReference();
        // reference directly to file on server
        ref = storageReference.child("responses/IMG_20211203_170516.json");
        // static URL needed if using DownloadManager
        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                String url = uri.toString();
                downloadFile(Diagnostics_Report.this, "IMG_20211203_170516", ".json", DIRECTORY_DOWNLOADS, url);
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
        request.setDestinationInExternalFilesDir(context, destinationDirectory, fileName + fileExtension);
        downloadManager.enqueue(request);
    }
}

