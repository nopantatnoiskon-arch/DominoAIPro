package com.dominoaipro;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_CAPTURE = 910;

    private EditText handInput;
    private EditText leftInput;
    private EditText rightInput;

    private TextView resultText;
    private TextView statsText;
    private TextView captureStatus;

    private ImageView previewImage;

    private SharedPreferences prefs;

    private boolean receiverRegistered = false;
    private boolean activityVisible = false;

    private long lastSavedFrameTime = 0;

    private final BroadcastReceiver frameReceiver =
            new BroadcastReceiver() {

        @Override
        public void onReceive(
                Context context, Intent intent) {

            if (intent == null) {
                return;
            }

            String path = intent.getStringExtra(
                    ScreenCaptureService.EXTRA_PATH);

            if (path == null) {
                return;
            }

            File source = new File(path);

            if (!source.exists()) {
                return;
            }

            /*
             * Ketika aplikasi berada di latar belakang,
             * simpan gambar terakhir secara terpisah.
             */
            if (!activityVisible) {

                long now = System.currentTimeMillis();

                // Batasi penyimpanan maksimal sekitar
                // dua kali per detik.
                if (now - lastSavedFrameTime < 500) {
                    return;
                }

                lastSavedFrameTime = now;

                saveLastFrame(source);
                return;
            }

            /*
             * Saat aplikasi tampil di depan, jangan
             * mengganti pratinjau dengan layar aplikasi
             * sendiri.
             */
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                "DominoStats", MODE_PRIVATE);

        createInterface();
    }

    private void createInterface() {

        ScrollView scroll = new ScrollView(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 24);
        layout.setBackgroundColor(
                Color.rgb(20, 28, 38));

        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("DOMINO AI PRO");
        title.setTextSize(28);
        title.setTextColor(
                Color.rgb(210, 235, 70));
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Asisten Strategi Domino V2");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        Button startButton = new Button(this);
        startButton.setText("MULAI BACA LAYAR");
        layout.addView(startButton);

        Button stopButton = new Button(this);
        stopButton.setText("HENTIKAN BACA LAYAR");
        layout.addView(stopButton);

        captureStatus = new TextView(this);
        captureStatus.setTextColor(Color.WHITE);
        captureStatus.setTextSize(16);
        captureStatus.setPadding(0, 12, 0, 12);
        captureStatus.setText(
                "Pembacaan layar belum dimulai.");
        layout.addView(captureStatus);

        previewImage = new ImageView(this);
        previewImage.setAdjustViewBounds(true);
        previewImage.setMaxHeight(700);
        layout.addView(previewImage);

        startButton.setOnClickListener(v -> {

            MediaProjectionManager manager =
                    (MediaProjectionManager)
                    getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);

            if (manager != null) {

                startActivityForResult(
                        manager.createScreenCaptureIntent(),
                        REQUEST_CAPTURE);

            } else {

                captureStatus.setText(
                        "Layanan tangkapan layar tidak tersedia.");
            }
        });

        stopButton.setOnClickListener(v -> {

            Intent stopIntent = new Intent(
                    this, ScreenCaptureService.class);

            stopIntent.setAction(
                    ScreenCaptureService.ACTION_STOP);

            startService(stopIntent);

            captureStatus.setText(
                    "Pembacaan layar dihentikan.");

            previewImage.setImageDrawable(null);
        });

        addLabel(layout, "Kartu di tangan");

        handInput = addInput(
                layout, "Contoh: 6-6, 6-3, 2-5");

        addLabel(layout, "Angka ujung kiri meja");

        leftInput = addInput(layout, "Contoh: 6");

        addLabel(layout, "Angka ujung kanan meja");

        rightInput = addInput(layout, "Contoh: 3");

        Button analyzeButton = new Button(this);
        analyzeButton.setText("ANALISIS LANGKAH");
        layout.addView(analyzeButton);

        resultText = new TextView(this);
        resultText.setTextColor(Color.WHITE);
        resultText.setTextSize(17);
        resultText.setPadding(0, 20, 0, 20);
        layout.addView(resultText);

        analyzeButton.setOnClickListener(
                v -> analyzeMoves());

        addLabel(layout, "Catatan Pertandingan");

        statsText = new TextView(this);
        statsText.setTextColor(Color.WHITE);
        statsText.setTextSize(16);
        layout.addView(statsText);

        Button winButton = new Button(this);
        winButton.setText("CATAT MENANG");
        layout.addView(winButton);

        Button loseButton = new Button(this);
        loseButton.setText("CATAT KALAH");
        layout.addView(loseButton);

        Button resetButton = new Button(this);
        resetButton.setText("RESET STATISTIK");
        layout.addView(resetButton);

        winButton.setOnClickListener(
                v -> recordResult(true));

        loseButton.setOnClickListener(
                v -> recordResult(false));

        resetButton.setOnClickListener(v -> {

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Reset statistik?")
                    .setMessage(
                            "Semua catatan pertandingan akan dihapus.")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton(
                            "Reset", (dialog, which) -> {

                        prefs.edit().clear().apply();
                        updateStats();
                    })
                    .show();
        });

        updateStats();

        setContentView(scroll);
    }

    private void addLabel(
            LinearLayout layout, String text) {

        TextView label = new TextView(this);

        label.setText(text);
        label.setTextColor(
                Color.rgb(210, 235, 70));
        label.setTextSize(16);
        label.setPadding(0, 20, 0, 8);

        layout.addView(label);
    }

    private EditText addInput(
            LinearLayout layout, String hint) {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(16);

        layout.addView(input);

        return input;
    }

    /*
     * Menyimpan salinan gambar terakhir.
     * File sumber berasal dari ScreenCaptureService.
     */
    private void saveLastFrame(File source) {

        File destination = new File(
                getFilesDir(), "last_game_frame.jpg");

        File temporary = new File(
                getFilesDir(), "last_game_frame.tmp");

        try (
                FileInputStream input =
                        new FileInputStream(source);

                FileOutputStream output =
                        new FileOutputStream(temporary)
        ) {

            byte[] buffer = new byte[8192];
            int length;

            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();

        } catch (Exception e) {

            android.util.Log.e(
                    "DominoAIPro",
                    "Gagal menyimpan gambar terakhir",
                    e);

            temporary.delete();
            return;
        }

        if (temporary.exists()
                && temporary.length() > 0) {

            if (destination.exists()) {
                destination.delete();
            }

            if (!temporary.renameTo(destination)) {
                android.util.Log.e(
                        "DominoAIPro",
                        "Gagal memperbarui gambar terakhir");
            }
        }
    }

    private void registerFrameReceiver() {

        if (receiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(
                ScreenCaptureService.ACTION_FRAME);

        if (Build.VERSION.SDK_INT >= 33) {

            registerReceiver(
                    frameReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED);

        } else {

            registerReceiver(
                    frameReceiver, filter);
        }

        receiverRegistered = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerFrameReceiver();

        /*
         * Muat gambar yang disimpan ketika kita
         * sedang berada di HGI.
         */
        File savedFrame = new File(
                getFilesDir(), "last_game_frame.jpg");

        if (savedFrame.exists()) {

            Bitmap bitmap = BitmapFactory.decodeFile(
                    savedFrame.getAbsolutePath());

            if (bitmap != null) {

                previewImage.setImageBitmap(bitmap);

                captureStatus.setText(
                        DominoVision.inspect(bitmap));
            }
        }

        activityVisible = true;
    }

    @Override
    protected void onPause() {

        activityVisible = false;

        // Receiver sengaja tidak dilepas di sini.
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (receiverRegistered) {

            try {
                unregisterReceiver(frameReceiver);

            } catch (IllegalArgumentException ignored) {
            }

            receiverRegistered = false;
        }

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode, resultCode, data);

        if (requestCode != REQUEST_CAPTURE) {
            return;
        }

        if (resultCode == RESULT_OK && data != null) {

            Intent startIntent = new Intent(
                    this, ScreenCaptureService.class);

            startIntent.setAction(
                    ScreenCaptureService.ACTION_START);

            startIntent.putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    resultCode);

            startIntent.putExtra(
                    ScreenCaptureService.EXTRA_RESULT_DATA,
                    data);

            if (Build.VERSION.SDK_INT >= 26) {

                startForegroundService(startIntent);

            } else {

                startService(startIntent);
            }

            captureStatus.setText(
                    "Izin diberikan. Menunggu gambar layar...");

        } else {

            captureStatus.setText(
                    "Izin tangkapan layar tidak diberikan.");
        }
    }

    private void analyzeMoves() {

        try {

            String hand = handInput.getText()
                    .toString().trim();

            if (hand.isEmpty()) {
                throw new IllegalArgumentException(
                        "Masukkan kartu yang kamu pegang.");
            }

            int left = Integer.parseInt(
                    leftInput.getText()
                            .toString().trim());

            int right = Integer.parseInt(
                    rightInput.getText()
                            .toString().trim());

            if (left < 0 || left > 6
                    || right < 0 || right > 6) {

                throw new IllegalArgumentException(
                        "Angka ujung meja harus 0 sampai 6.");
            }

            String[] tiles = hand.split(",");

            StringBuilder output =
                    new StringBuilder();

            String bestMove = "";
            int bestScore = Integer.MIN_VALUE;

            for (String raw : tiles) {

                String tile = raw.trim();
                String[] parts = tile.split("-");

                if (parts.length != 2) {

                    throw new IllegalArgumentException(
                            "Format kartu salah: " + tile);
                }

                int a = Integer.parseInt(
                        parts[0].trim());

                int b = Integer.parseInt(
                        parts[1].trim());

                if (a < 0 || a > 6
                        || b < 0 || b > 6) {

                    throw new IllegalArgumentException(
                            "Angka kartu harus 0 sampai 6.");
                }

                if (a == left || b == left
                        || a == right || b == right) {

                    int score = (a + b) * 10;

                    if (a == b) {
                        score += 8;
                    }

                    if (a == left || b == left) {
                        score += 2;
                    }

                    if (a == right || b == right) {
                        score += 2;
                    }

                    output.append("• ")
                            .append(tile)
                            .append("\n");

                    if (score > bestScore) {

                        bestScore = score;
                        bestMove = tile;
                    }
                }
            }

            if (bestMove.isEmpty()) {

                resultText.setText(
                        "Tidak ditemukan kartu yang cocok "
                        + "dengan kedua ujung meja.");

            } else {

                resultText.setText(
                        "KARTU YANG BISA DIMAINKAN\n"
                        + output
                        + "\nREKOMENDASI: "
                        + bestMove
                        + "\nSkor dasar: " + bestScore
                        + "\nBelum memperhitungkan seluruh "
                        + "strategi permainan.");
            }

        } catch (Exception e) {

            resultText.setText(
                    "Periksa input kamu.\n"
                    + e.getMessage());
        }
    }

    private void recordResult(boolean win) {

        String key = win ? "wins" : "losses";

        int value = prefs.getInt(key, 0);

        prefs.edit()
                .putInt(key, value + 1)
                .apply();

        updateStats();
    }

    private void updateStats() {

        int wins = prefs.getInt("wins", 0);
        int losses = prefs.getInt("losses", 0);

        int total = wins + losses;

        double rate = total == 0
                ? 0
                : wins * 100.0 / total;

        statsText.setText(
                "Menang: " + wins
                + "\nKalah: " + losses
                + "\nTotal pertandingan: " + total
                + "\nPersentase menang: "
                + String.format(
                        Locale.US, "%.1f%%", rate));
    }
                 }
