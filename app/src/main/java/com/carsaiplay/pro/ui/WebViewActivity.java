package com.carsaiplay.pro.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.carsaiplay.pro.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";
    private static final int PERMISSION_REQUEST_STORAGE = 1001;
    private static final String AD_UNIT_ID = "ca-app-pub-1454928749756538/4217770587";
    private static final String DOWNLOAD_FOLDER = "CarsaiPlay Pro";

    private WebView webView;
    private ProgressBar progressBar;
    private String siteUrl;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isVideoFullscreen = false;
    private FrameLayout fullscreenContainer;
    private Handler mainHandler;

    private FirebaseAnalytics firebaseAnalytics;
    private Trace performanceTrace;

    private AdView adView;
    private InterstitialAd interstitialAd;
    private int pageLoadCount = 0;

    private ListView mediaList;

    private final List<Uri> detectedMediaItems = new ArrayList<>();

    private final Pattern mediaUrlPattern =
            Pattern.compile(
                    ".*\\.(mp4|m3u8|mpd|ts|mkv|avi|mov|webm)$|"
                            + ".*/(manifest|master\\.m3u8|playlist\\.m3u8)$|"
                            + ".*\\.(mp3|wav|aac|ogg|m4a)$",
                    Pattern.CASE_INSENSITIVE);

    private final Set<String> blockedDomains =
            new HashSet<>(
                    Arrays.asList(
                            "example.com",
                            "malicious-site.com",
                            "ads.com",
                            "tracking.com",
                            "googleads",
                            "doubleclick",
                            "adserver",
            "pop-up",
                            "intent://",
                            "intent://ak.",
                            "adsystem",
                            "adnxs",
                            "advertising",
                            "trackingpixel",
                            "analytics",
                            "ak."));

    private final Set<String> blockedKeywords =
            new HashSet<>(
                    Arrays.asList(
                            "ads",
                            "popup",
                            "redirect",
                            "malicious",
                            "adserver",
                            "doubleclick",
                            "tracking",
                            "intent://",
                            "scheme=",
                            "fake",
                            "sponsor",
                            "sponsored",
                            "goto",
                            "aliexpress", "reward",
                            "bet",
                            "bets", "perfectgive",
                            "ak.", "package",
            "pop-up"));

    private void startDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimetype,
            long contentLength) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("User-Agent", userAgent);

            File downloadDir =
                    new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                            DOWNLOAD_FOLDER);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FOLDER + "/" + fileName);
            request.setTitle(fileName);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.allowScanningByMediaScanner();

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                Toast.makeText(this, "Starting download: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        mainHandler = new Handler(getMainLooper());
        initializeFirebase();
        initializeAdMob();
        initializeViews();
        configureWebView();
        configureFooterButtons();
        configureDownloadListener();
    }

    private void initializeFirebase() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        performanceTrace = FirebasePerformance.getInstance().newTrace("webview_load");
        performanceTrace.start();
    }

    private void initializeAdMob() {
        MobileAds.initialize(this, initializationStatus -> {});
    }

    private void loadAds() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        InterstitialAd.load(
                this,
                AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                    }
                });
    }

    private void initializeViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.webProgress);
        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mediaList = findViewById(R.id.mediaList);

        siteUrl = getIntent().getStringExtra("siteUrl");
        if (siteUrl == null || siteUrl.isEmpty()) {
            finish();
            return;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        configureWebViewClient();
        configureWebChromeClient();

        if (siteUrl != null) {
            webView.loadUrl(siteUrl);
        }
    }

    private void configureWebViewClient() {
    webView.setWebViewClient(new WebViewClient() {

        // Intercepta os links antes de carregá-los
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            // Bloqueia URLs específicas
            if (isUrlBlocked(url)) {
                return true; // Bloqueia a navegação
            }

            // Trata links de telefone e e-mail
            if (url.startsWith("tel:") || url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true; // Lança a ação apropriada
            }

            // Carrega a URL na WebView
            view.loadUrl(url);

          

            return true;
        }

        // Chamado quando o carregamento de uma página é iniciado
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE); // Exibe a barra de progresso

            // Injeta scripts se necessário
            injectScripts(view);
        }

        // Chamado quando o carregamento da página é concluído
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE); // Oculta a barra de progresso

            // Garantir que o conteúdo da página foi carregado corretamente
            // Você pode adicionar verificações ou melhorias aqui se necessário
        }

        // Chamado quando ocorre um erro ao carregar a página
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            progressBar.setVisibility(View.GONE); // Oculta a barra de progresso em caso de erro

            // Exibe uma mensagem de erro ou página personalizada
            Toast.makeText(view.getContext(), "Erro ao carregar a página", Toast.LENGTH_SHORT).show();
            // Pode-se também exibir uma página de erro personalizada ou recarregar a URL
        }

       

        // Lida com falhas críticas na conexão, por exemplo, falhas de DNS
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            handler.proceed(); // Você pode optar por tratar esse erro de forma diferente
        }
    });
}

    private void configureWebChromeClient() {
        webView.setWebChromeClient(
                new WebChromeClient() {
                    @Override
                    public void onProgressChanged(WebView view, int newProgress) {
                        progressBar.setProgress(newProgress);
                        progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onShowCustomView(View view, CustomViewCallback callback) {
                        if (customView != null) {
                            callback.onCustomViewHidden();
                            return;
                        }

                        customView = view;
                        customViewCallback = callback;

                        fullscreenContainer.addView(
                                customView,
                                new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));

                        View decorView = getWindow().getDecorView();
                        ((ViewGroup) decorView).addView(fullscreenContainer);

                        decorView.setSystemUiVisibility(
                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

                        webView.setVisibility(View.GONE);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        isVideoFullscreen = true;
                    }

                    @Override
                    public void onHideCustomView() {
                        if (customView == null) return;

                        View decorView = getWindow().getDecorView();
                        ((ViewGroup) decorView).removeView(fullscreenContainer);
                        fullscreenContainer.removeAllViews();
                        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

                        webView.setVisibility(View.VISIBLE);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                        customView = null;
                        customViewCallback.onCustomViewHidden();
                        customViewCallback = null;
                        isVideoFullscreen = false;
                    }
                });
    }

    private void processMediaUrl(String url) {
        if (!containsMediaUrl(url)) {
            Uri mediaUri = createMediaItem(url);
            detectedMediaItems.add(mediaUri);
            runOnUiThread(
                    () -> {
                        updateMediaList();
                    });
        }
    }

    private boolean containsMediaUrl(String url) {
        for (Uri item : detectedMediaItems) {
            if (item.toString().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private Uri createMediaItem(String url) {
        return Uri.parse(url);
    }

    private void updateMediaList() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this, R.layout.list_item_media, R.id.mediaTitle, getMediaTitles()) {
                    @NonNull
                    @Override
                    public View getView(
                            int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);

                        ImageButton btnPlay = view.findViewById(R.id.btnPlayMedia);
                        ImageButton btnDownload = view.findViewById(R.id.btnDownloadMedia);

                        btnPlay.setOnClickListener(
                                v -> {
                                    // No player implementation, just show a toast
                                    Toast.makeText(
                                                    WebViewActivity.this,
                                                    "Playing media: "
                                                            + getMediaTitles().get(position),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                });

                        btnDownload.setOnClickListener(
                                v -> {
                                    Uri mediaUri = detectedMediaItems.get(position);
                                    String mediaUrl = mediaUri.toString();
                                    startDownload(
                                            mediaUrl,
                                            webView.getSettings().getUserAgentString(),
                                            null,
                                            null,
                                            0);
                                });

                        return view;
                    }
                };

        mediaList.setAdapter(adapter);
    }

    private List<String> getMediaTitles() {
        List<String> titles = new ArrayList<>();
        for (Uri item : detectedMediaItems) {
            String url = item.toString();
            String title = url.substring(url.lastIndexOf('/') + 1);
            titles.add(title);
        }
        return titles;
    }

    private void configureDownloadListener() {
        webView.setDownloadListener(
                (url, userAgent, contentDisposition, mimetype, contentLength) -> {
                    if (ContextCompat.checkSelfPermission(
                                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        startDownload(url, userAgent, contentDisposition, mimetype, contentLength);
                    } else {
                        ActivityCompat.requestPermissions(
                                this,
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSION_REQUEST_STORAGE);
                    }
                });
    }

    private void injectScripts(WebView view) {
        String script =
                "javascript: (function() {"
                        + "    window.open = function() {"
                        + "        console.log('Pop-up blocked!');"
                        + "        return null;"
                        + "    };"
                        + "    function removeAds() {"
                        + "        const selectors = ["
                        + "            'iframe[src*=\"ads\"]',"
                        + "            'iframe[src*=\"ad.\"]',"
                        + "            'div[class*=\"ad-\"]',"
                        + "            'div[id*=\"ad-\"]',"
                        + "            '.advertisement',"
                        + "            '#advertisement'"
                        + "        ];"
                        + "        selectors.forEach(selector => {"
                        + "            document.querySelectorAll(selector).forEach(el => {"
                        + "                if (!el.src.includes('youtube.com') && !el.src.includes('vimeo.com')) {"
                        + "                    el.remove();"
                        + "                }"
                        + "            });"
                        + "        });"
                        + "    }"
                        + "    removeAds();"
                        + "    new MutationObserver(removeAds).observe(document.body, {"
                        + "        childList: true,"
                        + "        subtree: true"
                        + "    });"
                        + "})();";

        mainHandler.post(() -> view.evaluateJavascript(script, null));
    }

    private void showInterstitialAd() {
        pageLoadCount++;
        if (pageLoadCount % 3 == 0 && interstitialAd != null) {
            interstitialAd.show(this);
        }
    }

    private boolean isUrlBlocked(String url) {
        if (url == null) return false;

        for (String domain : blockedDomains) {
            if (url.contains(domain)) return true;
        }

        for (String keyword : blockedKeywords) {
            if (url.contains(keyword)) return true;
        }

        return false;
    }

    private void configureFooterButtons() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        ImageButton btnForward = findViewById(R.id.btnForward);
        ImageButton btnMenu = findViewById(R.id.btnMenu);

        if (btnBack != null) {
            btnBack.setOnClickListener(
                    v -> {
                        if (isVideoFullscreen) {
                            webView.getWebChromeClient().onHideCustomView();
                        } else if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                    });
        }

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(
                    v -> {
                        webView.reload();
                        mainHandler.postDelayed(() -> injectScripts(webView), 1000);
                    });
        }

        if (btnForward != null) {
            btnForward.setOnClickListener(
                    v -> {
                        if (webView.canGoForward()) {
                            webView.goForward();
                        }
                    });
            btnForward.setEnabled(webView.canGoForward());
        }

        if (btnMenu != null) {
            btnMenu.setOnClickListener(
                    v -> {
                        Intent intent = new Intent(WebViewActivity.this, MenuActivity.class);
                        startActivity(intent);
                    });
        }
    }

    @Override
    public void onBackPressed() {
        if (isVideoFullscreen) {
            webView.getWebChromeClient().onHideCustomView();
            return;
        }

        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        webView.onResume();
        injectScripts(webView);
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }

        if (performanceTrace != null) {
            performanceTrace.stop();
            performanceTrace = null;
        }

        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }

        if (customView != null) {
            customView = null;
        }

        if (customViewCallback != null) {
            customViewCallback = null;
        }

        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearSslPreferences();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, recarregar a página
                webView.reload();
            } else {
                // Permissão negada, mostrar mensagem ou tratar conforme necessário
                Toast.makeText(
                                this,
                                "A permissão para gravar arquivos é necessária para o download.",
                                Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
}
