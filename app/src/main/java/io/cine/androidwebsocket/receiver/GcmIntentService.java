package io.cine.androidwebsocket.receiver;

import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.List;

import io.cine.androidwebsocket.MyActivity;
import io.cine.androidwebsocket.R;

/**
 * Created by thomas on 9/18/14.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static String TAG = "GcmIntentService";
    NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
//                for (int i=0; i<5; i++) {
//                    Log.i(TAG, "Working... " + (i + 1)
//                            + "/5 @ " + SystemClock.elapsedRealtime());
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                    }
//                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
//                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
                startCineActivity(extras);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
//        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void startCineActivity(Bundle extras) {
//        String action = extras.getString("action");
//        Log.v(TAG, "Got action: "+ action);
//        if(isRunning(this)) {
//            Log.v(TAG, "IS RUNNING");

        Intent intent = new Intent(this, MyActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        intent.putExtras(extras);
        startActivity(intent);
//        }else{
//            Log.v(TAG, "NOT RUNNING");
//        }
    }


    // http://stackoverflow.com/questions/13909886/starting-an-activity-from-broadcast-receiver
    public boolean isRunning(Context ctx) {
        String myPackageName = ctx.getPackageName();
        Log.v(TAG, "My package name: " + myPackageName);
        ActivityManager activityManager = (ActivityManager)
                ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks =
                activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (myPackageName.equalsIgnoreCase(
                    task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MyActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
