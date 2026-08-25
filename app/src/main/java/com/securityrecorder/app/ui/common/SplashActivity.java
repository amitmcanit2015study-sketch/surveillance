package com.securityrecorder.app.ui.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import androidx.appcompat.app.AppCompatActivity;
import com.securityrecorder.app.R;
import com.securityrecorder.app.ui.main.MainActivity;

/**
 * Splash Screen displaying brand identity and "Developed by Amit Bharat" footer
 * before smoothly transitioning into MainActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1500L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Keep system bars immersive and stylized
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xFF1E3A8A);

        animateContent();

        handler.postDelayed(this::navigateToMain, SPLASH_DURATION_MS);
    }

    private void animateContent() {
        View brandView = findViewById(R.id.layoutCenterBrand);
        View developerView = findViewById(R.id.tvSplashDeveloper);

        if (brandView != null) {
            AnimationSet set = new AnimationSet(true);
            AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
            fade.setDuration(700);
            ScaleAnimation scale = new ScaleAnimation(
                    0.85f, 1.0f, 0.85f, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            scale.setDuration(700);
            set.addAnimation(fade);
            set.addAnimation(scale);
            brandView.startAnimation(set);
        }

        if (developerView != null) {
            AlphaAnimation fade = new AlphaAnimation(0.0f, 1.0f);
            fade.setDuration(900);
            developerView.startAnimation(fade);
        }
    }

    private void navigateToMain() {
        if (isNavigated || isFinishing() || isDestroyed()) return;
        isNavigated = true;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
