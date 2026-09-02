package com.android.sheguard.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.android.sheguard.BuildConfig;
import com.android.sheguard.R;
import com.android.sheguard.common.Constants;
import com.android.sheguard.databinding.DialogDownloadProgressBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppUpdateManager {

    private static final String TAG = "AppUpdateManager";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void checkForUpdates(final Activity activity, final boolean showUpToDateToast) {
        if (activity == null || activity.isFinishing()) return;

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(Constants.UPDATE_JSON_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream in = connection.getInputStream();
                    java.util.Scanner scanner = new java.util.Scanner(in).useDelimiter("\\A");
                    String jsonStr = scanner.hasNext() ? scanner.next() : "";
                    scanner.close();

                    JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();
                    final int serverVersionCode = jsonObject.has("versionCode") ? jsonObject.get("versionCode").getAsInt() : 1;
                    final String serverVersionName = jsonObject.has("versionName") ? jsonObject.get("versionName").getAsString() : "1.0.0";
                    final String downloadUrl = jsonObject.has("downloadUrl") ? jsonObject.get("downloadUrl").getAsString() : "";

                    StringBuilder changelogBuilder = new StringBuilder();
                    if (jsonObject.has("changelog") && jsonObject.get("changelog").isJsonArray()) {
                        JsonArray changelogArray = jsonObject.getAsJsonArray("changelog");
                        for (JsonElement element : changelogArray) {
                            changelogBuilder.append("• ").append(element.getAsString()).append("\n");
                        }
                    }
                    final String changelog = changelogBuilder.toString().trim();

                    int currentVersionCode = BuildConfig.VERSION_CODE;
                    String currentVersionName = BuildConfig.VERSION_NAME;

                    mainHandler.post(() -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;

                        if (serverVersionCode > currentVersionCode || isNewerVersion(serverVersionName, currentVersionName)) {
                            showUpdateAvailableDialog(activity, serverVersionName, currentVersionName, downloadUrl, changelog);
                        } else {
                            if (showUpToDateToast) {
                                Toast.makeText(activity, activity.getString(R.string.app_is_up_to_date, currentVersionName), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (showUpToDateToast && !activity.isFinishing()) {
                            Toast.makeText(activity, "Unable to check for updates (HTTP " + ")", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (showUpToDateToast && !activity.isFinishing()) {
                        Toast.makeText(activity, "Unable to check for updates. Please check network.", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static boolean isNewerVersion(String serverVersion, String currentVersion) {
        try {
            String[] serverParts = serverVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            int length = Math.max(serverParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int serverPart = i < serverParts.length ? Integer.parseInt(serverParts[i].replaceAll("[^0-9]", "")) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i].replaceAll("[^0-9]", "")) : 0;
                if (serverPart > currentPart) return true;
                if (serverPart < currentPart) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void showUpdateAvailableDialog(final Activity activity, final String serverVersion, final String currentVersion, final String downloadUrl, final String changelog) {
        String msg = "A new version of GuardianAI is available!\n\n"
                + "• Latest Version: v" + serverVersion + "\n"
                + "• Current Version: v" + currentVersion + "\n\n"
                + "What's New:\n" + changelog;

        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.update_available_title) + " (v" + serverVersion + ")")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(activity.getString(R.string.btn_download_update), (dialog, which) -> {
                    startInAppDownload(activity, downloadUrl, serverVersion);
                })
                .setNegativeButton(activity.getString(R.string.btn_later), null)
                .show();
    }

    public static void startInAppDownload(final Activity activity, final String downloadUrl, final String serverVersion) {
        if (activity == null || activity.isFinishing()) return;

        DialogDownloadProgressBinding binding = DialogDownloadProgressBinding.inflate(LayoutInflater.from(activity));
        binding.tvDownloadTitle.setText(activity.getString(R.string.downloading_update) + " (v" + serverVersion + ")");
        binding.tvDownloadSubtitle.setText("Downloading APK directly to device...");
        binding.progressIndicator.setProgress(0);
        binding.tvDownloadPercent.setText("0%");
        binding.tvDownloadSize.setText("0.0 MB / 0.0 MB");

        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity)
                .setView(binding.getRoot())
                .setCancelable(false)
                .create();

        progressDialog.show();

        AtomicBoolean isCancelled = new AtomicBoolean(false);

        binding.btnCancelDownload.setOnClickListener(v -> {
            isCancelled.set(true);
            progressDialog.dismiss();
            Toast.makeText(activity, "Download cancelled", Toast.LENGTH_SHORT).show();
        });

        executor.execute(() -> {
            HttpURLConnection connection = null;
            InputStream in = null;
            FileOutputStream out = null;
            try {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                }

                int fileLength = connection.getContentLength();
                in = connection.getInputStream();

                File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) {
                    downloadDir = activity.getFilesDir();
                }
                File apkFile = new File(downloadDir, "GuardianAI-v" + serverVersion + ".apk");
                if (apkFile.exists()) {
                    apkFile.delete();
                }

                out = new FileOutputStream(apkFile);
                byte[] data = new byte[8192];
                long total = 0;
                int count;
                long lastUpdateTime = 0;

                while ((count = in.read(data)) != -1) {
                    if (isCancelled.get()) {
                        out.close();
                        apkFile.delete();
                        return;
                    }
                    total += count;
                    out.write(data, 0, count);

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime > 100 || total == fileLength) {
                        lastUpdateTime = currentTime;
                        final long downloadedBytes = total;
                        final int percent = fileLength > 0 ? (int) ((downloadedBytes * 100) / fileLength) : 0;
                        final double downloadedMb = downloadedBytes / (1024.0 * 1024.0);
                        final double totalMb = fileLength > 0 ? fileLength / (1024.0 * 1024.0) : 0;

                        mainHandler.post(() -> {
                            if (!progressDialog.isShowing() || activity.isFinishing()) return;
                            binding.progressIndicator.setProgress(percent);
                            binding.tvDownloadPercent.setText(percent + "%");
                            if (totalMb > 0) {
                                binding.tvDownloadSize.setText(String.format(Locale.getDefault(), "%.1f MB / %.1f MB", downloadedMb, totalMb));
                            } else {
                                binding.tvDownloadSize.setText(String.format(Locale.getDefault(), "%.1f MB", downloadedMb));
                            }
                        });
                    }
                }

                out.flush();
                out.close();
                in.close();

                mainHandler.post(() -> {
                    if (activity.isFinishing()) return;
                    progressDialog.dismiss();
                    Toast.makeText(activity, activity.getString(R.string.download_complete), Toast.LENGTH_LONG).show();
                    installApk(activity, apkFile);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download failed: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    if (activity.isFinishing()) return;
                    progressDialog.dismiss();
                    Toast.makeText(activity, activity.getString(R.string.download_failed) + " (" + e.getMessage() + ")", Toast.LENGTH_LONG).show();
                });
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                } catch (Exception ignored) {}
                if (connection != null) connection.disconnect();
            }
        });
    }

    public static void installApk(Activity activity, File apkFile) {
        if (activity == null || apkFile == null || !apkFile.exists()) {
            Log.e(TAG, "Cannot install: APK file does not exist");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(activity, activity.getString(R.string.install_permission_msg), Toast.LENGTH_LONG).show();
                    Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(permissionIntent);
                    return;
                }
            }

            Uri apkUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".fileprovider",
                    apkFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Failed to launch APK installer: " + e.getMessage(), e);
            Toast.makeText(activity, "Failed to open installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
