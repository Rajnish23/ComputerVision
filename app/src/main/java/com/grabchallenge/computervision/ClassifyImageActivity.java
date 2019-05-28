package com.grabchallenge.computervision;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.common.modeldownload.FirebaseRemoteModel;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class ClassifyImageActivity extends AppCompatActivity {

    private static final String TAG = "ClassifyImageActivity";
    private ImageView mImageView;
    private TextView mResult;
    private ProgressBar mProgress;
    private String mPath;
    private Uri imageUri;
    private FirebaseVisionImage visionImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify_image);

        mImageView = findViewById(R.id.img);
        mResult = findViewById(R.id.label_text);
        mProgress = findViewById(R.id.progressBar);

        if (getIntent() != null) {
            mPath = getIntent().getStringExtra("imagePath");

            if(mPath == null){
                imageUri = Uri.parse(getIntent().getStringExtra("uri"));
            }
        }
        FirebaseApp.initializeApp(this);

        configHostedModel();

    }

    private void configHostedModel() {
        FirebaseModelDownloadConditions downloadConditions = new FirebaseModelDownloadConditions
                .Builder()
                .requireWifi()
                .build();

        //Remote Model
        FirebaseRemoteModel remoteModel = new FirebaseRemoteModel.Builder("Car_Dataset")
                .enableModelUpdates(true)
                .setInitialDownloadConditions(downloadConditions)
                .setUpdatesDownloadConditions(downloadConditions)
                .build();

        //Local Model
        FirebaseLocalModel localModel = new FirebaseLocalModel.Builder("Car_Dataset")
                .setAssetFilePath("manifest.json")
                .build();

        FirebaseModelManager.getInstance().registerLocalModel(localModel);
        FirebaseModelManager.getInstance().registerRemoteModel(remoteModel);

        //prepareImage for input

        try {
            Bitmap bitmap = getThumbnail();
            mImageView.setImageBitmap(bitmap);
            visionImage = FirebaseVisionImage.fromBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //imageLaberer setup with remote and local model
        FirebaseVisionOnDeviceAutoMLImageLabelerOptions labelerOptions =
                new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
                .setRemoteModelName("Car_Dataset")
                .setLocalModelName("Car_Dataset")
                .setConfidenceThreshold(0.6f)       //change as per requirement
                .build();

        try {
            FirebaseVisionImageLabeler labeler =
                    FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(labelerOptions);


            // To track the download and get notified when the download completes, call

            // downloadRemoteModelIfNeeded. Note that if you don't call downloadRemoteModelIfNeeded, the model

            // downloading is still triggered implicitly.

            FirebaseModelManager.getInstance().downloadRemoteModelIfNeeded(remoteModel).addOnCompleteListener(

                    new OnCompleteListener<Void>() {

                        @Override

                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {

                                Toast.makeText(ClassifyImageActivity.this, "Download remote AutoML model success.", Toast.LENGTH_SHORT)
                                        .show();

                            } else {

                                String downloadingError = "Error downloading remote model.";

                                Log.e(TAG, downloadingError, task.getException());

                                Toast.makeText(ClassifyImageActivity.this, downloadingError, Toast.LENGTH_SHORT).show();

                            }

                        }

                    })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });

            /**
             * @visionImage Image to process and label the model name with confidence score.
             */
            labeler.processImage(visionImage)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            Log.i(TAG, "onSuccess: "+labels);
                            mProgress.setVisibility(View.GONE);

                            if(labels != null && labels.size()>0) {
                                for (FirebaseVisionImageLabel label : labels) {
                                    String text = label.getText();
                                    float confidence = label.getConfidence();

                                    mResult.append("Model " + text + "\n " + confidence + " Confidence");
                                }
                            }
                            else{
                                mResult.setText("No Result Found, Try Again!");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mProgress.setVisibility(View.GONE);
                            e.printStackTrace();
                        }
                    });
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
    }


    public Bitmap getThumbnail() throws  IOException {

        if(mPath != null) {
            InputStream input = new FileInputStream(mPath);
            BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
            onlyBoundsOptions.inJustDecodeBounds = true;
            onlyBoundsOptions.inDither = true;//optional
            onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
            BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
            input.close();
            if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
                return null;
            }
            int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;
            int THUMBNAIL_SIZE = 750;
            double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
            bitmapOptions.inDither = true; //optional
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//
            input = new FileInputStream(mPath);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
            input.close();
            return bitmap;
        }
        else{
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
            int dstHeight = (int) (bitmap.getHeight() * (512.0 / bitmap.getWidth()));
            bitmap = Bitmap.createScaledBitmap(bitmap,512,dstHeight,true);
            return bitmap;
        }

    }



    private static int getPowerOfTwoForSampleRatio(double ratio){

        int k = Integer.highestOneBit((int)Math.floor(ratio));

        if(k==0) return 1;

        else return k;

    }

}
