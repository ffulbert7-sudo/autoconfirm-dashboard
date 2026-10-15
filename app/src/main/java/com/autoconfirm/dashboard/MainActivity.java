package com.autoconfirm.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String serverUrl = prefs.getString("server_url", "");
        String sessionCookie = prefs.getString("session_cookie", "");

        if (serverUrl.isEmpty() || sessionCookie.isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        webView = findViewById(R.id.webview);
        progress = findViewById(R.id.progress);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().setCookie(serverUrl, sessionCookie);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                if (url.contains("/login")) {
                    SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
                    editor.remove("session_cookie");
                    editor.apply();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        });

        webView.loadUrl(serverUrl + "/");
        // Demarrer AlertService
        startService(new Intent(this, AlertService.class));
        // Demander permission notifications Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        // Creer canal et envoyer notif de bienvenue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "AutoConfirmAlerts", "Alertes AutoConfirm", android.app.NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        // Notification de bienvenue pour enregistrer l'app
        SharedPreferences prefsCheck = getSharedPreferences("config", MODE_PRIVATE);
        if (!prefsCheck.getBoolean("notif_init", false)) {
            androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, "AutoConfirmAlerts")
                .setContentTitle("✅ AutoConfirm Dashboard")
                .setContentText("Alertes activées ! Vous serez notifié des sessions expirées.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
            android.app.NotificationManager nm2 = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm2 != null) nm2.notify(999, builder.build());
            prefsCheck.edit().putBoolean("notif_init", true).apply();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
