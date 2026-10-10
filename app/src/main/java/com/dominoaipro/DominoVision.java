package com.dominoaipro;

import android.graphics.Bitmap;

public final class DominoVision {

    private DominoVision() {
        // Kelas utilitas.
    }

    public static String inspect(Bitmap frame) {

        if (frame == null) {
            return "Belum ada gambar layar.";
        }

        return "Gambar layar berhasil diterima!\n"
                + "Ukuran: "
                + frame.getWidth()
                + " x "
                + frame.getHeight()
                + "\n\n"
                + "Pengenalan kartu domino "
                + "belum diaktifkan.";
    }
}
