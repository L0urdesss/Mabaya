package com.example.mabaya;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.IOException;

public class ImageDescriptionSimple extends Fullscreen {

    ImageView imageView;
    Button retakeButton;
    TextView confidenceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_description);

        imageView = findViewById(R.id.imageView);
        retakeButton = findViewById(R.id.retake);
        confidenceTextView = findViewById(R.id.confidenceTextView);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("imagePath")) {
                String imagePath = intent.getStringExtra("imagePath");
                Log.d("ImageDescriptionSimple", "Image path received: " + imagePath);
                
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        confidenceTextView.setText("Image loaded successfully from camera capture!\nPath: " + imagePath);
                        Log.d("ImageDescriptionSimple", "Image loaded successfully");
                    } else {
                        confidenceTextView.setText("Failed to load image from path: " + imagePath);
                        Log.e("ImageDescriptionSimple", "Failed to decode image");
                    }
                } catch (Exception e) {
                    confidenceTextView.setText("Error loading image: " + e.getMessage());
                    Log.e("ImageDescriptionSimple", "Exception: " + e.getMessage());
                    e.printStackTrace();
                }
                
            } else if (intent.hasExtra("imageUri")) {
                String imageUriString = intent.getStringExtra("imageUri");
                Uri imageUri = Uri.parse(imageUriString);
                
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
                    confidenceTextView.setText("Image loaded successfully from gallery!\nURI: " + imageUriString);
                    Log.d("ImageDescriptionSimple", "Image loaded from URI successfully");
                } catch (IOException e) {
                    confidenceTextView.setText("Error loading image from URI: " + e.getMessage());
                    Log.e("ImageDescriptionSimple", "IOException: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Simply finish this activity to go back to CamAct
                finish();
            }
        });
    }
}