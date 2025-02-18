package com.carsaiplay.pro.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.carsaiplay.pro.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SitesActivity extends AppCompatActivity {

    private RecyclerView recyclerSites;
    private SitesAdapter sitesAdapter;
    private TextView textMensagemPremium;
    private Spinner spinnerCategorias;
    private String apiUrl = "https://67a30b1e409de5ed525729ab.mockapi.io/carsaiplay/Sites";
    private String userPlano;
    private ArrayList<JSONObject> listaSites = new ArrayList<>();
    private Set<String> categoriasDisponiveis = new HashSet<>();
    
    // Firebase
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics crashlytics;
    private Trace performanceTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sites);

        // Inicialização Firebase
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();
        performanceTrace = FirebasePerformance.getInstance().newTrace("sites_activity_load");
        performanceTrace.start();

        recyclerSites = findViewById(R.id.recyclerSites);
        textMensagemPremium = findViewById(R.id.textMensagemPremium);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);

        userPlano = getIntent().getStringExtra("userPlano");
        
        // Logging do plano do usuário
        crashlytics.setCustomKey("user_plan", userPlano);
        Bundle planBundle = new Bundle();
        planBundle.putString("user_plan", userPlano);
        firebaseAnalytics.logEvent("sites_activity_access", planBundle);

        recyclerSites.setLayoutManager(new LinearLayoutManager(this));
        sitesAdapter = new SitesAdapter();
        recyclerSites.setAdapter(sitesAdapter);

        carregarSites();

        spinnerCategorias.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        String categoriaSelecionada = spinnerCategorias.getSelectedItem().toString();
                        
                        // Analytics para seleção de categoria
                        Bundle categoryBundle = new Bundle();
                        categoryBundle.putString("selected_category", categoriaSelecionada);
                        firebaseAnalytics.logEvent("category_selected", categoryBundle);
                        
                        if (categoriaSelecionada.equals("HENTAIS 🔞")) {
                            mostrarAvisoHentai();
                        } else {
                            exibirSites(categoriaSelecionada);
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    private void carregarSites() {
        Trace loadTrace = FirebasePerformance.getInstance().newTrace("load_sites_api");
        loadTrace.start();

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonArrayRequest request =
                new JsonArrayRequest(
                        Request.Method.GET,
                        apiUrl,
                        null,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                loadTrace.stop();
                                performanceTrace.putMetric("sites_loaded", response.length());
                                
                                Log.d("SitesActivity", "Dados recebidos: " + response.toString());
                                listaSites.clear();
                                categoriasDisponiveis.clear();
                                for (int i = 0; i < response.length(); i++) {
                                    try {
                                        JSONObject site = response.getJSONObject(i);
                                        String categoria = site.getString("category");
                                        if (!categoria.equals("intent")
                                                && !categoria.equals("ads")
                                                && !categoria.equals("pop-up")) {
                                            listaSites.add(site);
                                            categoriasDisponiveis.add(categoria);
                                        }
                                    } catch (JSONException e) {
                                        crashlytics.recordException(e);
                                        Log.e("SitesActivity", "Erro ao processar JSON: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                                preencherSpinnerCategorias();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                loadTrace.stop();
                                crashlytics.recordException(error);
                                Log.e("SitesActivity", "Erro na requisição: " + error.getMessage());
                                Toast.makeText(
                                        SitesActivity.this,
                                        "Erro ao carregar sites!",
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

        queue.add(request);
    }

    private void preencherSpinnerCategorias() {
        Set<String> categoriasFormatadas = new HashSet<>();
        for (String categoria : categoriasDisponiveis) {
            categoriasFormatadas.add(formatarCategoria(categoria));
        }

        if (userPlano.equals("teste")) {
            categoriasFormatadas.removeIf(
                    categoria ->
                            !categoria.equals("FILMES & SÉRIES") && !categoria.equals("ANIMES"));
        } else if (userPlano.equals("básico")) {
            categoriasFormatadas.removeIf(categoria -> categoria.equals("HENTAIS 🔞"));
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this, R.layout.spinner_item, new ArrayList<>(categoriasFormatadas));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapter);

        if (userPlano.equals("básico")) {
            textMensagemPremium.setVisibility(View.VISIBLE);
            textMensagemPremium.setText(
                    "Você tem acesso limitado. Pague o plano PREMIUM para ter acesso a todas as categorias e à lista completa.");
        }
        if (userPlano.equals("teste")) {
            textMensagemPremium.setVisibility(View.VISIBLE);
            textMensagemPremium.setText(
                    "Você tem acesso limitado. Pague o plano BÁSICO ou PREMIUM para ter acesso à lista completa de 3 Categorias.");
        }
        
        // Analytics para categorias disponíveis
        Bundle categoriesBundle = new Bundle();
        categoriesBundle.putString("available_categories", categoriasFormatadas.toString());
        categoriesBundle.putString("user_plan", userPlano);
        firebaseAnalytics.logEvent("categories_loaded", categoriesBundle);
    }

    private String formatarCategoria(String categoria) {
        switch (categoria) {
            case "mangas":
                return "HQS & MANGÁS";
            case "hentais":
                return "HENTAIS 🔞";
            case "filmes":
                return "FILMES & SÉRIES";
            default:
                return categoria.toUpperCase();
        }
    }

    private void exibirSites(String categoriaSelecionada) {
        ArrayList<JSONObject> sitesFiltrados = new ArrayList<>();
        for (JSONObject site : listaSites) {
            try {
                String categoria = site.getString("category");
                String categoriaFormatada = formatarCategoria(categoria);
                if (categoriaFormatada.equals(categoriaSelecionada)) {
                    sitesFiltrados.add(site);
                }
            } catch (JSONException e) {
                crashlytics.recordException(e);
                Log.e("SitesActivity", "Erro ao filtrar sites: " + e.getMessage());
                e.printStackTrace();
            }
        }
        sitesAdapter.setSites(sitesFiltrados);
        
        // Analytics para sites exibidos
        Bundle sitesBundle = new Bundle();
        sitesBundle.putString("category", categoriaSelecionada);
        sitesBundle.putInt("sites_count", sitesFiltrados.size());
        firebaseAnalytics.logEvent("sites_displayed", sitesBundle);
    }

    private void abrirWebView(JSONObject site) {
        try {
            // Analytics para site acessado
            Bundle siteBundle = new Bundle();
            siteBundle.putString("site_name", site.getString("name"));
            siteBundle.putString("site_category", site.getString("category"));
            firebaseAnalytics.logEvent("site_accessed", siteBundle);
            
            Intent intent = new Intent(SitesActivity.this, LoadingActivity.class);
            intent.putExtra("siteUrl", site.getString("url"));
            startActivity(intent);
        } catch (JSONException e) {
            crashlytics.recordException(e);
            Log.e("SitesActivity", "Erro ao abrir WebView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mostrarAvisoHentai() {
        new AlertDialog.Builder(this)
                .setTitle("Aviso de Conteúdo Adulto")
                .setMessage("Esta categoria contém conteúdo adulto. Deseja continuar?")
                .setPositiveButton(
                        "Sim",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Analytics para confirmação de conteúdo adulto
                                Bundle hentaiBundle = new Bundle();
                                hentaiBundle.putString("action", "accepted");
                                firebaseAnalytics.logEvent("adult_content_warning", hentaiBundle);
                                
                                exibirSites("HENTAIS 🔞");
                            }
                        })
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Analytics para rejeição de conteúdo adulto
                        Bundle hentaiBundle = new Bundle();
                        hentaiBundle.putString("action", "rejected");
                        firebaseAnalytics.logEvent("adult_content_warning", hentaiBundle);
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (performanceTrace != null) {
            performanceTrace.stop();
        }
    }

    private class SitesAdapter extends RecyclerView.Adapter<SitesAdapter.SiteViewHolder> {
        private ArrayList<JSONObject> sites = new ArrayList<>();

        public void setSites(ArrayList<JSONObject> sites) {
            this.sites = sites;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_site, parent, false);
            return new SiteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SiteViewHolder holder, int position) {
            JSONObject site = sites.get(position);
            try {
                holder.textSiteName.setText(site.getString("name"));
                holder.textSiteCategory.setText(formatarCategoria(site.getString("category")));
                holder.textSiteDescription.setText(site.getString("description"));
                holder.textSiteState.setText(site.getString("state"));
            } catch (JSONException e) {
                crashlytics.recordException(e);
                Log.e("SitesActivity", "Erro ao exibir site: " + e.getMessage());
                e.printStackTrace();
            }

            holder.itemView.setOnClickListener(
                    v -> {
                        if (site.optString("category").equals("hentais")) {
                            mostrarAvisoHentai();
                        } else {
                            abrirWebView(site);
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return sites.size();
        }

        class SiteViewHolder extends RecyclerView.ViewHolder {
            TextView textSiteName;
            TextView textSiteCategory;
            TextView textSiteDescription;
            TextView textSiteState;

            public SiteViewHolder(@NonNull View itemView) {
                super(itemView);
                textSiteName = itemView.findViewById(R.id.textSiteName);
                textSiteCategory = itemView.findViewById(R.id.textSiteCategory);
                textSiteDescription = itemView.findViewById(R.id.textSiteDescription);
                textSiteState = itemView.findViewById(R.id.textSiteState);
            }
        }
    }
}