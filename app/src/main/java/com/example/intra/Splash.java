package com.example.intra;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        setContentView(R.layout.activity_splash);

        // Animate logo with pop-in effect
        ImageView logo = findViewById(R.id.imageView2);
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);
        logo.startAnimation(popIn);

        // Typewriter animation
        TypeWriter typeWriter = findViewById(R.id.textView);
        typeWriter.setCharacterDelay(75);
        typeWriter.animateText("Talk Smart. Talk Intra.");

        // Launch next activity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(Splash.this, LoginActivity.class));
            finish();
        }, 4000);
    }
}
