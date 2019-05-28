package com.grabchallenge.computervision;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ChoiceActivity extends AppCompatActivity {

    public static final int ACTION_REQUEST_GALLERY = 0;

    private static final String TAG = "ChoiceActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice);
    }

    public void cameraOption(View view) {

        Intent cameraIntent = new Intent(ChoiceActivity.this,CameraActivity.class);
        startActivity(cameraIntent);
    }

    public void uploadOption(View view) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        Intent chooser = Intent.createChooser(intent,"Choose a Picture");
        startActivityForResult(chooser,ACTION_REQUEST_GALLERY);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "onActivityResult: "+requestCode +"\t"+resultCode+"\t"+data);
        if(resultCode == RESULT_OK){
            if (requestCode == ACTION_REQUEST_GALLERY) {
                assert data != null;
                Uri imageUri = data.getData();
                Log.i(TAG, "onActivityResult: " + imageUri);
                Intent classifyIntent = new Intent(ChoiceActivity.this, ClassifyImageActivity.class);
                assert imageUri != null;
                classifyIntent.putExtra("uri", imageUri.toString());
                startActivity(classifyIntent);
            }
        }
    }
}
