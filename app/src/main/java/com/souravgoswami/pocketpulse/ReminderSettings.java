package com.souravgoswami.pocketpulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

final class ReminderSettings {
    static final String PREFS = "pocket_pulse_settings";
    static final String KEY_RUNNING = "running";

    private static final String KEY_MODE = "mode";
    private static final String KEY_BURST_DURATION_MS = "burst_duration_ms";
    private static final String KEY_BURST_COUNT = "burst_count";
    private static final String KEY_BURST_GAP_MS = "burst_gap_ms";
    private static final String KEY_EXACT_INTERVAL_SEC = "exact_interval_sec";
    private static final String KEY_RANGE_MIN_SEC = "range_min_sec";
    private static final String KEY_RANGE_MAX_SEC = "range_max_sec";
    private static final String KEY_AROUND_BASE_SEC = "around_base_sec";
    private static final String KEY_AROUND_VARIATION_SEC = "around_variation_sec";

    static final String MODE_RANGE = "range";
    static final String MODE_AROUND = "around";
    static final String MODE_EXACT = "exact";

    final String mode;
    final int burstDurationMs;
    final int burstCount;
    final int burstGapMs;
    final int exactIntervalSec;
    final int rangeMinSec;
    final int rangeMaxSec;
    final int aroundBaseSec;
    final int aroundVariationSec;

    ReminderSettings(
            String mode,
            int burstDurationMs,
            int burstCount,
            int burstGapMs,
            int exactIntervalSec,
            int rangeMinSec,
            int rangeMaxSec,
            int aroundBaseSec,
            int aroundVariationSec
    ) {
        this.mode = mode;
        this.burstDurationMs = clamp(burstDurationMs, 20, 2000);
        this.burstCount = clamp(burstCount, 1, 8);
        this.burstGapMs = clamp(burstGapMs, 30, 2000);
        this.exactIntervalSec = clamp(exactIntervalSec, 3, 3600);
        this.rangeMinSec = clamp(rangeMinSec, 3, 3600);
        this.rangeMaxSec = clamp(rangeMaxSec, 3, 3600);
        this.aroundBaseSec = clamp(aroundBaseSec, 3, 3600);
        this.aroundVariationSec = clamp(aroundVariationSec, 0, 1800);
    }

    static ReminderSettings defaults() {
        return new ReminderSettings(MODE_RANGE, 250, 3, 50, 18, 15, 20, 18, 3);
    }

    static ReminderSettings load(Context context) {
        ReminderSettings defaults = defaults();
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new ReminderSettings(
                prefs.getString(KEY_MODE, defaults.mode),
                prefs.getInt(KEY_BURST_DURATION_MS, defaults.burstDurationMs),
                prefs.getInt(KEY_BURST_COUNT, defaults.burstCount),
                prefs.getInt(KEY_BURST_GAP_MS, defaults.burstGapMs),
                prefs.getInt(KEY_EXACT_INTERVAL_SEC, defaults.exactIntervalSec),
                prefs.getInt(KEY_RANGE_MIN_SEC, defaults.rangeMinSec),
                prefs.getInt(KEY_RANGE_MAX_SEC, defaults.rangeMaxSec),
                prefs.getInt(KEY_AROUND_BASE_SEC, defaults.aroundBaseSec),
                prefs.getInt(KEY_AROUND_VARIATION_SEC, defaults.aroundVariationSec)
        ).normalized();
    }

    void save(Context context) {
        ReminderSettings settings = normalized();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODE, settings.mode)
                .putInt(KEY_BURST_DURATION_MS, settings.burstDurationMs)
                .putInt(KEY_BURST_COUNT, settings.burstCount)
                .putInt(KEY_BURST_GAP_MS, settings.burstGapMs)
                .putInt(KEY_EXACT_INTERVAL_SEC, settings.exactIntervalSec)
                .putInt(KEY_RANGE_MIN_SEC, settings.rangeMinSec)
                .putInt(KEY_RANGE_MAX_SEC, settings.rangeMaxSec)
                .putInt(KEY_AROUND_BASE_SEC, settings.aroundBaseSec)
                .putInt(KEY_AROUND_VARIATION_SEC, settings.aroundVariationSec)
                .apply();
    }

    long nextDelayMs(Random random) {
        int seconds;
        if (MODE_EXACT.equals(mode)) {
            seconds = exactIntervalSec;
        } else if (MODE_AROUND.equals(mode)) {
            int min = Math.max(3, aroundBaseSec - aroundVariationSec);
            int max = Math.max(min, aroundBaseSec + aroundVariationSec);
            seconds = randomInt(random, min, max);
        } else {
            seconds = randomInt(random, Math.min(rangeMinSec, rangeMaxSec), Math.max(rangeMinSec, rangeMaxSec));
        }
        return seconds * 1000L;
    }

    long[] vibrationPattern() {
        long[] pattern = new long[burstCount * 2];
        for (int i = 0; i < burstCount; i++) {
            pattern[i * 2] = i == 0 ? 0 : burstGapMs;
            pattern[i * 2 + 1] = burstDurationMs;
        }
        return pattern;
    }

    int[] vibrationAmplitudes() {
        int[] amplitudes = new int[burstCount * 2];
        for (int i = 0; i < burstCount; i++) {
            amplitudes[i * 2] = 0;
            amplitudes[i * 2 + 1] = 180;
        }
        return amplitudes;
    }

    static boolean isRunning(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_RUNNING, false);
    }

    static void setRunning(Context context, boolean running) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_RUNNING, running)
                .apply();
    }

    ReminderSettings normalized() {
        String normalizedMode = mode;
        if (!MODE_RANGE.equals(mode) && !MODE_AROUND.equals(mode) && !MODE_EXACT.equals(mode)) {
            normalizedMode = MODE_RANGE;
        }
        int min = Math.min(rangeMinSec, rangeMaxSec);
        int max = Math.max(rangeMinSec, rangeMaxSec);
        int variation = Math.min(aroundVariationSec, Math.max(0, aroundBaseSec - 3));
        return new ReminderSettings(
                normalizedMode,
                burstDurationMs,
                burstCount,
                burstGapMs,
                exactIntervalSec,
                min,
                max,
                aroundBaseSec,
                variation
        );
    }

    private static int randomInt(Random random, int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
