package com.dominoaipro;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.content.SharedPreferences;
import java.util.*;

public class MainActivity extends Activity {

    EditText handInput, leftInput, rightInput;
    TextView resultText, statsText;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("DominoStats", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(Color.rgb(20, 28, 38));

        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("DOMINO AI PRO");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(210, 235, 70));
        title.setGravity(17);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Asisten Strategi Domino");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(17);
        layout.addView(subtitle);

        addLabel(layout, "Kartu di tangan");
        handInput = addInput(layout, "Contoh: 6-6, 6-3, 2-5");

        addLabel(layout, "Angka ujung kiri meja");
        leftInput = addInput(layout, "Contoh: 6");

        addLabel(layout, "Angka ujung kanan meja");
        rightInput = addInput(layout, "Contoh: 3");

        Button analyze = new Button(this);
        analyze.setText("ANALISIS LANGKAH");
        layout.addView(analyze);

        resultText = new TextView(this);
        resultText.setTextColor(Color.WHITE);
        resultText.setTextSize(17);
        resultText.setPadding(0, 24, 0, 24);
        layout.addView(resultText);

        analyze.setOnClickListener(v -> analyzeMoves());

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

        winButton.setOnClickListener(v -> recordResult(true));
        loseButton.setOnClickListener(v -> recordResult(false));

        resetButton.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("Reset statistik?")
                .setMessage("Semua catatan menang dan kalah akan dihapus.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Reset", (d, w) -> {
                    prefs.edit().clear().apply();
                    updateStats();
                }).show()
        );

        updateStats();
        setContentView(scroll);
    }

    void addLabel(LinearLayout layout, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Color.rgb(210, 235, 70));
        label.setTextSize(16);
        label.setPadding(0, 20, 0, 8);
        layout.addView(label);
    }

    EditText addInput(LinearLayout layout, String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(16);
        layout.addView(input);
        return input;
    }

    void analyzeMoves() {
        try {
            String hand = handInput.getText().toString().trim();
            int left = Integer.parseInt(
                leftInput.getText().toString().trim());
            int right = Integer.parseInt(
                rightInput.getText().toString().trim());

            if (left < 0 || left > 6 ||
                right < 0 || right > 6) {
                throw new IllegalArgumentException(
                    "Angka ujung meja harus 0 sampai 6.");
            }

            if (hand.isEmpty()) {
                throw new IllegalArgumentException(
                    "Masukkan kartu yang kamu pegang.");
            }

            String[] tiles = hand.split(",");
            List<String> playable = new ArrayList<>();
            String bestMove = "";
            int bestScore = Integer.MIN_VALUE;

            for (String raw : tiles) {
                String tile = raw.trim();
                String[] parts = tile.split("-");

                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                        "Format kartu salah: " + tile +
                        ". Gunakan format 6-6, 6-3.");
                }

                int a = Integer.parseInt(parts[0].trim());
                int b = Integer.parseInt(parts[1].trim());

                if (a < 0 || a > 6 || b < 0 || b > 6) {
                    throw new IllegalArgumentException(
                        "Angka kartu harus 0 sampai 6: " + tile);
                }

                if (a == left || b == left ||
                    a == right || b == right) {

                    playable.add(tile);

                    int score = (a + b) * 10;

                    if (a == b) score += 8;

                    if (a == left || b == left) score += 2;
                    if (a == right || b == right) score += 2;

                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = tile;
                    }
                }
            }

            StringBuilder output = new StringBuilder();

            output.append("HASIL ANALISIS\n\n");

            if (playable.isEmpty()) {
                output.append("Tidak ada kartu yang cocok ");
                output.append("dengan kedua ujung meja.\n");
                output.append("Periksa kembali angka kartu.");
            } else {
                output.append("Kartu yang bisa dimainkan:\n");

                for (String tile : playable) {
                    output.append("• ").append(tile).append("\n");
                }

                output.append("\nREKOMENDASI:\n");
                output.append("Mainkan kartu ")
                      .append(bestMove).append("\n\n");

                output.append("Skor strategi: ")
                      .append(bestScore).append("\n");

                output.append(
                    "Penilaian awal berdasarkan jumlah pip, " +
                    "kartu kembar, dan kecocokan ujung meja.\n");

                output.append(
                    "Ini rekomendasi dasar, bukan jaminan menang.");
            }

            resultText.setText(output.toString());

        } catch (Exception e) {
            resultText.setText(
                "Periksa input kamu.\n" + e.getMessage());
        }
    }

    void recordResult(boolean win) {
        String key = win ? "wins" : "losses";
        int value = prefs.getInt(key, 0);
        prefs.edit().putInt(key, value + 1).apply();
        updateStats();
    }

    void updateStats() {
        int wins = prefs.getInt("wins", 0);
        int losses = prefs.getInt("losses", 0);
        int total = wins + losses;

        double rate = total == 0 ? 0 :
            (wins * 100.0 / total);

        statsText.setText(
            "Menang: " + wins +
            "\nKalah: " + losses +
            "\nTotal pertandingan: " + total +
            "\nPersentase menang: " +
            String.format(Locale.US, "%.1f%%", rate)
        );
    }
}
