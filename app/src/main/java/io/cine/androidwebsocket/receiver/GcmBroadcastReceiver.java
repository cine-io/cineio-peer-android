package io.cine.androidwebsocket.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by thomas on 9/18/14.
 */
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "GcmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
//    public void onReceive(Context context, Intent intent) {
//        Bundle extras = intent.getExtras();
//
//        Intent newIntent = new Intent(context, MyActivity.class);
//        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        newIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
////        newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
////        newIntent.setFlags(Intent.FLAG_FROM_BACKGROUND);
//        newIntent.putExtras(extras);
//        Log.d(TAG, "STARTING ACTIVITY");
//        context.startActivity(newIntent);
//    }
}
