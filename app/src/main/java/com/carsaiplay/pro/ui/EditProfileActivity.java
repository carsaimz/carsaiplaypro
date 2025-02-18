package com.carsaiplay.pro.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editSenha;
    private Button btnSave;
    private SharedPreferences prefs;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        editName = findViewById(R.id.edit_name);
        editSenha = findViewById(R.id.edit_senha);
        btnSave = findViewById(R.id.btn_save);

        prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);

        // Preencher o campo com o nome atual
        editName.setText(prefs.getString("name", ""));

        btnSave.setOnClickListener(
                v -> {
                    String newName = editName.getText().toString();
                    String newSenha = editSenha.getText().toString();

                    // Validação dos campos
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "O nome não pode estar vazio.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Atualiza os dados no MockAPI
                    atualizarDadosNoMockAPI(newName, newSenha);
                });
    }

    private void atualizarDadosNoMockAPI(String newName, String newSenha) {
        // Recupera o ID do usuário salvo no SharedPreferences
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(this, "Usuário não encontrado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // URL da MockAPI para atualizar o usuário
        String apiUrl = "https://67a30b1e409de5ed525729ab.mockapi.io/carsaiplay/Users/" + userId;

        // Cria o corpo da requisição (JSON)
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", newName);
            if (!newSenha.isEmpty()) {
                requestBody.put("senha", newSenha);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao criar requisição.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Faz a requisição PUT para atualizar os dados
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                apiUrl,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Atualiza os dados no SharedPreferences
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("name", newName);
                            if (!newSenha.isEmpty()) {
                                editor.putString("senha", newSenha);
                            }
                            editor.apply();

                            Toast.makeText(EditProfileActivity.this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                            finish(); // Fecha a atividade
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(EditProfileActivity.this, "Erro ao processar resposta.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(EditProfileActivity.this, "Erro ao atualizar perfil.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }
}