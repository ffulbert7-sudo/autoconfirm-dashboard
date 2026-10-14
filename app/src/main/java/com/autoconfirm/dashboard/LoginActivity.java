package com.autoconfirm.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LoginActivity extends AppCompatActivity {
    private EditText etUrl, etEmail, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etUrl = findViewById(R.id.et_url);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvError = findViewById(R.id.tv_error);
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        etUrl.setText(prefs.getString("server_url", "https://autoconfirm.online"));
        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        final String serverUrl = etUrl.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        if (serverUrl.isEmpty() || email.isEmpty() || password.isEmpty()) {
            tvError.setText("Tous les champs sont requis");
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        btnLogin.setEnabled(false);
        btnLogin.setText("Connexion...");
        tvError.setVisibility(View.GONE);
        new Thread(() -> {
            try {
                String body = "email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8");
                URL loginUrl = new URL(serverUrl + "/login");
                HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.close();
                int code = conn.getResponseCode();
                String cookie = conn.getHeaderField("Set-Cookie");
                conn.disconnect();
                final String finalCookie = cookie != null ? cookie : "";
                handler.post(() -> {
                    if (finalCookie.contains("authToken")) {
                        String sessionCookie = finalCookie.split(";")[0];
                        SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
                        editor.putString("server_url", serverUrl);
                        editor.putString("session_cookie", sessionCookie);
                        editor.apply();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        tvError.setText("Email ou mot de passe incorrect");
                        tvError.setVisibility(View.VISIBLE);
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Se connecter");
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    tvError.setText("Erreur: " + e.getMessage());
                    tvError.setVisibility(View.VISIBLE);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Se connecter");
                });
            }
        }).start();
    }
}
