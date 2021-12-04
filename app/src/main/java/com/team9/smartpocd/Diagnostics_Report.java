package com.team9.smartpocd;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;
import android.widget.RelativeLayout;
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

        download();

        EditText tde = (EditText)findViewById(R.id.editTextTextPersonName);
        try {
            String responseJson = readJSONfile();
            tde.setText(responseJson);
            //Boast.makeText(Diagnostics_Report.this, "Result: " + responseJson, Toast.LENGTH_SHORT).show();
            System.out.println(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        }

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


        System.out.println("Path: " + destinationDirectory );

        downloadManager.enqueue(request);
    }

    public String readJSONfile() throws IOException {

        Context context = Diagnostics_Report.this;
        File jsonFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "IMG_20211203_170516.json");

        FileReader jsonFileReader = new FileReader(jsonFile);
        BufferedReader bufferedReader = new BufferedReader(jsonFileReader);
        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        stringBuilder.append(line);
        String jsonContent = stringBuilder.toString();

        return  jsonContent;
    }

}

