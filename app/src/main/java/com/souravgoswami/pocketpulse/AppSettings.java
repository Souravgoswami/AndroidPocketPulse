package com.souravgoswami.pocketpulse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import java.util.Locale;

final class AppSettings {
    static final int DEFAULT_ACCENT_COLOR = Color.rgb(255, 153, 0);
    static final int DEFAULT_ICON_COLOR = Color.rgb(255, 153, 0);
    static final int DEFAULT_STOP_COLOR = Color.rgb(240, 65, 125);

    private static final String PREFS = "pocket_pulse_app_settings";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_ICON_COLOR = "icon_color";
    private static final String KEY_STOP_COLOR = "stop_color";
    private static final String KEY_DARK_MODE = "dark_mode";

    final int accentColor;
    final int iconColor;
    final int stopColor;
    final boolean darkMode;

    AppSettings(int accentColor, int iconColor, int stopColor, boolean darkMode) {
        this.accentColor = accentColor;
        this.iconColor = iconColor;
        this.stopColor = stopColor;
        this.darkMode = darkMode;
    }

    static AppSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new AppSettings(
                prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR),
                prefs.getInt(KEY_ICON_COLOR, DEFAULT_ICON_COLOR),
                prefs.getInt(KEY_STOP_COLOR, DEFAULT_STOP_COLOR),
                prefs.getBoolean(KEY_DARK_MODE, false)
        );
    }

    void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_ACCENT_COLOR, accentColor)
                .putInt(KEY_ICON_COLOR, iconColor)
                .putInt(KEY_STOP_COLOR, stopColor)
                .putBoolean(KEY_DARK_MODE, darkMode)
                .apply();
    }

    static boolean isValidHexColor(String raw) {
        String value = normalizeHex(raw);
        if (value.length() != 7 || value.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'A' && c <= 'F')
                    || (c >= 'a' && c <= 'f');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    static int parseHexColor(String raw) {
        return Color.parseColor(normalizeHex(raw));
    }

    static String colorToHex(int color) {
        return String.format(Locale.US, "#%06X", 0xFFFFFF & color);
    }

    private static String normalizeHex(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.startsWith("#")) {
            value = "#" + value;
        }
        return value;
    }
}
