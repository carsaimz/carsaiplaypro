package com.carsaiplay.pro.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.carsaiplay.pro.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Obtém a referência do TextView da versão
        TextView textViewVersion = findViewById(R.id.textViewVersion);

        // Carrega a versão do aplicativo dinamicamente e exibe no TextView
        textViewVersion.setText("Versão " + getAppVersion());
    }

    // Método para obter a versão do aplicativo
    private String getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName; // Retorna a versão do aplicativo
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "1.0.0"; // Retorna uma versão padrão em caso de erro
        }
    }
}