package com.dominoaipro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.content.pm.ServiceInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    public static final String ACTION_START =
            "com.dominoaipro.START_CAPTURE";
    public static final String ACTION_STOP =
            "com.dominoaipro.STOP_CAPTURE";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String ACTION_FRAME =
            "com.dominoaipro.FRAME_READY";
    public static final String EXTRA_WIDTH = "frame_width";
    public static final String EXTRA_HEIGHT = "frame_height";
    public static final String EXTRA_PATH = "frame_path";

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler handler;
    private int density;

    private static final String CHANNEL_ID = "domino_capture";

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        density = getResources().getDisplayMetrics().densityDpi;
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_STOP.equals(intent.getAction())) {
            stopCapture();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {

            int resultCode = intent.getIntExtra(
                    EXTRA_RESULT_CODE, 0);

            Intent data;

            if (Build.VERSION.SDK_INT >= 33) {
                data = intent.getParcelableExtra(
                        EXTRA_RESULT_DATA, Intent.class);
            } else {
                //noinspection deprecation
                data = intent.getParcelableExtra(
                        EXTRA_RESULT_DATA);
            }

            if (data == null || resultCode == 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setContentTitle("Domino AI Pro")
                            .setContentText("Pembacaan layar aktif")
                            .setSmallIcon(
                                    android.R.drawable.ic_menu_view)
                            .setOngoing(true)
                            .build();

            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                        7,
                        notification,
                        ServiceInfo
                                .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(7, notification);
            }

            MediaProjectionManager manager =
                    (MediaProjectionManager)
                            getSystemService(
                                    Context.MEDIA_PROJECTION_SERVICE);

            projection = manager.getMediaProjection(
                    resultCode, data);

            if (projection == null) {
                stopSelf();
                return START_NOT_STICKY;
            }

            projection.registerCallback(
                    new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            stopCapture();
                            stopSelf();
                        }
                    },
                    handler);

            startCapture();
        }

        return START_NOT_STICKY;
    }

    private void startCapture() {

        int width =
                getResources().getDisplayMetrics().widthPixels;
        int height =
                getResources().getDisplayMetrics().heightPixels;

        if (width <= 0 || height <= 0) {
            stopSelf();
            return;
        }

        imageReader = ImageReader.newInstance(
                width, height, PixelFormat.RGBA_8888, 2);

        imageReader.setOnImageAvailableListener(
                reader -> {

                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();

                        if (image == null) return;

                        Image.Plane plane =
                                image.getPlanes()[0];

                        ByteBuffer buffer =
                                plane.getBuffer();

                        int pixelStride =
                                plane.getPixelStride();

                        int rowStride =
                                plane.getRowStride();

                        int rowPadding =
                                rowStride - pixelStride * width;

                        Bitmap padded = Bitmap.createBitmap(
                                width + rowPadding / pixelStride,
                                height,
                                Bitmap.Config.ARGB_8888);

                        buffer.rewind();
                        padded.copyPixelsFromBuffer(buffer);

                        Bitmap frame = Bitmap.createBitmap(
                                padded, 0, 0, width, height);

                        padded.recycle();

                        int outWidth = Math.min(width, 720);

                        int outHeight = Math.max(
                                1,
                                (int) (height *
                                        (outWidth / (float) width)));

                        Bitmap scaled = Bitmap.createScaledBitmap(
                                frame,
                                outWidth,
                                outHeight,
                                true);

                        File file = new File(
                                getCacheDir(),
                                "latest_screen.jpg");

                        try (FileOutputStream output =
                                     new FileOutputStream(file)) {

                            scaled.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    82,
                                    output);
                        }

                        Intent event =
                                new Intent(ACTION_FRAME);

                        event.setPackage(getPackageName());

                        event.putExtra(
                                EXTRA_WIDTH, outWidth);

                        event.putExtra(
                                EXTRA_HEIGHT, outHeight);

                        event.putExtra(
                                EXTRA_PATH,
                                file.getAbsolutePath());

                        sendBroadcast(event);

                        scaled.recycle();
                        frame.recycle();

                    } catch (Exception ignored) {
                        // Coba lagi pada frame berikutnya.
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                },
                handler);

        virtualDisplay = projection.createVirtualDisplay(
                "DominoAIProCapture",
                width,
                height,
                density,
                DisplayManager
                        .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler);
    }

    private void stopCapture() {

        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(
                    null, null);
            imageReader.close();
            imageReader = null;
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (projection != null) {
            projection.stop();
            projection = null;
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Pembacaan layar Domino AI Pro",
                            NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
