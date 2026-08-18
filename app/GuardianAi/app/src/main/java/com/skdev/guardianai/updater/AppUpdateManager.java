package com.skdev.guardianai.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.skdev.guardianai.R;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * In-App Update Engine: Parses update.json from /Apk/update.json or assets,
 * verifies version codes, and displays interactive update modal.
 */
public class AppUpdateManager {

    private static final String TAG = "AppUpdateManager";

    public static void checkForUpdates(Activity activity, boolean showToastIfLatest) {
        new Thread(() -> {
            try {
                // Read update.json from assets or fallback
                InputStream is = activity.getAssets().open("update.json");
                byte[] buffer = new byte[is.available()];
                is.read(buffer);
                is.close();

                String json = new String(buffer, StandardCharsets.UTF_8);
                UpdateInfo updateInfo = new Gson().fromJson(json, UpdateInfo.class);

                activity.runOnUiThread(() -> {
                    int currentVersionCode = 1;
                    try {
                        PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                        currentVersionCode = pInfo.versionCode;
                    } catch (PackageManager.NameNotFoundException ignored) {}

                    if (updateInfo != null && updateInfo.getVersionCode() > currentVersionCode) {
                        showUpdateDialog(activity, updateInfo);
                    } else {
                        if (showToastIfLatest) {
                            Toast.makeText(activity, "✨ GuardianAI is up to date (v" +
                                    (updateInfo != null ? updateInfo.getVersionName() : "1.0.0") + ")", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error checking update: " + e.getMessage());
                activity.runOnUiThread(() -> {
                    if (showToastIfLatest) {
                        Toast.makeText(activity, "Unable to check for updates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private static void showUpdateDialog(Activity activity, UpdateInfo updateInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_app_update, null, false);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tv_dialog_update_title);
        TextView tvMeta = view.findViewById(R.id.tv_dialog_update_meta);
        TextView tvChangelog = view.findViewById(R.id.tv_dialog_changelog);
        Button btnLater = view.findViewById(R.id.btn_dialog_update_later);
        Button btnNow = view.findViewById(R.id.btn_dialog_update_now);

        tvTitle.setText(updateInfo.getTitle());
        tvMeta.setText("Release: " + updateInfo.getReleaseDate() + " • " + updateInfo.getFileSizeFormatted());

        if (updateInfo.getChangelog() != null && !updateInfo.getChangelog().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String item : updateInfo.getChangelog()) {
                sb.append("• ").append(item).append("\n");
            }
            tvChangelog.setText(sb.toString().trim());
        }

        btnLater.setOnClickListener(v -> dialog.dismiss());
        btnNow.setOnClickListener(v -> {
            Toast.makeText(activity, "Downloading " + updateInfo.getApkFileName() + "...", Toast.LENGTH_LONG).show();
            if (updateInfo.getDownloadUrl() != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.getDownloadUrl()));
                activity.startActivity(intent);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
