package com.metinsaritas.copyphone_pc;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.net.ContentHandler;

/**
 * Created by User on 09-Aug-17.
 */

public class MyNotification {
    private RemoteViews remoteViews;
    private Context context;
    private NotificationManager manager;
    private Notification notification;
    private NotificationCompat.Builder builder;
    private PendingIntent pendingIntent;
    private Intent intent;
    private int notificationId;

    public MyNotification (Context context) {
        this.context = context;

        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
        //intent = new Intent();
        intent = new Intent(context, context.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(context,0,intent,0);

        remoteViews = new RemoteViews(context.getPackageName(),R.layout.notification);

        builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.notification_icon)
                //.setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notification = builder.build();
        notification.contentView = remoteViews;
        notification.flags = Notification.FLAG_NO_CLEAR;
        notificationId = (int) System.currentTimeMillis();
    }

    public void showNotify () {
        manager.notify(notificationId, notification);
    }

    public RemoteViews getRemoteViews() {
        return remoteViews;
    }

    public void cancel() {
        manager.cancel(notificationId);
    }
}
