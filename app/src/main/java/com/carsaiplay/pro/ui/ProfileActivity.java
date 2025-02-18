package com.carsaiplay.pro.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.carsaiplay.pro.R;
import org.json.JSONException;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName, profileEmail;
    private Button btnEditProfile, btnLogout;
    private SharedPreferences prefs;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);

        prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);

        // Recupera o ID do usuário salvo no SharedPreferences
        String userId = prefs.getString("userId", null);

        if (userId != null) {
            // Faz a requisição para obter os dados do usuário da MockAPI
            carregarDadosDoUsuario(userId);
        } else {
            // Se não houver ID, exibe uma mensagem de erro
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show();
            finish(); // Fecha a atividade
        }

        // Abrir tela de edição
        btnEditProfile.setOnClickListener(
                v -> {
                    Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                    startActivity(intent);
                });

        // Botão de Logout
        btnLogout.setOnClickListener(
                v -> {
                    logout();
                });
    }

    private void carregarDadosDoUsuario(String userId) {
        String apiUrl = "https://67a30b1e409de5ed525729ab.mockapi.io/carsaiplay/Users/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Extrai os dados do usuário da resposta
                            String name = response.getString("name");
                            String email = response.getString("email");

                            // Atualiza a interface
                            profileName.setText(name);
                            profileEmail.setText(email);

                            // Salva os dados no SharedPreferences
                            salvarDadosDoUsuario(name, email);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(ProfileActivity.this, "Erro ao processar dados do usuário.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(ProfileActivity.this, "Erro ao carregar dados do usuário.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }

    private void salvarDadosDoUsuario(String name, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("name", name);
        editor.putString("email", email);
        editor.apply();
    }

    private void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Remove os dados do usuário
        editor.apply();

        // Redireciona para a tela de login
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        Toast.makeText(this, "Você saiu da conta.", Toast.LENGTH_SHORT).show();
    }
}