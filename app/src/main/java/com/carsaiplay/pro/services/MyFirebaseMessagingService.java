package com.carsaiplay.pro.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.carsaiplay.pro.R;
import com.carsaiplay.pro.ui.WebViewActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "default_channel";
    private static final String CHANNEL_NAME = "Default Channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Aqui você pode enviar o token para seu servidor se necessário
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Verifica se a mensagem contém dados
        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage);
        }

        // Verifica se a mensagem contém notificação
        if (remoteMessage.getNotification() != null) {
            handleNotificationMessage(remoteMessage.getNotification());
        }
    }

    private void handleDataMessage(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");
        String url = remoteMessage.getData().get("url");

        if (title == null) title = getString(R.string.app_name);
        if (message == null) message = "";

        sendNotification(title, message, url);
    }

    private void handleNotificationMessage(RemoteMessage.Notification notification) {
        String title = notification.getTitle();
        String message = notification.getBody();

        if (title == null) title = getString(R.string.app_name);
        if (message == null) message = "";

        sendNotification(title, message, null);
    }

    private void sendNotification(String title, String message, String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        if (url != null && !url.isEmpty()) {
            intent.putExtra("url", url);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Criar canal de notificação para Android O e superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}