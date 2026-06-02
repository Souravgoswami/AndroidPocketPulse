package com.souravgoswami.pocketpulse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 15;
    private static final String APP_VERSION = "0.0.2";

    private AppSettings appSettings;
    private int colorPage;
    private int colorCard;
    private int colorText;
    private int colorMuted;
    private int colorBorder;
    private int colorInput;
    private int colorInputDisabled;
    private int colorDisabledText;

    private TextView statusText;
    private TextView batteryStatusText;
    private TextView batteryDetailText;
    private Button startStopButton;
    private Button batterySettingsButton;
    private CheckBox highReliabilityInput;
    private Button applyButton;
    private Button previewButton;

    private RadioButton rangeRadio;
    private RadioButton aroundRadio;
    private RadioButton exactRadio;

    private EditText burstDurationInput;
    private EditText burstCountInput;
    private EditText burstGapInput;
    private EditText rangeMinInput;
    private EditText rangeMaxInput;
    private EditText aroundBaseInput;
    private EditText aroundVariationInput;
    private EditText exactIntervalInput;

    private String selectedMode = ReminderSettings.MODE_RANGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContentView());
        loadSettings();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRunningState();
        refreshBatterySafeState();
    }

    private View buildContentView() {
        loadThemeColors();
        applySystemBars();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(true);
        scrollView.setBackgroundColor(colorPage);
        applySystemBarPadding(scrollView);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(header());

        TextView subtitle = text("Quiet, repeated vibration bursts so your phone still feels present in your pocket.", 15, colorMuted, Typeface.NORMAL);
        subtitle.setPadding(0, dp(8), 0, dp(18));
        root.addView(subtitle);

        root.addView(statusCard());
        root.addView(batterySafeCard());
        root.addView(section("Pulse", pulseSettings()));
        root.addView(section("Interval", intervalSettings()));
        root.addView(actions());

        TextView footer = text("Tip: keep the running notification enabled. Some phones may also need battery optimization disabled for this app.", 13, colorMuted, Typeface.NORMAL);
        footer.setPadding(0, dp(14), 0, 0);
        root.addView(footer);

        return scrollView;
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = text("P", 20, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(appSettings.iconColor, appSettings.iconColor, 8));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.rightMargin = dp(12);
        header.addView(icon, iconParams);

        TextView title = text("Pocket Pulse", 30, appSettings.accentColor, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(title, titleParams);

        Button menuButton = compactButton("Menu");
        menuButton.setOnClickListener(this::showMainMenu);
        header.addView(menuButton);

        return header;
    }

    private void showMainMenu(View anchor) {
        PopupWindow popup = new PopupWindow(this);
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        menu.setBackground(rounded(colorCard, colorBorder, 8));

        menu.addView(menuItem("App Settings", v -> {
            popup.dismiss();
            showAppSettingsDialog();
        }));
        menu.addView(menuItem("About", v -> {
            popup.dismiss();
            showAboutDialog();
        }));
        menu.addView(menuItem("Version", v -> {
            popup.dismiss();
            showVersionDialog();
        }));
        menu.addView(menuItem("Exit", v -> {
            popup.dismiss();
            exitApp();
        }));

        popup.setContentView(menu);
        popup.setWidth(dp(220));
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setBackgroundDrawable(rounded(colorCard, colorBorder, 8));
        popup.setOutsideTouchable(true);
        popup.setFocusable(true);
        popup.showAsDropDown(anchor, -dp(156), dp(6));
    }

    private void showAppSettingsDialog() {
        LinearLayout content = dialogContent();

        TextView title = dialogTitle("App Settings");
        content.addView(title);

        TextView hint = text("Use #RRGGBB colours. These change the app UI immediately.", 14, colorMuted, Typeface.NORMAL);
        hint.setPadding(0, 0, 0, dp(12));
        content.addView(hint);

        EditText accentInput = colorValueInput(AppSettings.colorToHex(appSettings.accentColor));
        content.addView(colorDialogField("Accent colour", accentInput));

        EditText iconInput = colorValueInput(AppSettings.colorToHex(appSettings.iconColor));
        content.addView(colorDialogField("Icon colour", iconInput));

        EditText stopInput = colorValueInput(AppSettings.colorToHex(appSettings.stopColor));
        content.addView(colorDialogField("Stop button colour", stopInput));

        CheckBox darkModeInput = new CheckBox(this);
        darkModeInput.setText("Dark mode");
        darkModeInput.setTextSize(16);
        darkModeInput.setTextColor(colorText);
        darkModeInput.setChecked(appSettings.darkMode);
        darkModeInput.setButtonTintList(ColorStateList.valueOf(appSettings.accentColor));
        content.addView(darkModeInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(view -> {
            styleDialog(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                if (!AppSettings.isValidHexColor(accentInput.getText().toString())
                        || !AppSettings.isValidHexColor(iconInput.getText().toString())
                        || !AppSettings.isValidHexColor(stopInput.getText().toString())) {
                    Toast.makeText(this, "Use colours like #FF9900", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AppSettings(
                        AppSettings.parseHexColor(accentInput.getText().toString()),
                        AppSettings.parseHexColor(iconInput.getText().toString()),
                        AppSettings.parseHexColor(stopInput.getText().toString()),
                        darkModeInput.isChecked()
                ).save(this);
                refreshServiceNotificationIfRunning();
                dialog.dismiss();
                rebuildUi();
            });
        });
        dialog.show();
    }

    private void showAboutDialog() {
        LinearLayout content = dialogContent();
        content.addView(dialogTitle("About"));
        TextView body = text(
                "Pocket Pulse keeps your phone tactile with small configurable vibration bursts, so it still feels present in your pocket.",
                15,
                colorText,
                Typeface.NORMAL
        );
        content.addView(body);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("OK", null)
                .create();
        dialog.setOnShowListener(view -> styleDialog(dialog));
        dialog.show();
    }

    private void showVersionDialog() {
        LinearLayout content = dialogContent();
        content.addView(dialogTitle("Version"));
        TextView body = text("Version " + APP_VERSION, 16, colorText, Typeface.BOLD);
        content.addView(body);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("OK", null)
                .create();
        dialog.setOnShowListener(view -> styleDialog(dialog));
        dialog.show();
    }

    private void exitApp() {
        if (ReminderSettings.isRunning(this)) {
            ReminderSettings.setRunning(this, false);
            Intent intent = new Intent(this, VibeReminderService.class);
            intent.setAction(VibeReminderService.ACTION_STOP);
            startService(intent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }

    private View statusCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView label = text("Status", 13, appSettings.accentColor, Typeface.BOLD);
        card.addView(label);

        statusText = text("Stopped", 24, colorText, Typeface.BOLD);
        statusText.setPadding(0, dp(6), 0, dp(14));
        card.addView(statusText);

        startStopButton = button("Start reminders", true);
        startStopButton.setOnClickListener(v -> toggleService());
        card.addView(startStopButton, fullWidthParams());

        return card;
    }

    private View batterySafeCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView label = text("Battery safety", 13, appSettings.accentColor, Typeface.BOLD);
        card.addView(label);

        batteryStatusText = text("", 20, colorText, Typeface.BOLD);
        batteryStatusText.setPadding(0, dp(6), 0, dp(8));
        card.addView(batteryStatusText);

        batteryDetailText = text("", 14, colorMuted, Typeface.NORMAL);
        batteryDetailText.setPadding(0, 0, 0, dp(14));
        card.addView(batteryDetailText);

        batterySettingsButton = button("Open battery settings", false);
        batterySettingsButton.setOnClickListener(v -> openBatterySettings());
        card.addView(batterySettingsButton, fullWidthParams());

        TextView reliabilityTitle = text("Screen-off reliability", 15, colorText, Typeface.BOLD);
        reliabilityTitle.setPadding(0, dp(16), 0, dp(6));
        card.addView(reliabilityTitle);

        TextView reliabilityDetail = text("Keeps the CPU awake while reminders run, so pulses are less likely to be missed after locking the phone. Uses more battery.", 14, colorMuted, Typeface.NORMAL);
        reliabilityDetail.setPadding(0, 0, 0, dp(8));
        card.addView(reliabilityDetail);

        highReliabilityInput = new CheckBox(this);
        highReliabilityInput.setText("High reliability mode");
        highReliabilityInput.setTextSize(16);
        highReliabilityInput.setTextColor(colorText);
        highReliabilityInput.setButtonTintList(ColorStateList.valueOf(appSettings.accentColor));
        highReliabilityInput.setChecked(ReminderSettings.load(this).highReliabilityMode);
        highReliabilityInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSettingsFromUi();
            refreshServiceNotificationIfRunning();
        });
        card.addView(highReliabilityInput);

        refreshBatterySafeState();
        return card;
    }

    private View pulseSettings() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        burstDurationInput = numberInput(250);
        layout.addView(inputRow("Vibration", burstDurationInput, "ms each burst"));

        burstCountInput = numberInput(3);
        layout.addView(inputRow("Bursts", burstCountInput, "per reminder"));

        burstGapInput = numberInput(50);
        layout.addView(inputRow("Gap", burstGapInput, "ms between bursts"));

        return layout;
    }

    private View intervalSettings() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        rangeRadio = radio("Random between");
        aroundRadio = radio("Random around");
        exactRadio = radio("Exactly every");

        rangeMinInput = numberInput(15);
        rangeMaxInput = numberInput(20);
        aroundBaseInput = numberInput(18);
        aroundVariationInput = numberInput(3);
        exactIntervalInput = numberInput(18);

        layout.addView(modeRow(rangeRadio, rangeInputs()));
        layout.addView(modeRow(aroundRadio, aroundInputs()));
        layout.addView(modeRow(exactRadio, exactInputs()));

        return layout;
    }

    private View actions() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, dp(4), 0, 0);

        applyButton = button("Apply changes", false);
        applyButton.setOnClickListener(v -> {
            saveSettingsFromUi();
            Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show();
        });
        layout.addView(applyButton, fullWidthParams());

        previewButton = button("Preview pulse", false);
        previewButton.setOnClickListener(v -> {
            ReminderSettings settings = saveSettingsFromUi();
            VibrationPlayer.play(this, settings);
        });
        LinearLayout.LayoutParams params = fullWidthParams();
        params.topMargin = dp(10);
        layout.addView(previewButton, params);

        return layout;
    }

    private LinearLayout rangeInputs() {
        LinearLayout row = compactRow();
        row.addView(smallLabel("from"));
        row.addView(rangeMinInput);
        row.addView(smallLabel("to"));
        row.addView(rangeMaxInput);
        row.addView(smallLabel("sec"));
        return row;
    }

    private LinearLayout aroundInputs() {
        LinearLayout row = compactRow();
        row.addView(aroundBaseInput);
        row.addView(smallLabel("sec +/-"));
        row.addView(aroundVariationInput);
        row.addView(smallLabel("sec"));
        return row;
    }

    private LinearLayout exactInputs() {
        LinearLayout row = compactRow();
        row.addView(exactIntervalInput);
        row.addView(smallLabel("sec"));
        return row;
    }

    private View modeRow(RadioButton radioButton, View controls) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(10));
        row.setOnClickListener(v -> selectMode(modeFor(radioButton)));

        radioButton.setOnClickListener(v -> selectMode(modeFor(radioButton)));
        row.addView(radioButton);
        row.addView(controls);
        return row;
    }

    private View inputRow(String label, EditText input, String suffix) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelView = text(label, 15, colorText, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(labelView, labelParams);

        row.addView(input);

        TextView suffixView = text(suffix, 14, colorMuted, Typeface.NORMAL);
        suffixView.setPadding(dp(10), 0, 0, 0);
        row.addView(suffixView);

        return row;
    }

    private LinearLayout section(String title, View content) {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = text(title, 17, appSettings.accentColor, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, dp(8));
        card.addView(titleView);
        card.addView(content);
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setBackground(rounded(colorCard, colorBorder, 8));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(14);
        card.setLayoutParams(params);
        return card;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : appSettings.accentColor);
        button.setBackground(rounded(primary ? appSettings.accentColor : colorCard, primary ? appSettings.accentColor : colorBorder, 8));
        button.setMinHeight(dp(48));
        return button;
    }

    private Button compactButton(String label) {
        Button button = button(label, false);
        button.setMinHeight(dp(40));
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private TextView menuItem(String label, View.OnClickListener listener) {
        TextView item = text(label, 16, label.equals("Exit") ? appSettings.stopColor : colorText, Typeface.BOLD);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), 0, dp(12), 0);
        item.setOnClickListener(listener);
        item.setBackground(rounded(colorCard, colorCard, 6));
        item.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
        ));
        return item;
    }

    private RadioButton radio(String label) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(label);
        radioButton.setTextSize(16);
        radioButton.setTextColor(colorText);
        radioButton.setButtonTintList(ColorStateList.valueOf(appSettings.accentColor));
        return radioButton;
    }

    private EditText numberInput(int defaultValue) {
        EditText input = new EditText(this);
        input.setText(String.valueOf(defaultValue));
        input.setSelectAllOnFocus(true);
        input.setGravity(Gravity.CENTER);
        input.setTextSize(16);
        input.setTextColor(colorText);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        input.setBackground(rounded(colorInput, colorBorder, 8));
        input.setPadding(dp(8), 0, dp(8), 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(72), dp(44));
        params.leftMargin = dp(6);
        params.rightMargin = dp(6);
        input.setLayoutParams(params);
        return input;
    }

    private TextView smallLabel(String value) {
        TextView label = text(value, 14, colorMuted, Typeface.NORMAL);
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    private LinearLayout compactRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(34), dp(2), 0, 0);
        return row;
    }

    private LinearLayout dialogContent() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), 0);
        return content;
    }

    private TextView dialogTitle(String value) {
        TextView title = text(value, 22, appSettings.accentColor, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(12));
        return title;
    }

    private View colorDialogField(String label, EditText input) {
        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.VERTICAL);
        field.setPadding(0, dp(6), 0, dp(10));

        TextView labelView = text(label, 15, colorText, Typeface.BOLD);
        labelView.setPadding(0, 0, 0, dp(6));
        field.addView(labelView);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(input);

        Button pickButton = compactButton("Pick");
        pickButton.setOnClickListener(v -> showColorPicker(label, input));
        LinearLayout.LayoutParams pickParams = new LinearLayout.LayoutParams(dp(76), dp(44));
        pickParams.leftMargin = dp(8);
        row.addView(pickButton, pickParams);
        field.addView(row);
        return field;
    }

    private EditText colorValueInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(colorText);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7), new InputFilter.AllCaps()});
        input.setBackground(rounded(colorInput, colorBorder, 8));
        input.setPadding(dp(10), 0, dp(10), 0);
        input.setLayoutParams(new LinearLayout.LayoutParams(dp(116), dp(44)));
        return input;
    }

    private void showColorPicker(String title, EditText targetInput) {
        int initialColor = AppSettings.isValidHexColor(targetInput.getText().toString())
                ? AppSettings.parseHexColor(targetInput.getText().toString())
                : appSettings.accentColor;

        LinearLayout content = dialogContent();
        content.addView(dialogTitle(title));

        TextView preview = text(AppSettings.colorToHex(initialColor), 16, readableTextColor(initialColor), Typeface.BOLD);
        preview.setGravity(Gravity.CENTER);
        preview.setBackground(rounded(initialColor, initialColor, 8));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        previewParams.bottomMargin = dp(14);
        content.addView(preview, previewParams);

        TextView redLabel = colorChannelLabel("Red", Color.red(initialColor));
        SeekBar redBar = colorSeekBar(Color.red(initialColor));
        TextView greenLabel = colorChannelLabel("Green", Color.green(initialColor));
        SeekBar greenBar = colorSeekBar(Color.green(initialColor));
        TextView blueLabel = colorChannelLabel("Blue", Color.blue(initialColor));
        SeekBar blueBar = colorSeekBar(Color.blue(initialColor));

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateColorPickerPreview(preview, redLabel, greenLabel, blueLabel, redBar, greenBar, blueBar);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        redBar.setOnSeekBarChangeListener(listener);
        greenBar.setOnSeekBarChangeListener(listener);
        blueBar.setOnSeekBarChangeListener(listener);

        content.addView(redLabel);
        content.addView(redBar);
        content.addView(greenLabel);
        content.addView(greenBar);
        content.addView(blueLabel);
        content.addView(blueBar);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setPositiveButton("Use colour", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(view -> {
            styleDialog(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                int color = Color.rgb(redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
                targetInput.setText(AppSettings.colorToHex(color));
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private SeekBar colorSeekBar(int value) {
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(255);
        seekBar.setProgress(value);
        seekBar.setProgressTintList(ColorStateList.valueOf(appSettings.accentColor));
        seekBar.setThumbTintList(ColorStateList.valueOf(appSettings.accentColor));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(colorBorder));
        seekBar.setPadding(0, 0, 0, dp(8));
        return seekBar;
    }

    private TextView colorChannelLabel(String label, int value) {
        TextView textView = text(label + ": " + value, 14, colorMuted, Typeface.BOLD);
        textView.setPadding(0, dp(4), 0, 0);
        return textView;
    }

    private void updateColorPickerPreview(
            TextView preview,
            TextView redLabel,
            TextView greenLabel,
            TextView blueLabel,
            SeekBar redBar,
            SeekBar greenBar,
            SeekBar blueBar
    ) {
        int color = Color.rgb(redBar.getProgress(), greenBar.getProgress(), blueBar.getProgress());
        preview.setText(AppSettings.colorToHex(color));
        preview.setTextColor(readableTextColor(color));
        preview.setBackground(rounded(color, color, 8));
        redLabel.setText("Red: " + redBar.getProgress());
        greenLabel.setText("Green: " + greenBar.getProgress());
        blueLabel.setText("Blue: " + blueBar.getProgress());
    }

    private int readableTextColor(int background) {
        int red = Color.red(background);
        int green = Color.green(background);
        int blue = Color.blue(background);
        return (red * 299 + green * 587 + blue * 114) / 1000 >= 150 ? Color.BLACK : Color.WHITE;
    }

    private void styleDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(rounded(colorCard, colorBorder, 8));
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (positive != null) {
            positive.setTextColor(appSettings.accentColor);
        }
        if (negative != null) {
            negative.setTextColor(appSettings.accentColor);
        }
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT, style);
        return textView;
    }

    private ReminderSettings saveSettingsFromUi() {
        ReminderSettings settings = collectSettings().normalized();
        settings.save(this);
        setInputValues(settings);
        return settings;
    }

    private ReminderSettings collectSettings() {
        return new ReminderSettings(
                selectedMode,
                readInt(burstDurationInput, 250),
                readInt(burstCountInput, 3),
                readInt(burstGapInput, 50),
                readInt(exactIntervalInput, 18),
                readInt(rangeMinInput, 15),
                readInt(rangeMaxInput, 20),
                readInt(aroundBaseInput, 18),
                readInt(aroundVariationInput, 3),
                highReliabilityInput == null || highReliabilityInput.isChecked()
        );
    }

    private void loadSettings() {
        ReminderSettings settings = ReminderSettings.load(this);
        selectedMode = settings.mode;
        setInputValues(settings);
        selectMode(selectedMode);
        refreshRunningState();
    }

    private void rebuildUi() {
        setContentView(buildContentView());
        loadSettings();
    }

    private void loadThemeColors() {
        appSettings = AppSettings.load(this);
        if (appSettings.darkMode) {
            colorPage = Color.rgb(18, 26, 42);
            colorCard = Color.rgb(23, 31, 49);
            colorText = Color.rgb(233, 237, 246);
            colorMuted = Color.rgb(168, 179, 209);
            colorBorder = Color.rgb(42, 53, 80);
            colorInput = Color.rgb(15, 21, 36);
            colorInputDisabled = Color.rgb(19, 26, 43);
            colorDisabledText = Color.rgb(128, 134, 150);
        } else {
            colorPage = Color.rgb(246, 248, 251);
            colorCard = Color.WHITE;
            colorText = Color.rgb(19, 32, 24);
            colorMuted = Color.rgb(91, 106, 98);
            colorBorder = Color.rgb(220, 229, 223);
            colorInput = Color.WHITE;
            colorInputDisabled = Color.rgb(245, 247, 246);
            colorDisabledText = Color.rgb(150, 160, 154);
        }
    }

    private void applySystemBars() {
        getWindow().setStatusBarColor(colorPage);
        getWindow().setNavigationBarColor(colorPage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = appSettings.darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (!appSettings.darkMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    private void applySystemBarPadding(ScrollView scrollView) {
        final int baseLeft = 0;
        final int baseTop = 0;
        final int baseRight = 0;
        final int baseBottom = 0;

        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(
                    baseLeft + insets.getSystemWindowInsetLeft(),
                    baseTop + insets.getSystemWindowInsetTop(),
                    baseRight + insets.getSystemWindowInsetRight(),
                    baseBottom + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            scrollView.requestApplyInsets();
        }
    }

    private void setInputValues(ReminderSettings settings) {
        burstDurationInput.setText(String.valueOf(settings.burstDurationMs));
        burstCountInput.setText(String.valueOf(settings.burstCount));
        burstGapInput.setText(String.valueOf(settings.burstGapMs));
        exactIntervalInput.setText(String.valueOf(settings.exactIntervalSec));
        rangeMinInput.setText(String.valueOf(settings.rangeMinSec));
        rangeMaxInput.setText(String.valueOf(settings.rangeMaxSec));
        aroundBaseInput.setText(String.valueOf(settings.aroundBaseSec));
        aroundVariationInput.setText(String.valueOf(settings.aroundVariationSec));
        if (highReliabilityInput != null) {
            highReliabilityInput.setChecked(settings.highReliabilityMode);
        }
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        rangeRadio.setChecked(ReminderSettings.MODE_RANGE.equals(mode));
        aroundRadio.setChecked(ReminderSettings.MODE_AROUND.equals(mode));
        exactRadio.setChecked(ReminderSettings.MODE_EXACT.equals(mode));

        setEnabledForRow(rangeMinInput, ReminderSettings.MODE_RANGE.equals(mode));
        setEnabledForRow(rangeMaxInput, ReminderSettings.MODE_RANGE.equals(mode));
        setEnabledForRow(aroundBaseInput, ReminderSettings.MODE_AROUND.equals(mode));
        setEnabledForRow(aroundVariationInput, ReminderSettings.MODE_AROUND.equals(mode));
        setEnabledForRow(exactIntervalInput, ReminderSettings.MODE_EXACT.equals(mode));
    }

    private void setEnabledForRow(EditText input, boolean enabled) {
        input.setEnabled(enabled);
        input.setAlpha(enabled ? 1f : 0.45f);
        input.setTextColor(enabled ? colorText : colorDisabledText);
        input.setBackground(rounded(enabled ? colorInput : colorInputDisabled, colorBorder, 8));
    }

    private void toggleService() {
        if (ReminderSettings.isRunning(this)) {
            ReminderSettings.setRunning(this, false);
            Intent intent = new Intent(this, VibeReminderService.class);
            intent.setAction(VibeReminderService.ACTION_STOP);
            startService(intent);
        } else {
            saveSettingsFromUi();
            requestNotificationPermissionIfNeeded();
            Intent intent = new Intent(this, VibeReminderService.class);
            intent.setAction(VibeReminderService.ACTION_START);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
                ReminderSettings.setRunning(this, true);
            } catch (RuntimeException error) {
                ReminderSettings.setRunning(this, false);
                Toast.makeText(this, "Could not start reminders", Toast.LENGTH_SHORT).show();
            }
        }
        refreshRunningState();
    }

    private void refreshServiceNotificationIfRunning() {
        if (!ReminderSettings.isRunning(this)) {
            return;
        }
        Intent intent = new Intent(this, VibeReminderService.class);
        intent.setAction(VibeReminderService.ACTION_REFRESH);
        startService(intent);
    }

    private void refreshBatterySafeState() {
        if (batteryStatusText == null || batteryDetailText == null || batterySettingsButton == null) {
            return;
        }

        boolean exempt = isBatteryOptimizationExempt();
        if (exempt) {
            batteryStatusText.setText("Battery looks safe");
            batteryStatusText.setTextColor(appSettings.accentColor);
            batteryDetailText.setText("Android reports Pocket Pulse is exempt from battery optimizations.");
            batterySettingsButton.setText("Review battery settings");
        } else {
            batteryStatusText.setText("Unrestricted recommended");
            batteryStatusText.setTextColor(appSettings.stopColor);
            batteryDetailText.setText("Android may pause reminders in the background. Allow unrestricted battery use for more reliable pocket pulses.");
            batterySettingsButton.setText("Allow unrestricted");
        }
    }

    private boolean isBatteryOptimizationExempt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationExempt()) {
            Intent requestIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            requestIntent.setData(Uri.parse("package:" + getPackageName()));
            if (tryStartActivity(requestIntent)) {
                return;
            }
        }

        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        appSettingsIntent.setData(Uri.parse("package:" + getPackageName()));
        if (!tryStartActivity(appSettingsIntent)) {
            tryStartActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private boolean tryStartActivity(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private void refreshRunningState() {
        boolean running = ReminderSettings.isRunning(this);
        statusText.setText(running ? "Running" : "Stopped");
        statusText.setTextColor(running ? appSettings.stopColor : colorText);
        startStopButton.setText(running ? "Stop reminders" : "Start reminders");
        startStopButton.setTextColor(Color.WHITE);
        startStopButton.setBackground(rounded(running ? appSettings.stopColor : appSettings.accentColor, running ? appSettings.stopColor : appSettings.accentColor, 8));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private static String modeFor(RadioButton radioButton) {
        CharSequence text = radioButton.getText();
        if ("Random around".contentEquals(text)) {
            return ReminderSettings.MODE_AROUND;
        }
        if ("Exactly every".contentEquals(text)) {
            return ReminderSettings.MODE_EXACT;
        }
        return ReminderSettings.MODE_RANGE;
    }

    private int readInt(EditText input, int fallback) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private LinearLayout.LayoutParams fullWidthParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
