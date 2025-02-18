package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.carsaiplay.pro.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 3000; // 3 segundos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay de 3 segundos antes de abrir a Tela de Informações
        new Handler()
                .postDelayed(
                        () -> {
                            Intent intent = new Intent(SplashActivity.this, InfoActivity.class);
                            startActivity(intent);
                            finish(); // Fecha a SplashScreen para não voltar
                        },
                        SPLASH_TIME);
    }
}
