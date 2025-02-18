package com.carsaiplay.pro.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.carsaiplay.pro.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private EditText etName, etSenha;
    private Button btnLogin, btnSolicitarConta, btnTabelaPrecos, btnEsqueciMinhaConta;
    private CheckBox checkBoxLembrar;
    private String apiUrl = "https://67a30b1e409de5ed525729ab.mockapi.io/carsaiplay/Users";
    private SharedPreferences prefs;
    private ProgressDialog progressDialog;
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics crashlytics;
    private ArrayList<JSONObject> listaSites = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inicializarFirebase();
        inicializarViews();
        configurarBotaoLogin();
        verificarLoginSalvo();
    }

    private void inicializarFirebase() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();
    }

    private void inicializarViews() {
        etName = findViewById(R.id.etName);
        etSenha = findViewById(R.id.etSenha);
        btnLogin = findViewById(R.id.btnLogin);
        btnSolicitarConta = findViewById(R.id.btnSolicitarConta);
        btnTabelaPrecos = findViewById(R.id.btnTabelaPrecos);
        btnEsqueciMinhaConta = findViewById(R.id.btnEsqueciMinhaConta);
        checkBoxLembrar = findViewById(R.id.checkBoxLembrar);
        prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        btnLogin.setBackgroundResource(R.drawable.botao_arredondado);
        btnLogin.setBackgroundTintList(
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));

        configurarBotoesAdicionais();
    }

    private void configurarBotaoLogin() {
        btnLogin.setOnClickListener(v -> {
            firebaseAnalytics.logEvent("login_attempt", null);
            mostrarCarregamento();
            validarLogin();
        });
    }

    private void configurarBotoesAdicionais() {
        btnSolicitarConta.setOnClickListener(v -> abrirWhatsAppNovaConta());
        btnTabelaPrecos.setOnClickListener(v -> mostrarTabelaPrecos());
        btnEsqueciMinhaConta.setOnClickListener(v -> abrirWhatsAppRecuperarConta());
    }

    private void verificarLoginSalvo() {
        if (prefs.getBoolean("lembrar_login", false)) {
            etName.setText(prefs.getString("saved_name", ""));
            etSenha.setText(prefs.getString("saved_senha", ""));
            checkBoxLembrar.setChecked(true);

            if (prefs.getBoolean("auto_login", false)) {
                validarLogin();
            }
        }
    }

    private void mostrarCarregamento() {
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Autenticando usuário...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void validarLogin() {
        String name = etName.getText().toString().trim();
        String senha = etSenha.getText().toString().trim();

        if (name.isEmpty() || senha.isEmpty()) {
            if (progressDialog != null) progressDialog.dismiss();
            Toast.makeText(this, "É necessário preencher usuário e senha!", Toast.LENGTH_SHORT).show();
            return;
        }

        realizarRequisicaoLogin(name, senha);
    }

    private void realizarRequisicaoLogin(final String name, final String senha) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                apiUrl,
                null,
                response -> processarRespostaLogin(response, name, senha),
                error -> tratarErroLogin(error));

        queue.add(request);
    }

    private void processarRespostaLogin(JSONArray response, String name, String senha) {
        listaSites.clear();
        final boolean[] nomeEncontrado = {false};
        final boolean[] senhaEncontrada = {false};
        final JSONObject[] userCorreto = {null};

        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject obj = response.getJSONObject(i);
                if (!obj.has("senha")) {
                    listaSites.add(obj);
                    continue;
                }
                
                if (obj.getString("name").equals(name)) {
                    nomeEncontrado[0] = true;
                    if (obj.getString("senha").equals(senha)) {
                        senhaEncontrada[0] = true;
                        userCorreto[0] = obj;
                        break;
                    }
                } else if (obj.getString("senha").equals(senha)) {
                    senhaEncontrada[0] = true;
                }
            } catch (JSONException e) {
                crashlytics.recordException(e);
                e.printStackTrace();
            }
        }

        finalizarLogin(userCorreto[0], nomeEncontrado[0], senhaEncontrada[0], name, senha);
    }

    private void finalizarLogin(JSONObject userCorreto, boolean nomeEncontrado, 
                              boolean senhaEncontrada, String name, String senha) {
        new Handler().postDelayed(() -> {
            if (progressDialog != null) progressDialog.dismiss();

            if (userCorreto != null) {
                salvarDadosLogin(name, senha);
                try {
                    verificarUsuario(userCorreto);
                } catch (JSONException e) {
                    crashlytics.recordException(e);
                    Toast.makeText(LoginActivity.this,
                            "Erro ao processar dados do usuário",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                tratarLoginInvalido(nomeEncontrado, senhaEncontrada);
            }
        }, 1500);
    }

    private void salvarDadosLogin(String name, String senha) {
        if (checkBoxLembrar.isChecked()) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("saved_name", name);
            editor.putString("saved_senha", senha);
            editor.putBoolean("lembrar_login", true);
            editor.putBoolean("auto_login", true);
            editor.apply();
        } else {
            prefs.edit().clear().apply();
        }
    }

    private void tratarLoginInvalido(boolean nomeEncontrado, boolean senhaEncontrada) {
        if (!nomeEncontrado && !senhaEncontrada) {
            firebaseAnalytics.logEvent("login_new_account_needed", null);
            Toast.makeText(this,
                    "Acesso não autorizado! Para utilizar o CarsaiPlay Pro, entre em contato com o administrador",
                    Toast.LENGTH_LONG).show();
            abrirWhatsAppNovaConta();
        } else if (!nomeEncontrado) {
            firebaseAnalytics.logEvent("login_invalid_username", null);
            Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show();
        } else {
            firebaseAnalytics.logEvent("login_invalid_password", null);
            Toast.makeText(this, "Senha incorreta", Toast.LENGTH_SHORT).show();
        }
    }

    private void tratarErroLogin(VolleyError error) {
        if (progressDialog != null) progressDialog.dismiss();
        crashlytics.recordException(error);
        Toast.makeText(this, "Não foi possível estabelecer conexão com o servidor", Toast.LENGTH_SHORT).show();
    }

    private void verificarUsuario(JSONObject user) throws JSONException {
        String state = user.getString("state");
        String plano = user.getString("plano");
        String name = user.getString("name");
        String expiresAt = user.getString("expiresAt");
        boolean isNewUser = user.optBoolean("isNewUser", false);

        if (state.equals("blocked")) {
            tratarUsuarioBloqueado();
            return;
        }

        verificarExpiracaoConta(user, expiresAt, name);
    }

    private void tratarUsuarioBloqueado() {
        firebaseAnalytics.logEvent("login_blocked_user", null);
        Toast.makeText(this, "Acesso suspenso! Entre em contato com o suporte.", Toast.LENGTH_LONG).show();
        abrirWhatsAppSuporte();
    }

    private void verificarExpiracaoConta(JSONObject user, String expiresAt, String name) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date dataExpiracao = sdf.parse(expiresAt);
            Date hoje = new Date();

            long diff = dataExpiracao.getTime() - hoje.getTime();
            long diasRestantes = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

            if (diasRestantes < 0) {
                mostrarDialogoAssinaturaExpirada(name);
            } else if (diasRestantes < 7) {
                mostrarDialogoAssinaturaProximaExpirar(diasRestantes, user, name);
            } else {
                prosseguirLogin(user);
            }
        } catch (ParseException | JSONException e) {
            crashlytics.recordException(e);
            Toast.makeText(this, "Erro ao verificar validade da assinatura", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoAssinaturaExpirada(String name) {
        firebaseAnalytics.logEvent("login_expired_account", null);
        new AlertDialog.Builder(this)
                .setTitle("Assinatura Expirada")
                .setMessage("Sua assinatura expirou. Deseja renovar agora?")
                .setPositiveButton("Renovar", (dialog, which) -> abrirWhatsAppRenovacao(name))
                .setNegativeButton("Depois", null)
                .show();
    }

    private void mostrarDialogoAssinaturaProximaExpirar(long diasRestantes, JSONObject user, String name) {
        firebaseAnalytics.logEvent("login_expiring_soon", null);
        new AlertDialog.Builder(this)
                .setTitle("Aviso de Vencimento")
                .setMessage("Sua assinatura expira em " + diasRestantes + " dias. Deseja renovar agora?")
                .setPositiveButton("Renovar", (dialog, which) -> abrirWhatsAppRenovacao(name))
                .setNegativeButton("Mais tarde", (dialog, which) -> {
                    try {
                        prosseguirLogin(user);
                    } catch (JSONException e) {
                        crashlytics.recordException(e);
                    }
                })
                .show();
    }

    private void mostrarTabelaPrecos() {
        String tabelaPrecos = "Confira os nossos planos e preços:\n\n"
            + "Plano: Teste\n"
            + "Validade: 30 minutos a 1 hora\n"
            + "Preço: Gratuito\n"
            + "Acesso: Limitado. Você poderá acessar apenas 4 sites dentro de 2 categorias.\n\n"
            + "Plano: Básico\n"
            + "Validade: Entre 1 dia e 30 dias (1 mês)\n"
            + "Preços:\n"
            + "1 dia: 25 MZN\n"
            + "7 dias: 150 MZN\n"
            + "30 dias (1 mês): 250 MZN\n"
            + "Acesso: Limitado. Apenas 3 categorias disponíveis para acesso.\n\n"
            + "Plano: Premium\n"
            + "Validade: Entre 30 dias (1 mês) e 1 ano\n"
            + "Preços:\n"
            + "30 dias (1 mês): 260 MZN\n"
            + "6 meses: 1200 MZN\n"
            + "12 meses (1 ano): 2300 MZN\n"
            + "Acesso: Total e Ilimitado. Acesso a todos os sites e categorias.\n\n"
            + "Descontos e Cupons\n"
            + "Descontos e cupons especiais podem ser aplicados a qualquer um dos planos. Fique atento às nossas promoções e aproveite os preços reduzidos!";
        new AlertDialog.Builder(this)
                .setTitle("Opções de Assinatura")
                .setMessage(tabelaPrecos)
                .setPositiveButton("Fechar", null)
                .setNegativeButton("Criar Conta", (dialog, which) -> abrirWhatsAppNovaConta())
                .show();
    }

    private void abrirWhatsAppSuporte() {
        abrirWhatsAppComMensagem("+258862414345", 
                "Olá, minha conta no CarsaiPlay Pro foi suspensa e preciso de ajuda.");
    }

    private void abrirWhatsAppNovaConta() {
        abrirWhatsAppComMensagem("+258862414345", 
                "Olá, tenho interesse em criar uma conta no CarsaiPlay Pro.");
    }

    private void abrirWhatsAppRenovacao(String userName) {
        abrirWhatsAppComMensagem("+258862414345", 
                "Olá, meu nome é " + userName + " e gostaria de renovar minha assinatura do CarsaiPlay Pro.");
    }

    private void abrirWhatsAppRecuperarConta() {
        abrirWhatsAppComMensagem("+258862414345", 
                "Olá, preciso recuperar o acesso à minha conta do CarsaiPlay Pro.");
    }

    private void abrirWhatsAppComMensagem(String numero, String mensagem) {
        String url = "https://wa.me/" + numero + "?text=" + Uri.encode(mensagem);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void prosseguirLogin(JSONObject user) throws JSONException {
        String plano = user.getString("plano");
        String name = user.getString("name");
        boolean isNewUser = user.optBoolean("isNewUser", false);
        
        int totalSites = calcularTotalSites(plano);
        int totalCategorias = calcularTotalCategorias(plano);

        salvarDadosUsuario(user);
        mostrarMensagemBoasVindas(name, plano, totalSites, totalCategorias, isNewUser);
    }

    private int calcularTotalSites(String plano) {
        switch (plano.toLowerCase()) {
            case "teste":
                return 4;
            case "básico":
            case "basico":
            case "premium":
            case "vip":
                int count = 0;
                for (JSONObject site : listaSites) {
                    try {
                        String categoria = site.getString("category");
                        if (!categoria.equals("intent") && 
                            !categoria.equals("ads") && 
                            !categoria.equals("pop-up")) {
                            count++;
                        }
                    } catch (JSONException e) {
                        crashlytics.recordException(e);
                    }
                }
                return count;
            default:
                return 0;
        }
    }

    private int calcularTotalCategorias(String plano) {
        switch (plano.toLowerCase()) {
            case "teste":
                return 2;
            case "básico":
            case "basico":
                return 3;
            case "premium":
            case "vip":
                Set<String> categorias = new HashSet<>();
                for (JSONObject site : listaSites) {
                    try {
                        String categoria = site.getString("category");
                        if (!categoria.equals("intent") && 
                            !categoria.equals("ads") && 
                            !categoria.equals("pop-up")) {
                            categorias.add(categoria);
                        }
                    } catch (JSONException e) {
                        crashlytics.recordException(e);
                    }
                }
                return categorias.size();
            default:
                return 0;
        }
    }

    private void salvarDadosUsuario(JSONObject user) throws JSONException {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userId", user.getString("id"));
        editor.putString("userName", user.getString("name"));
        editor.putString("userPlano", user.getString("plano"));
        editor.apply();
    }

    private void mostrarMensagemBoasVindas(String name, String plano, int totalSites, 
                                          int totalCategorias, boolean isNewUser) {
        String mensagem;
        if (plano.equalsIgnoreCase("premium") || plano.equalsIgnoreCase("vip")) {
            mensagem = String.format("Olá %s! Seu plano inclui acesso ilimitado a todo o conteúdo disponível.",
                    name, totalSites);
        } else if (plano.equalsIgnoreCase("basico") || plano.equalsIgnoreCase("básico")) {
            mensagem = String.format("Olá %s! Seu plano inclui acesso às categorias: Filmes e Séries, Animes, HQs e Mangás.",
                    name, totalSites);
        } else if (plano.equalsIgnoreCase("teste")) {
            mensagem = String.format("Olá %s! Sua conta de teste permite acesso a 4 sites em 2 categorias.", name);
        } else {
            mensagem = String.format("Olá %s!", name);
        }

        if (isNewUser) {
            mensagem += "\nSeja bem-vindo à nossa comunidade, " + name + "!";
            firebaseAnalytics.logEvent("new_user_login", null);
        }

        mostrarDialogoBoasVindas(mensagem, name, plano);
    }

    private void mostrarDialogoBoasVindas(String mensagem, String name, String plano) {
        new AlertDialog.Builder(this)
                .setTitle("Bem-vindo!")
                .setMessage(mensagem)
                .setPositiveButton("OK", (dialog, which) -> {
                    Intent intent = new Intent(LoginActivity.this, SitesActivity.class);
                    intent.putExtra("userId", prefs.getString("userId", ""));
                    intent.putExtra("userName", name);
                    intent.putExtra("userPlano", plano);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}