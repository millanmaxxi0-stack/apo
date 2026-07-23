package com.peyam.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PeyamFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CALL_CHANNEL_ID = "peyam_calls";
    private static final String MSG_CHANNEL_ID = "peyam_messages";
    public static final int CALL_NOTIF_ID = 991;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // No action needed here: the web app calls PeyamPush.getToken() itself right after
        // login and sends it to the server via /api/fcm-register (see peyam-native.js).
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) return;
        String type = data.get("type");
        if ("call".equals(type)) {
            showIncomingCall(data);
        } else if ("call-cancel".equals(type)) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(CALL_NOTIF_ID);
            CallActivity.dismissIfActive();
        } else if ("message".equals(type)) {
            showMessageNotification(data);
        }
    }

    private void showIncomingCall(Map<String, String> data) {
        createChannels();
        String from = data.get("from");
        boolean video = "true".equals(data.get("video"));

        Intent fullScreenIntent = new Intent(this, CallActivity.class);
        fullScreenIntent.putExtra("peyam_from", from);
        fullScreenIntent.putExtra("peyam_video", video);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(this, PeyamCallActionReceiver.class);
        declineIntent.putExtra("peyam_from", from);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(video ? "Incoming video call" : "Incoming call")
                .setContentText(from + " is calling you")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(0, "Decline", declinePendingIntent)
                .setAutoCancel(true)
                .setOngoing(true);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(CALL_NOTIF_ID, builder.build());

        // On many devices (and Android 10 and below), a killed app's full-screen intent isn't
        // reliably surfaced from the notification alone, so we also start the activity directly.
        // This is allowed here because it originates from a high-priority FCM data message,
        // not general background activity.
        startActivity(fullScreenIntent);
    }

    private void showMessageNotification(Map<String, String> data) {
        createChannels();
        String title = data.get("title");
        String body = data.get("body");
        String chatId = data.get("chatId");
        String from = data.get("from");

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.putExtra("peyam_action", "openChat");
        openIntent.putExtra("peyam_from", from);
        openIntent.putExtra("peyam_chatId", chatId);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int reqCode = chatId != null ? chatId.hashCode() : (from != null ? from.hashCode() : 0);
        PendingIntent pi = PendingIntent.getActivity(this, reqCode, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MSG_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(reqCode, builder.build());
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel callChannel = new NotificationChannel(CALL_CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH);
        callChannel.setDescription("Incoming voice and video calls");
        callChannel.enableLights(true);
        callChannel.setLightColor(Color.GREEN);
        callChannel.setBypassDnd(true);
        callChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(callChannel);

        NotificationChannel msgChannel = new NotificationChannel(MSG_CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH);
        msgChannel.setDescription("New chat messages");
        nm.createNotificationChannel(msgChannel);
    }
}
