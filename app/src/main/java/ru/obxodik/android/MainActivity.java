package ru.obxodik.android;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_VPN = 4201;
    private static final int REQ_NOTIFICATIONS = 4202;

    private MaterialSwitch swYoutube;
    private MaterialSwitch swDiscord;
    private MaterialSwitch swTelegram;
    private MaterialSwitch swTgWs;
    private MaterialButton masterBtn;
    private MaterialButton logsBtn;
    private MaterialButton githubBtn;
    private MaterialButton tgConnectBtn;
    private MaterialButton tgCopyBtn;
    private Spinner strategySpinner;
    private TextView statusDot;
    private TextView statusTitle;
    private TextView statusSub;
    private TextView tvYoutubeSub;
    private TextView tvDiscordSub;
    private TextView tvTelegramSub;
    private TextView tgwsStatus;
    private TextView logView;
    private TextView versionText;

    private boolean updating;
    private boolean logsOpen;
    private boolean pendingVpn;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            refreshUi();
            if (logsOpen) loadLogs();
            handler.postDelayed(this, 2000);
        }
    };

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.rootScroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        swYoutube = findViewById(R.id.swYoutube);
        swDiscord = findViewById(R.id.swDiscord);
        swTelegram = findViewById(R.id.swTelegram);
        swTgWs = findViewById(R.id.swTgWs);
        masterBtn = findViewById(R.id.masterBtn);
        logsBtn = findViewById(R.id.logsBtn);
        githubBtn = findViewById(R.id.githubBtn);
        tgConnectBtn = findViewById(R.id.tgConnectBtn);
        tgCopyBtn = findViewById(R.id.tgCopyBtn);
        strategySpinner = findViewById(R.id.strategySpinner);
        statusDot = findViewById(R.id.statusDot);
        statusTitle = findViewById(R.id.statusTitle);
        statusSub = findViewById(R.id.statusSub);
        tvYoutubeSub = findViewById(R.id.tvYoutubeSub);
        tvDiscordSub = findViewById(R.id.tvDiscordSub);
        tvTelegramSub = findViewById(R.id.tvTelegramSub);
        tgwsStatus = findViewById(R.id.tgwsStatus);
        logView = findViewById(R.id.logView);
        versionText = findViewById(R.id.versionText);

        versionText.setText("Obxodik " + BuildConfig.VERSION_NAME + " · Android");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, Strategies.NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        strategySpinner.setAdapter(adapter);

        setupListeners();
        requestNotificationPermission();
        refreshUi();

        if (Prefs.master(this)) {
            StatusStore.VpnStatus vpn = StatusStore.vpn(this);
            StatusStore.TgStatus tg = StatusStore.tg(this);
            if (!vpn.running && !tg.running) {
                handler.postDelayed(this::applyAll, 700);
            }
        }
    }

    private void setupListeners() {
        swYoutube.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            Prefs.setYoutube(this, checked);
            afterSelectionChanged();
        });
        swDiscord.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            Prefs.setDiscord(this, checked);
            afterSelectionChanged();
        });
        swTelegram.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            Prefs.setTelegram(this, checked);
            afterSelectionChanged();
        });
        swTgWs.setOnCheckedChangeListener((button, checked) -> {
            if (updating) return;
            Prefs.setTgws(this, checked);
            if (Prefs.master(this)) applyAll();
            refreshUi();
        });

        strategySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (updating) return;
                Prefs.setStrategy(MainActivity.this, position);
                if (Prefs.master(MainActivity.this)) applyAll();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        masterBtn.setOnClickListener(v -> {
            if (Prefs.master(this)) {
                Prefs.setMaster(this, false);
                stopServices();
                refreshUi();
                return;
            }
            if (!swYoutube.isChecked() && !swDiscord.isChecked() && !swTelegram.isChecked()) {
                toast(getString(R.string.no_apps));
                return;
            }
            Prefs.setMaster(this, true);
            applyAll();
        });

        logsBtn.setOnClickListener(v -> {
            logsOpen = !logsOpen;
            logView.setVisibility(logsOpen ? View.VISIBLE : View.GONE);
            logsBtn.setText(logsOpen ? R.string.hide_logs : R.string.logs);
            if (logsOpen) loadLogs();
        });

        githubBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/tokhirdzhon/obxodik-android")));
            } catch (Exception e) {
                toast(e.getMessage());
            }
        });

        tgConnectBtn.setOnClickListener(v -> connectTelegram());
        tgCopyBtn.setOnClickListener(v -> copyTelegramLink());
    }

    private void afterSelectionChanged() {
        if (Prefs.master(this)) applyAll();
        refreshUi();
    }

    private void applyAll() {
        if (!Prefs.master(this)) {
            stopServices();
            refreshUi();
            return;
        }

        boolean yt = Prefs.youtube(this);
        boolean dc = Prefs.discord(this);
        boolean tg = Prefs.telegram(this);
        boolean ws = Prefs.tgws(this);

        if (!yt && !dc && !tg) {
            toast(getString(R.string.no_apps));
            Prefs.setMaster(this, false);
            stopServices();
            refreshUi();
            return;
        }

        List<String> vpnPackages = AppPackages.vpnPackages(this, yt, dc, tg, ws);
        if (!vpnPackages.isEmpty()) {
            Intent prepare = VpnService.prepare(this);
            if (prepare != null) {
                pendingVpn = true;
                toast(getString(R.string.vpn_permission));
                startActivityForResult(prepare, REQ_VPN);
                return;
            }
            ServiceController.startVpn(this, vpnPackages, Strategies.args(Prefs.strategy(this)));
        } else {
            ServiceController.stopVpn(this);
        }

        boolean telegramInstalled = !AppPackages.telegramPackages(this).isEmpty();
        if (tg && ws && telegramInstalled) {
            int port = Prefs.findFreePort(Prefs.tgPort(this));
            Prefs.setTgPort(this, port);
            ServiceController.startTgWs(this, port, Prefs.telegramSecret(this));
        } else {
            ServiceController.stopTgWs(this);
        }

        handler.postDelayed(this::refreshUi, 1100);
        handler.postDelayed(this::refreshUi, 2600);
    }

    private void stopServices() {
        ServiceController.stopVpn(this);
        ServiceController.stopTgWs(this);
        handler.postDelayed(this::refreshUi, 700);
    }

    private void refreshUi() {
        updating = true;
        swYoutube.setChecked(Prefs.youtube(this));
        swDiscord.setChecked(Prefs.discord(this));
        swTelegram.setChecked(Prefs.telegram(this));
        swTgWs.setChecked(Prefs.tgws(this));
        strategySpinner.setSelection(Prefs.strategy(this));
        updating = false;

        boolean ytInstalled = AppPackages.installed(this, AppPackages.YOUTUBE);
        boolean dcInstalled = AppPackages.installed(this, AppPackages.DISCORD);
        boolean tgInstalled = !AppPackages.telegramPackages(this).isEmpty();

        swYoutube.setEnabled(ytInstalled);
        swDiscord.setEnabled(dcInstalled);
        swTelegram.setEnabled(tgInstalled);
        swTgWs.setEnabled((tgInstalled && Prefs.telegram(this)) || Prefs.tgws(this));

        tvYoutubeSub.setText(ytInstalled ? R.string.youtube_sub : R.string.not_installed);
        tvDiscordSub.setText(dcInstalled ? R.string.discord_sub : R.string.not_installed);
        tvTelegramSub.setText(tgInstalled ? R.string.telegram_sub : R.string.not_installed);

        StatusStore.VpnStatus vpn = StatusStore.vpn(this);
        StatusStore.TgStatus tg = StatusStore.tg(this);
        boolean running = vpn.running || tg.running;
        boolean master = Prefs.master(this);
        String error = !vpn.error.isEmpty() ? vpn.error : tg.error;

        masterBtn.setText(master ? R.string.stop : R.string.start);

        if (running) {
            statusDot.setTextColor(ContextCompat.getColor(this, R.color.ok));
            statusTitle.setText("Обход работает");
            StringBuilder sub = new StringBuilder();
            if (vpn.running) {
                sub.append("YouTube/Discord через VPN");
            }
            if (tg.running) {
                if (sub.length() > 0) sub.append(" · ");
                sub.append("Telegram WS-прокси");
            }
            statusSub.setText(sub.length() == 0 ? "Активные профили запущены" : sub.toString());
        } else if (master) {
            statusDot.setTextColor(ContextCompat.getColor(this, error.isEmpty() ? R.color.warn : R.color.danger));
            statusTitle.setText(error.isEmpty() ? "Запуск…" : "Обход не запустился");
            statusSub.setText(error.isEmpty() ? "Применяем стратегию и разрешения" : error);
        } else {
            statusDot.setTextColor(ContextCompat.getColor(this, R.color.muted_dim));
            statusTitle.setText(R.string.status_off);
            statusSub.setText(R.string.status_off_sub);
        }

        if (tg.running) {
            tgwsStatus.setTextColor(ContextCompat.getColor(this, R.color.ok));
            tgwsStatus.setText("127.0.0.1:" + tg.port + " · " + getString(R.string.tg_ws_running));
        } else if (!tg.error.isEmpty()) {
            tgwsStatus.setTextColor(ContextCompat.getColor(this, R.color.danger));
            tgwsStatus.setText(tg.error);
        } else {
            tgwsStatus.setTextColor(ContextCompat.getColor(this, R.color.muted_dim));
            tgwsStatus.setText(R.string.tg_ws_off);
        }

        boolean hasLink = tg.link != null && !tg.link.isEmpty();
        tgConnectBtn.setEnabled(hasLink);
        tgCopyBtn.setEnabled(hasLink);
        if (logsOpen) loadLogs();
    }

    private void connectTelegram() {
        StatusStore.TgStatus tg = StatusStore.tg(this);
        if (tg.link == null || tg.link.isEmpty()) {
            toast(getString(R.string.tg_not_running));
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(tg.link)));
        } catch (Exception e) {
            toast("Telegram не установлен или ссылка не поддерживается");
        }
    }

    private void copyTelegramLink() {
        StatusStore.TgStatus tg = StatusStore.tg(this);
        if (tg.link == null || tg.link.isEmpty()) {
            toast(getString(R.string.tg_not_running));
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Obxodik proxy", tg.link));
            toast(getString(R.string.link_copied));
        }
    }

    private void loadLogs() {
        logView.setText(LogStore.readAll(this, 300));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(
                this,
                statusReceiver,
                new IntentFilter(ServiceController.ACTION_STATUS),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        handler.post(poll);
        refreshUi();
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(poll);
        try {
            unregisterReceiver(statusReceiver);
        } catch (Exception ignored) {
            // not registered
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_VPN) {
            pendingVpn = false;
            if (resultCode == RESULT_OK) {
                applyAll();
            } else {
                Prefs.setMaster(this, false);
                toast("Без разрешения VPN обход YouTube/Discord невозможен");
                stopServices();
                refreshUi();
            }
        }
    }
}
