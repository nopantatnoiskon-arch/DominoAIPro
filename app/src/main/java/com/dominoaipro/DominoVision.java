package com.dominoaipro;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public final class DominoVision {

    private DominoVision() {
    }

    public static String inspect(Bitmap frame) {

        if (frame == null) {
            return "Belum ada gambar layar.";
        }

        int width = frame.getWidth();
        int height = frame.getHeight();

        StringBuilder result = new StringBuilder();

        result.append("DOMINO AI PRO - VISION\n\n");
        result.append("Gambar: ")
                .append(width)
                .append(" x ")
                .append(height)
                .append("\n\n");

        if (width < 100 || height < 100) {
            return result.append(
                    "Gambar terlalu kecil untuk diperiksa.")
                    .toString();
        }

        /*
         * Kurangi resolusi pemeriksaan agar pemrosesan
         * tetap ringan di HP.
         */
        int step = Math.max(
                1, Math.min(width, height) / 350);

        int[] columnScores = new int[width];

        int brightPixels = 0;
        int redPixels = 0;
        int darkPixels = 0;

        /*
         * Hitung karakteristik warna pada gambar.
         */
        for (int y = 0; y < height; y += step) {

            for (int x = 0; x < width; x += step) {

                int pixel = frame.getPixel(x, y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                if (isBrightTile(pixel)) {

                    brightPixels++;
                    columnScores[x]++;
                }

                if (isRed(pixel)) {
                    redPixels++;
                }

                if (isDark(pixel)) {
                    darkPixels++;
                }
            }
        }

        result.append("Pemeriksaan warna:\n");

        result.append("Area terang: ")
                .append(brightPixels)
                .append(" sampel\n");

        result.append("Sampel merah: ")
                .append(redPixels)
                .append("\n");

        result.append("Sampel gelap: ")
                .append(darkPixels)
                .append("\n\n");

        /*
         * Cari kelompok kolom yang memiliki banyak
         * piksel terang. Ini hanya kandidat area kartu,
         * bukan hasil pengenalan kartu yang pasti.
         */
        List<String> candidates = new ArrayList<>();

        int threshold = Math.max(
                3, height / (step * 100));

        int start = -1;

        for (int x = 0; x < width; x++) {

            boolean active =
                    columnScores[x] >= threshold;

            if (active && start == -1) {
                start = x;
            }

            if (!active && start != -1) {

                int end = x - 1;
                int span = end - start + 1;

                if (span >= Math.max(3, width / 100)) {

                    candidates.add(
                            start + "-" + end);
                }

                start = -1;
            }
        }

        if (start != -1) {

            candidates.add(
                    start + "-" + (width - 1));
        }

        result.append("Kandidat area terang:\n");

        if (candidates.isEmpty()) {

            result.append(
                    "Belum ditemukan kandidat yang jelas.\n");

        } else {

            int limit = Math.min(
                    candidates.size(), 20);

            for (int i = 0; i < limit; i++) {

                result.append("• Kolom ")
                        .append(candidates.get(i))
                        .append("\n");
            }
        }

        result.append(
                "\nStatus: deteksi awal berjalan.\n");

        result.append(
                "Angka kartu belum dapat dipastikan.\n");

        result.append(
                "Perlu pengujian dengan gambar meja HGI.");

        return result.toString();
    }

    private static boolean isBrightTile(int pixel) {

        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        return r > 170
                && g > 150
                && b > 140
                && max - min < 100;
    }

    private static boolean isRed(int pixel) {

        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        return r > 100
                && r > g * 1.25
                && r > b * 1.15;
    }

    private static boolean isDark(int pixel) {

        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);

        return r < 75
                && g < 75
                && b < 75;
    }
            }
