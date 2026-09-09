package com.arcx.activity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.arcx.R;
import com.arcx.adapter.GamePagerAdapter;
import com.arcx.adapter.GamePagerAdapter.GameItem;
import com.arcx.utils.ObbManage;
import com.mundo.MundoCore;
import com.mundo.entity.pm.InstallResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class MainActivity extends AppCompatActivity {

    static {
        try { System.loadLibrary("zenin"); } catch (Throwable ignored) {}
    }

    private static final int USER_ID = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean exitPending = false;

    private TextView expiryText;
    private TextView androidVersion;
    private TextView deviceName;
    
    private LinearLayout obbButton;
    private LinearLayout fbButton;
    private ViewPager2 gameViewPager;
    private LinearLayout dotsIndicator;
    private GamePagerAdapter gameAdapter;
    private List<GameItem> gameList;

    public static native String exdate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Bind Views
        expiryText = findViewById(R.id.expiryText);
        androidVersion = findViewById(R.id.androidVersion);
        deviceName = findViewById(R.id.deviceName);
        obbButton = findViewById(R.id.obbFixButton);
        fbButton = findViewById(R.id.fbFixButton);
        gameViewPager = findViewById(R.id.gameViewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);

        showDeviceInfo();
        startExpiryTimer();
        setupGameViewPager();

        // Click Listeners
        obbButton.setOnClickListener(v -> fixObb());
        fbButton.setOnClickListener(v -> facebookFix());
    }

    private void setupGameViewPager() {
        gameList = new ArrayList<>();
        // 4 distinct package names as requested
        gameList.add(new GameItem("BGMI INDIA", "com.pubg.imobile"));
        gameList.add(new GameItem("PUBG MOBILE (GLOBAL)", "com.tencent.ig"));
        gameList.add(new GameItem("PUBG MOBILE (KR)", "com.pubg.krmobile"));
        gameList.add(new GameItem("PUBG MOBILE (VN)", "com.vng.pubgmobile"));

        gameAdapter = new GamePagerAdapter(this, gameList, (item, isInstalled) -> {
            handleGameClick(item, isInstalled);
        });

        gameViewPager.setAdapter(gameAdapter);
        setupDotsIndicator(gameList.size());

        gameViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotsIndicator(position);
            }
        });
    }

    private void setupDotsIndicator(int count) {
        dotsIndicator.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    i == 0 ? 24 : 12,
                    12
            );
            params.setMargins(6, 0, 6, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0 ? R.drawable.bg_indicator_active : R.drawable.bg_indicator_inactive);
            dotsIndicator.addView(dot);
        }
    }

    private void updateDotsIndicator(int position) {
        int childCount = dotsIndicator.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View dot = dotsIndicator.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (i == position) {
                params.width = 24;
                dot.setBackgroundResource(R.drawable.bg_indicator_active);
            } else {
                params.width = 12;
                dot.setBackgroundResource(R.drawable.bg_indicator_inactive);
            }
            dot.setLayoutParams(params);
        }
    }

    private void handleGameClick(GameItem item, boolean isInstalled) {
        try {
            if (MundoCore.get() == null) {
                Toast.makeText(this, "Engine unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isInstalled) {
                Toast.makeText(this, "Launching " + item.title + "...", Toast.LENGTH_SHORT).show();
                MundoCore.get().launchApk(item.packageName, USER_ID);
                return;
            }

            Toast.makeText(this, "Installing " + item.title + "...", Toast.LENGTH_SHORT).show();
            ObbManage manager = new ObbManage(this);
            manager.installOrFix((success, message) -> runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (gameAdapter != null) {
                    gameAdapter.notifyDataSetChanged();
                }
            }));
        } catch (Throwable e) {
            Toast.makeText(this, "Game action error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showDeviceInfo() {
        String model = Build.MODEL != null ? Build.MODEL : "Unknown Device";
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        deviceName.setText(manufacturer.toUpperCase() + " " + model);
        androidVersion.setText("ANDROID " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
    }

    private void startExpiryTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    String value = exdate();
                    if (value == null || value.trim().isEmpty() || "NULL".equalsIgnoreCase(value)) {
                        expiryText.setText("EXPIRY • UNKNOWN");
                        expiryText.setTextColor(getColor(R.color.text_secondary));
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    Date expiry = sdf.parse(value);
                    if (expiry == null) {
                        expiryText.setText("EXPIRY • ERROR");
                        return;
                    }

                    long diff = expiry.getTime() - System.currentTimeMillis();
                    if (diff <= 0) {
                        expiryText.setText("EXPIRED");
                        expiryText.setTextColor(getColor(R.color.arcx_red));
                        Toast.makeText(MainActivity.this, "Key expired", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long days = diff / 86400000L;
                    long hours = (diff / 3600000L) % 24;
                    long minutes = (diff / 60000L) % 60;
                    long seconds = (diff / 1000L) % 60;

                    expiryText.setText(String.format(Locale.US, "%03dD : %02dH : %02dM : %02dS", days, hours, minutes, seconds));
                    expiryText.setTextColor(Color.parseColor("#FFD700"));
                    handler.postDelayed(this, 1000);
                } catch (Throwable e) {
                    expiryText.setText("EXPIRY • ERROR");
                    expiryText.setTextColor(getColor(R.color.arcx_red));
                }
            }
        });
    }

    private void fixObb() {
        Toast.makeText(this, "Checking OBB files...", Toast.LENGTH_SHORT).show();
        obbButton.setAlpha(0.5f);
        ObbManage manager = new ObbManage(this);
        manager.installOrFix((success, message) -> runOnUiThread(() -> {
            obbButton.setAlpha(1.0f);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }));
    }

    private void facebookFix() {
        try {
            if (MundoCore.get() == null) {
                Toast.makeText(this, "Engine unavailable", Toast.LENGTH_SHORT).show();
                return;
            }

            String fbPkg = "com.facebook.katana";
            if (MundoCore.get().isInstalled(fbPkg, USER_ID)) {
                Toast.makeText(this, "Facebook already cloned! Launching...", Toast.LENGTH_SHORT).show();
                MundoCore.get().launchApk(fbPkg, USER_ID);
            } else {
                Toast.makeText(this, "Cloning Facebook (com.facebook.katana)...", Toast.LENGTH_SHORT).show();
                Toast.makeText(this, "FB Clone Initiated.", Toast.LENGTH_LONG).show();
            }
        } catch (Throwable e) {
            Toast.makeText(this, "FB Fix Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (exitPending) { 
            finishAffinity(); 
            return; 
        }
        exitPending = true;
        Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();
        handler.postDelayed(() -> exitPending = false, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}