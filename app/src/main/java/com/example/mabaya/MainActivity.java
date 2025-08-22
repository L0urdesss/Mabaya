package com.example.mabaya;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Fullscreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler handler = new Handler();
        handler.postDelayed(() -> {

            Intent startIntent = new Intent(MainActivity.this, Start.class);


            startActivity(startIntent);


            finish();
        }, 5000);
    }
}
