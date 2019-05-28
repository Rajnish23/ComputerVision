package com.grabchallenge.computervision;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    private CameraView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = findViewById(R.id.camera);

        //Check for Camera Permission and Storage
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 101){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }
            else{
                Snackbar.make(mCameraView,"Permission Required",Snackbar.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraView.stop();
    }

    public void takePicture(View view) {
        mCameraView.captureImage(new CameraKitEventCallback<CameraKitImage>() {
            @Override
            public void callback(final CameraKitImage cameraKitImage) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String path = saveToInternalStorage(cameraKitImage.getBitmap());
                        Intent mClassifyIntent = new Intent(CameraActivity.this, ClassifyImageActivity.class);
                        mClassifyIntent.putExtra("imagePath", path);
                        startActivity(mClassifyIntent);
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    }
                }).start();
            }
        });
    }

    private String saveToInternalStorage(Bitmap bitmap){
        ContextWrapper mWrapper = new ContextWrapper(getApplicationContext());
        File directory = mWrapper.getDir("imageClassDir",MODE_PRIVATE);
        File path = new File(directory,"image.jpg");
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return path.getAbsolutePath();
    }
}
