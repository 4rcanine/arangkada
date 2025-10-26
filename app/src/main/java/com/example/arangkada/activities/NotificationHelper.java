package com.example.arangkada.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.arangkada.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationHelper {

    private static final String CHANNEL_ID = "booking_updates";
    private static final String PREFS_NAME = "NotificationsData";

    public static void showBookingNotification(Context context, String title, String message, String type) {

        saveNotification(context, title, message, type);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for booking updates");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }


        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private static void saveNotification(Context context, String title, String message, String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString("notifications", "[]");

        try {
            JSONArray array = new JSONArray(json);

            // Create new notification entry
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("type", type);

            // Add new notification at the top of the list
            JSONArray newArray = new JSONArray();
            newArray.put(notification);
            for (int i = 0; i < array.length(); i++) {
                newArray.put(array.getJSONObject(i));
            }

            prefs.edit().putString("notifications", newArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
