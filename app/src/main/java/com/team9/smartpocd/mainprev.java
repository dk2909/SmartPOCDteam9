package com.team9.smartpocd;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
// Firebase Libraries
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class mainprev extends AppCompatActivity {
    // Firebase server object
    private FirebaseStorage storage;
    // Reference to access server
    private StorageReference storageReference;
    // declare image process button
    private Button imageProcButton;
    // widget to download files from server
    // reference directly to file on server
    StorageReference ref;




    // get handle on server storage
    // storage = FirebaseStorage.getInstance();
    // storageReference = storage.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Initialize the widgets
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainprev);
        Button imageProcButton = findViewById(R.id.ip_button);
        assert imageProcButton != null;
        imageProcButton.setOnClickListener(v -> imageProcess());

        Button diagnosticReport = findViewById(R.id.diagnostics_reports_button);
        assert diagnosticReport != null;
        diagnosticReport.setOnClickListener(v -> diagnosticReportFunction());

    }
    private void imageProcess()
    {
        Intent switchToImProcIntent = new Intent(mainprev.this, ImageProcessActivity.class);
        startActivity(switchToImProcIntent);
    }
    private void diagnosticReportFunction()
    {
        Intent switchToImProcIntent2 = new Intent(mainprev.this, Diagnostics_Report.class);
        startActivity(switchToImProcIntent2);
    }

}
