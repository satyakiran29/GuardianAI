package com.android.sheguard.util;

import android.os.AsyncTask;
import android.util.Log;
import com.android.sheguard.api.NotificationAPI;

public class FirebaseUtil {

    public static void updateToken() {
        // Managed via Django Backend & Supabase
    }

    @SuppressWarnings("deprecation")
    public static class SendNotificationTask extends AsyncTask<Void, Void, String> {
        public SendNotificationTask(NotificationAPI notificationApiService, String userToken, String title, String message) {
            Log.i("Notification", "Dispatching notification via Supabase & Backend: " + title);
        }

        @Override
        protected String doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(String accessToken) {}
    }
}
