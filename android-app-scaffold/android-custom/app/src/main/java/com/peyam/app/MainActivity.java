package com.peyam.app;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {

    // Holds the action that launched the app while it was fully closed, until the JS
    // bridge asks for it via PeyamPush.getPendingAction().
    private static JSObject pendingAction = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PeyamPushPlugin.class);
        super.onCreate(savedInstanceState);
        JSObject action = intentToAction(getIntent());
        if (action != null) pendingAction = action;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        JSObject action = intentToAction(intent);
        if (action == null) return;
        PluginHandle handle = getBridge().getPlugin("PeyamPush");
        if (handle != null && handle.getInstance() != null) {
            ((PeyamPushPlugin) handle.getInstance()).firePendingAction(action);
        } else {
            pendingAction = action;
        }
    }

    private JSObject intentToAction(Intent intent) {
        if (intent == null || intent.getStringExtra("peyam_action") == null) return null;
        JSObject o = new JSObject();
        o.put("type", intent.getStringExtra("peyam_action"));
        o.put("from", intent.getStringExtra("peyam_from"));
        o.put("chatId", intent.getStringExtra("peyam_chatId"));
        o.put("video", intent.getBooleanExtra("peyam_video", false));
        intent.removeExtra("peyam_action");
        return o;
    }

    public static JSObject consumePendingAction() {
        JSObject a = pendingAction;
        pendingAction = null;
        return a;
    }
}
