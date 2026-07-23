package com.peyam.app;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PeyamCallActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String from = intent.getStringExtra("peyam_from");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(PeyamFirebaseMessagingService.CALL_NOTIF_ID);
        CallActivity.dismissIfActive();

        Intent open = new Intent(context, MainActivity.class);
        open.putExtra("peyam_action", "declineCall");
        open.putExtra("peyam_from", from);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(open);
    }
}
