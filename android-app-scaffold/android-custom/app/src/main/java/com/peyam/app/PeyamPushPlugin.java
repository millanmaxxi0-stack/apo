package com.peyam.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.firebase.messaging.FirebaseMessaging;

@CapacitorPlugin(name = "PeyamPush")
public class PeyamPushPlugin extends Plugin {

    // Called from JS (peyam-native.js) right after login to get the FCM device token,
    // which the web app then POSTs to /api/fcm-register.
    @PluginMethod
    public void getToken(PluginCall call) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                call.reject("Could not get FCM token", task.getException());
                return;
            }
            JSObject ret = new JSObject();
            ret.put("token", task.getResult());
            call.resolve(ret);
        });
    }

    // Called from JS on startup to check whether the app was launched by tapping a
    // notification (e.g. app was fully closed) so it knows to accept/decline a call
    // or open a chat once login finishes.
    @PluginMethod
    public void getPendingAction(PluginCall call) {
        JSObject action = MainActivity.consumePendingAction();
        call.resolve(action != null ? action : new JSObject());
    }

    // Called from MainActivity when the app is already running and a new notification is tapped.
    public void firePendingAction(JSObject action) {
        notifyListeners("peyamAction", action);
    }
}
