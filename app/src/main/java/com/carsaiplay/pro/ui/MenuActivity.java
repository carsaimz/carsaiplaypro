package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.carsaiplay.pro.R;

public class MenuActivity extends AppCompatActivity {

    private static final String TAG = "MenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Obtém a referência do TextView da versão
        TextView tvVersion = findViewById(R.id.tv_version);

        // Carrega a versão do aplicativo dinamicamente
        String appVersion = getAppVersion();
        if (appVersion != null) {
            tvVersion.setText(getString(R.string.app_version, appVersion)); // Usa string formatada
        } else {
            tvVersion.setText(R.string.version_unknown); // Exibe uma mensagem de erro
        }

        // Configura os listeners dos botões usando lambda
        Button btnAbout = findViewById(R.id.btn_about);
        Button btnSupport = findViewById(R.id.btn_support);
        Button btnAccount = findViewById(R.id.btn_account);
        Button btnClose = findViewById(R.id.btn_close_menu);

        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        btnSupport.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, SupportActivity.class);
            startActivity(intent);
        });

        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> finish()); // Fecha a MenuActivity
    }

    private String getAppVersion() {
        try {
            // Obtém as informações do pacote do aplicativo
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName; // Retorna a versão do aplicativo
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Erro ao obter a versão do aplicativo", e);
            Toast.makeText(this, R.string.version_error, Toast.LENGTH_SHORT).show();
            return null; // Retorna null em caso de erro
        }
    }
}