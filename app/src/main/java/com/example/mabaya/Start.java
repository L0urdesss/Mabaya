package com.example.mabaya;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Start extends Fullscreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

       
        Button getStartedButton = findViewById(R.id.button);

        
        getStartedButton.setOnClickListener(v -> {
            // Create an Intent to start the Choose activity
            Intent intent = new Intent(Start.this, CamAct.class);

            
            startActivity(intent);
        });
    }
}