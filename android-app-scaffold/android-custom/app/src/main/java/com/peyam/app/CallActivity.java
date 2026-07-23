package com.peyam.app;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {

    // Lets the FCM service dismiss this screen instantly if the caller hangs up before we answer.
    private static CallActivity activeInstance;

    private Ringtone ringtone;
    private Vibrator vibrator;
    private String from;
    private boolean isVideo;

    public static void dismissIfActive() {
        if (activeInstance != null) activeInstance.finishAndRemoveTask();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeInstance = this;

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_call);

        from = getIntent().getStringExtra("peyam_from");
        isVideo = getIntent().getBooleanExtra("peyam_video", false);

        ((TextView) findViewById(R.id.call_caller_name)).setText(from);
        ((TextView) findViewById(R.id.call_status)).setText(isVideo ? "Incoming video call" : "Incoming call");

        findViewById(R.id.call_accept_btn).setOnClickListener(v -> {
            stopRinging();
            Intent open = new Intent(this, MainActivity.class);
            open.putExtra("peyam_action", "acceptCall");
            open.putExtra("peyam_from", from);
            open.putExtra("peyam_video", isVideo);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(open);
            finishAndRemoveTask();
        });

        findViewById(R.id.call_decline_btn).setOnClickListener(v -> {
            stopRinging();
            Intent open = new Intent(this, MainActivity.class);
            open.putExtra("peyam_action", "declineCall");
            open.putExtra("peyam_from", from);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(open);
            finishAndRemoveTask();
        });

        startRinging();
    }

    private void startRinging() {
        try {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
                }
                ringtone.play();
            }
        } catch (Exception ignored) {}
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(new long[]{0, 800, 800}, 0);
    }

    private void stopRinging() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRinging();
        if (activeInstance == this) activeInstance = null;
    }
}
