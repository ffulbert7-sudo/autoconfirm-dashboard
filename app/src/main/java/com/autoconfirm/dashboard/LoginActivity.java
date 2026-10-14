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
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private EditText etUrl, etEmail, etPassword;
    private Button btnLogin;
    private TextView tvError;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUrl = findViewById(R.id.et_url);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvError = findViewById(R.id.tv_error);

        // URL par defaut
        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", "https://autoconfirm.online");
        etUrl.setText(savedUrl);

        btnLogin.setOnClickListener(v -> doLogin());
    }

    private void doLogin() {
        String url = etUrl.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (url.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Tous les champs sont requis");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Connexion...");
        tvError.setVisibility(View.GONE);

        RequestBody body = new FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build();

        Request request = new Request.Builder()
            .url(url + "/login")
            .post(body)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showError("Erreur réseau: " + e.getMessage());
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Se connecter");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String cookie = response.header("Set-Cookie", "");
                String responseUrl = response.request().url().toString();

                runOnUiThread(() -> {
                    if (cookie.contains("connect.sid") || responseUrl.contains("/")) {
                        // Extraire connect.sid
                        String sessionCookie = "";
                        for (String part : cookie.split(";")) {
                            if (part.trim().startsWith("connect.sid")) {
                                sessionCookie = part.trim();
                                break;
                            }
                        }
                        SharedPreferences.Editor editor = getSharedPreferences("config", MODE_PRIVATE).edit();
                        editor.putString("server_url", url);
                        editor.putString("session_cookie", sessionCookie);
                        editor.apply();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        showError("Email ou mot de passe incorrect");
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Se connecter");
                    }
                });
            }
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
