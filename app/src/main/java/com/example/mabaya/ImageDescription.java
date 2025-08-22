package com.example.mabaya;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ImageDescription extends Fullscreen {

    ImageView imageView;
    Button retakeButton;
    TextView confidenceTextView;
    TextView descriptionTextView;
    Bitmap bitmap;
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    Paint boxPaint = new Paint();
    Paint textPaint = new Paint();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_description);

        imageView = findViewById(R.id.imageView);
        retakeButton = findViewById(R.id.retake);
        confidenceTextView = findViewById(R.id.confidenceTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("imagePath")) {
                String imagePath = intent.getStringExtra("imagePath");
                Log.d("ImageDescription", "Image path received: " + imagePath);
                
                try {
                    // Load image with proper orientation handling to fix rotation
                    Bitmap myBitmap = decodeFileWithOrientation(imagePath);
                    if (myBitmap != null) {
                        // Scale down the bitmap if it's too large to prevent memory issues
                        int maxSize = 1024;
                        if (myBitmap.getWidth() > maxSize || myBitmap.getHeight() > maxSize) {
                            float scale = Math.min(
                                (float) maxSize / myBitmap.getWidth(),
                                (float) maxSize / myBitmap.getHeight()
                            );
                            int newWidth = Math.round(myBitmap.getWidth() * scale);
                            int newHeight = Math.round(myBitmap.getHeight() * scale);
                            myBitmap = Bitmap.createScaledBitmap(myBitmap, newWidth, newHeight, true);
                        }
                        
                        imageView.setImageBitmap(myBitmap);
                        bitmap = myBitmap;
                        Log.d("ImageDescription", "Bitmap loaded successfully, size: " + myBitmap.getWidth() + "x" + myBitmap.getHeight());
                        
                        // Try YOLO processing in a separate thread to avoid blocking UI
                        new Thread(() -> {
                            try {
                                initYolov5();
                                ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(bitmap);
                                
                                runOnUiThread(() -> {
                                    try {
                                        displayResults(recognitions);
                                    } catch (Exception e) {
                                        Log.e("ImageDescription", "Error displaying results: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("ImageDescription", "Error in YOLO processing: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }).start();
                        
                    } else {
                        Log.e("ImageDescription", "Failed to decode image from path: " + imagePath);
                    }
                } catch (Exception e) {
                    Log.e("ImageDescription", "Exception loading image: " + e.getMessage());
                    e.printStackTrace();
                }
                
            } else if (intent.hasExtra("imageUri")) {
                String imageUriString = intent.getStringExtra("imageUri");
                Uri imageUri = Uri.parse(imageUriString);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
                    
                    // Try YOLO processing in a separate thread
                    new Thread(() -> {
                        try {
                            initYolov5();
                            ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(bitmap);
                            
                            runOnUiThread(() -> {
                                try {
                                    displayResults(recognitions);
                                } catch (Exception e) {
                                    Log.e("ImageDescription", "Error displaying results: " + e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            Log.e("ImageDescription", "Error in YOLO processing: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                    
                } catch (IOException e) {
                    Log.e("ImageDescription", "Error loading image from URI: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        retakeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to CamAct and clear the activity stack
                Intent retakeIntent = new Intent(ImageDescription.this, CamAct.class);
                retakeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(retakeIntent);
                finish(); // Close the current activity
            }
        });
    }
    
    private Bitmap decodeFileWithOrientation(String imagePath) {
        try {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;

                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                if (bitmap != null) {
                    ExifInterface exif = new ExifInterface(imagePath);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    int rotationInDegrees = exifToDegrees(orientation);
                    if (rotationInDegrees != 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotationInDegrees);

                        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }

                    return bitmap;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int exifToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    private void initYolov5() {
        yolov5TFLiteDetector = new Yolov5TFLiteDetector();
        yolov5TFLiteDetector.setModelFile("best-fp16.tflite");
        yolov5TFLiteDetector.initialModel(this);

        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);

        textPaint.setTextSize(50);
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);
    }

    private void displayResults(ArrayList<Recognition> recognitions) {
        if (bitmap != null && recognitions != null) {
            Log.d("Recognitions", "Number of Recognitions: " + recognitions.size());

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            StringBuilder resultText = new StringBuilder();
            Map<String, Double> classConfidences = new HashMap<>();
            Map<String, Integer> classCounts = new HashMap<>();
            boolean hasValidDetection = false;

            // Higher confidence threshold for reliable classification
            double CONFIDENCE_THRESHOLD = 0.7; // Increased from 0.4 to 0.7

            for (Recognition recognition : recognitions) {
                String className = recognition.getLabelName();
                double confidence = recognition.getConfidence();

                if (confidence > 0.4) { // Still draw boxes for lower confidence
                    RectF location = recognition.getLocation();
                    canvas.drawRect(location, boxPaint);

                    // Only consider it a valid detection if confidence is high enough
                    if (confidence > CONFIDENCE_THRESHOLD) {
                        classConfidences.put(className, classConfidences.getOrDefault(className, 0.0) + confidence);
                        classCounts.put(className, classCounts.getOrDefault(className, 0) + 1);
                        hasValidDetection = true;

                        String labelText = className + ": " + String.format("%.2f", confidence) + "%";
                        canvas.drawText(labelText, location.left, location.top - 10, textPaint);
                    } else {
                        // Draw "Unknown" for low confidence detections
                        String labelText = "Unknown: " + String.format("%.2f", confidence) + "%";
                        canvas.drawText(labelText, location.left, location.top - 10, textPaint);
                    }
                }
            }

            // Show results based on confidence
            StringBuilder flowerNameText = new StringBuilder();
            StringBuilder descriptionText = new StringBuilder();
            
            if (hasValidDetection) {
                // Show flower names and descriptions for high confidence detections
                for (String className : classConfidences.keySet()) {
                    double totalConfidence = classConfidences.get(className);
                    int count = classCounts.get(className);
                    double averageConfidence = totalConfidence / count;

                    // Flower name (26sp - set in XML)
                    flowerNameText.append(className);
                    if (classConfidences.size() > 1) {
                        flowerNameText.append(", ");
                    }
                    
                    // Add detailed description (18sp - set in XML)
                    String description = getFlowerDescription(className);
                    if (!description.isEmpty()) {
                        descriptionText.append(description);
                        if (classConfidences.size() > 1) {
                            descriptionText.append("\n\n");
                        }
                    }
                }
            } else {
                // No high confidence detections - show unknown flower message
                flowerNameText.append("Unknown Flower");
                descriptionText.append("This flower is not in our trained database. Our model can currently identify: Rose, Tulip, Dandelion, Sampaguita, and Sunflower.\n\n");
                descriptionText.append("The detected flower may be a different species or variety not included in our training data.");
            }

            // Set text to separate TextViews
            confidenceTextView.setText(flowerNameText.toString());
            descriptionTextView.setText(descriptionText.toString());
            imageView.setImageBitmap(mutableBitmap);
        }
    }
    
    
    private String getFlowerDescription(String flowerName) {
        switch (flowerName.toLowerCase()) {
            case "rose":
                return "Rose – Commonly grown in Philippine gardens and flower farms (like in Benguet). Popular for bouquets and ornamental use.";
            case "tulip":
            case "tulips":
                return "Tulips – Not native to the Philippines but grown in cool highland climates like Benguet's flower farms, especially during the Panagbenga Festival in Baguio.";
            case "dandelion":
                return "Dandelion – Considered a weed in many countries, but also found in the Philippines. Known for its yellow flowers and fluffy seed heads kids like to blow.";
            case "sampaguita":
                return "Sampaguita – The national flower of the Philippines. Small white blossoms with a strong fragrance, often used in garlands and religious offerings.";
            case "sunflower":
                return "Sunflower – Grown in various Philippine provinces for ornamental use. Sunflowers naturally turn toward sunlight (heliotropism) and are also used for seeds and oil.";
            default:
                return "";
        }
    }

    private void predict() {
        if (bitmap != null) {
            ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(bitmap);
            displayResults(recognitions);
        }
    }
}