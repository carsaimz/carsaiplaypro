package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.carsaiplay.pro.R;
import androidx.appcompat.app.AppCompatActivity;
import com.carsaiplay.pro.ui.WebViewActivity;

public class LoadingActivity extends AppCompatActivity {

    private static final int TEMPO_CARREGAMENTO =
            3000; // Tempo da tela de carregamento (3 segundos)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ProgressBar progressBar = findViewById(R.id.progressBar);
        TextView txtMensagem = findViewById(R.id.txtMensagem);

        txtMensagem.setText("Carregando, Aguarde por favor...");

        String url = getIntent().getStringExtra("siteUrl");

        new Handler()
                .postDelayed(
                        () -> {
                            Intent intent = new Intent(LoadingActivity.this, WebViewActivity.class);
                            intent.putExtra("siteUrl", url);
                            startActivity(intent);
                            finish();
                        },
                        TEMPO_CARREGAMENTO);
    }
}
