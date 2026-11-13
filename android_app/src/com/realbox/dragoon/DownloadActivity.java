package com.realbox.dragoon;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.*;
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import legend.game.unpacker.PalSpanishLayout;

public final class DownloadActivity extends Activity {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private TextView status;
    private Button button;
    private ProgressBar progress;
    private long lastUpdate;
    private Path root;
    private String assetHash;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = getFilesDir().toPath().resolve("game");
        LinearLayout screen = new LinearLayout(this);
        screen.setBackgroundColor(0xff000000);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(32, 24, 32, 24);
        status = new TextView(this);
        status.setTextColor(0xffffffff);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(android.view.View.GONE);
        button = new Button(this);
        button.setText("Descargar contenido");
        screen.addView(status, new LinearLayout.LayoutParams(-1, -2));
        screen.addView(progress, new LinearLayout.LayoutParams(-1, -2));
        screen.addView(button);
        setContentView(screen);
        hideBars();

        try (InputStream marker = getAssets().open("engine-assets.sha256")) {
            assetHash = new String(marker.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            if (ready()) {
                startGame();
                return;
            }
            button.setOnClickListener(view -> install());
            install();
        } catch (IOException exception) {
            status.setText("No se pudo leer la configuración del juego.");
            button.setEnabled(false);
        }
    }

    private boolean ready() throws IOException {
        Path marker = root.resolve("engine-assets.sha256");
        return Files.isRegularFile(marker)
            && legend.core.TextFiles.readString(marker).equals(assetHash)
            && assetsReady(root)
            && PalSpanishLayout.isPrepared(root.resolve("files"));
    }

    private static boolean assetsReady(Path path) {
        return Files.isRegularFile(path.resolve("gfx/fonts/default.json"))
            && Files.isRegularFile(path.resolve("gfx/fonts/smooth.json"))
            && Files.isRegularFile(path.resolve("gfx/ui/title_menu.png"))
            && Files.isRegularFile(path.resolve("gfx/remaster/rose-original-hd/atlas.png"))
            && Files.isRegularFile(path.resolve("android-shaders/tmd.gsh"))
            && Files.isDirectory(path.resolve("resources/lod_core/lang"))
            && Files.isDirectory(path.resolve("patches"));
    }

    private void install() {
        button.setVisibility(android.view.View.GONE);
        progress.setVisibility(android.view.View.VISIBLE);
        status.setText("Preparando descarga…");
        WORKER.execute(() -> {
            try {
                if (isDestroyed()) return;
                if (!ready()) {
                    Properties config = new Properties();
                    try (InputStream stream = getAssets().open("content-download.properties")) {
                        config.load(stream);
                    }
                    URI url = URI.create(config.getProperty("url"));
                    long compressed = Long.parseLong(config.getProperty("size"));
                    long expanded = Long.parseLong(config.getProperty("expanded"));
                    String hash = config.getProperty("sha256");
                    Path temporary = getFilesDir().toPath().resolve("content-install");
                    Files.createDirectories(temporary);
                    Files.createDirectories(root);
                    Path archive = temporary.resolve("content.zip");
                    Path stage = temporary.resolve("staging");
                    ContentInstaller.remove(stage);
                    if (new android.os.StatFs(temporary.toString()).getAvailableBytes() < expanded + (Files.isRegularFile(archive) ? 0 : compressed)) {
                        throw new IOException("Libera espacio para descargar e instalar el juego.");
                    }
                    ContentInstaller.download(url, archive, compressed, hash, this::showProgress);
                    boolean keepPal = PalSpanishLayout.isPrepared(root.resolve("files"));
                    ContentInstaller.unpack(archive, stage, expanded, keepPal, this::showProgress);
                    if (!assetsReady(stage) || (!keepPal && !PalSpanishLayout.isPrepared(stage.resolve("files")))) {
                        throw new IOException("Faltan archivos necesarios en el paquete.");
                    }
                    ContentInstaller.activate(root, stage, keepPal, assetHash);
                    ContentInstaller.remove(temporary);
                }
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) startGame();
                });
            } catch (Exception exception) {
                android.util.Log.e("DragoonDownload", "Content install failed", exception);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    status.setText("No se pudo instalar el contenido. " + exception.getMessage());
                    button.setText("Reintentar");
                    button.setVisibility(android.view.View.VISIBLE);
                });
            }
        });
    }

    private void showProgress(String phase, long done, long total) {
        long now = SystemClock.elapsedRealtime();
        if (done != 0 && done != total && now - lastUpdate < 100) return;
        lastUpdate = now;
        int percent = (int) (100 * done / total);
        runOnUiThread(() -> {
            progress.setProgress(percent);
            status.setText(phase + "… " + percent + "%");
        });
    }

    private void startGame() {
        Intent intent = new Intent(this, MainActivity.class);
        if (getIntent().getExtras() != null) intent.putExtras(getIntent().getExtras());
        startActivity(intent);
        finish();
    }

    private void hideBars() {
        var controller = getWindow().getInsetsController();
        if (controller != null) controller.hide(WindowInsets.Type.systemBars());
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) hideBars();
    }
}
