package com.souravgoswami.pocketpulse;

import android.content.Context;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

final class VibrationPlayer {
    private VibrationPlayer() {
    }

    static void play(Context context, ReminderSettings settings) {
        Vibrator vibrator = getVibrator(context);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        long[] pattern = settings.vibrationPattern();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, settings.vibrationAmplitudes(), -1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibrationAttributes attributes = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_NOTIFICATION)
                        .build();
                vibrator.vibrate(effect, attributes);
            } else {
                vibrator.vibrate(effect);
            }
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private static Vibrator getVibrator(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (manager != null) {
                return manager.getDefaultVibrator();
            }
        }
        return (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
