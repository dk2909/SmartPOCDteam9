package com.team9.smartpocd;

import android.content.Intent;
import android.os.Bundle;

//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;

public class ImageProcessActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // Initialize the widgets
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_process);
        Button imageFunctionButton = findViewById(R.id.camera_dummy_button);
        assert imageFunctionButton != null;
        imageFunctionButton.setOnClickListener(v -> cameraFunction());

        Button ButtonTutorial = findViewById(R.id.tutorial);
        assert ButtonTutorial != null;
        ButtonTutorial.setOnClickListener(v -> tutorialFunction());
    }
    private void cameraFunction()
    {
        Intent switchToImProcIntent = new Intent(ImageProcessActivity.this, MainActivity.class);
        startActivity(switchToImProcIntent);
    }
    private void tutorialFunction()
    {
        Intent switchToImProcIntent2 = new Intent(ImageProcessActivity.this, sampling_tutorial.class);
        startActivity(switchToImProcIntent2);
    }
}