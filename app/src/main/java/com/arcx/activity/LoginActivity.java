package com.arcx.activity;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.arcx.R;
import com.arcx.utils.FileTask;
import com.arcx.utils.utils;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LoginActivity extends AppCompatActivity {

    static {
        try { System.loadLibrary("akshit"); } catch (Throwable ignored) {}
    }

    private static final int STORAGE_REQUEST = 100;
    private static final int UNKNOWN_REQUEST = 200;
    private static final int OVERLAY_REQUEST = 300;
    private static final String USER = "USER";

    private utils prefs;
    private EditText userKey;
    private Button loginButton;
    private TextView pasteButton;
    private TextView buyKey;
    private Dialog loadingDialog;

    public static native boolean nativeVerifySignature(Context context);
    private static native String Check(Context context, String key);
    private native String GetKey();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new utils(this);
        userKey = findViewById(R.id.userkey);
        loginButton = findViewById(R.id.login);
        pasteButton = findViewById(R.id.paste);
        buyKey = findViewById(R.id.GetKey);

        userKey.setText(prefs.getSt(USER, ""));
        loginButton.setEnabled(false);

        buyKey.setOnClickListener(v -> {
            try {
                String link = GetKey();
                if (link == null || link.trim().isEmpty()) return;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
            } catch (Throwable e) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
            }
        });

        pasteButton.setOnClickListener(v -> pasteKey());
        loginButton.setOnClickListener(v -> performLogin());
        checkPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(this::checkPermissions, 250);
    }

    private void checkPermissions() {
        if (!hasStoragePermission()) { loginButton.setEnabled(false); requestStoragePermission(); return; }
        if (!hasUnknownAppsPermission()) { loginButton.setEnabled(false); requestUnknownAppsPermission(); return; }
        if (!hasOverlayPermission()) { loginButton.setEnabled(false); requestOverlayPermission(); return; }
        loginButton.setEnabled(true);
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true;
        return Environment.isExternalStorageManager();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, STORAGE_REQUEST);
            } catch (Throwable e) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), STORAGE_REQUEST);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            }, STORAGE_REQUEST);
        }
    }

    private boolean hasUnknownAppsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        return getPackageManager().canRequestPackageInstalls();
    }

    private void requestUnknownAppsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, UNKNOWN_REQUEST);
        }
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermissions();
    }

    private void pasteKey() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard == null || !clipboard.hasPrimaryClip()) {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            CharSequence value = clip.getItemAt(0).getText();
            if (value == null) return;

            String key = value.toString().trim();
            if (key.length() > 5) {
                userKey.setText(key);
                userKey.setSelection(userKey.length());
            } else {
                Toast.makeText(this, "Invalid key", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    private void performLogin() {
        if (!hasAllPermissions()) { checkPermissions(); return; }

        String key = userKey.getText().toString().trim();
        if (key.isEmpty()) { userKey.setError("Enter your key"); return; }

        prefs.setSt(USER, key);
        showLoading("Verifying key...", false);

        new Thread(() -> {
            String result;
            try { result = Check(LoginActivity.this, key); } 
            catch (Throwable e) { result = e.getMessage() != null ? e.getMessage() : "Authentication failed"; }

            String finalResult = result;
            runOnUiThread(() -> {
                if ("OK".equals(finalResult)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("ARC-X Key", key));

                    showLoading("Preparing download...", false);
                    FileTask task = new FileTask(LoginActivity.this, success -> {
                        runOnUiThread(() -> {
                            dismissLoading();
                            if (!success) Toast.makeText(LoginActivity.this, "Resource download failed, but access granted.", Toast.LENGTH_LONG).show();
                            openMain();
                        });
                    });

                    task.setProgressListener(progress -> runOnUiThread(() -> updateDownloadProgress(progress)));
                    task.execute();
                } else {
                    dismissLoading();
                    showLoading(finalResult, true);
                }
            });
        }).start();
    }

    private void updateDownloadProgress(int progress) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            TextView message = loadingDialog.findViewById(R.id.statusText);
            ProgressBar bar = loadingDialog.findViewById(R.id.progressBar);
            if (message != null) message.setText("Downloading files... " + progress + "%");
            if (bar != null) { bar.setIndeterminate(false); bar.setProgress(progress); }
        }
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean hasAllPermissions() {
        return hasStoragePermission() && hasUnknownAppsPermission() && hasOverlayPermission();
    }

    private void showLoading(String text, boolean error) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.dialog_status);
            loadingDialog.setCancelable(false);
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }

        TextView message = loadingDialog.findViewById(R.id.statusText);
        ProgressBar bar = loadingDialog.findViewById(R.id.progressBar);
        Button ok = loadingDialog.findViewById(R.id.okButton);

        message.setText(text);
        if (error) {
            if (bar != null) bar.setVisibility(View.GONE);
            ok.setVisibility(View.VISIBLE);
            ok.setOnClickListener(v -> dismissLoading());
        } else {
            if (bar != null) { bar.setVisibility(View.VISIBLE); bar.setIndeterminate(true); }
            ok.setVisibility(View.GONE);
        }

        if (!loadingDialog.isShowing()) loadingDialog.show();
    }

    private void dismissLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
    }
}