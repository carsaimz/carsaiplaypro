package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.carsaiplay.pro.R;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // Encontrar o botão no layout
        Button btnAvancar = findViewById(R.id.btnAvancar);

        // Quando clicar, vai para a tela de login
        btnAvancar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(InfoActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // Fecha esta tela para não voltar
                    }
                });
    }
}
