package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.carsaiplay.pro.R;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SupportActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        Button btnWhatsApp = findViewById(R.id.btn_whatsapp);
        btnWhatsApp.setOnClickListener(v -> openWhatsApp());
    }

    private void openWhatsApp() {
        String phoneNumber = "+258862414345"; // Número do suporte no WhatsApp
        String message = "Olá, preciso de suporte para o app.";
        String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}