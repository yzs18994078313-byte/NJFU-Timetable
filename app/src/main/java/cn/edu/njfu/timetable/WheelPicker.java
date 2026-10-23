package cn.edu.njfu.timetable;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import java.util.Locale;

/** Public Android wheel controls, compatible with Android 5 and later. */
public final class WheelPicker {
  public interface Selected {
    void value(int value);
  }

  private WheelPicker() {}

  private static NumberPicker wheel(Activity a, int min, int max, int value, boolean twoDigits) {
    NumberPicker picker = new NumberPicker(a);
    picker.setVerticalFadingEdgeEnabled(false);
    picker.setFadingEdgeLength(0);
    if (android.os.Build.VERSION.SDK_INT >= 29) picker.setTextColor(Ui.INK);
    picker.setMinValue(min);
    picker.setMaxValue(max);
    String[] labels = new String[max - min + 1];
    for (int i = min; i <= max; i++)
      labels[i - min] = twoDigits ? String.format(Locale.US, "%02d", i) : "" + i;
    picker.setDisplayedValues(labels);
    picker.setValue(Math.max(min, Math.min(max, value)));
    picker.setWrapSelectorWheel(true);
    picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    return picker;
  }

  public static void time(Activity a, String title, int minute, Selected selected) {
    show(a, title, "上下滑动选择开始时间 · 24小时制", minute, 0, 0, true, selected);
  }

  public static void number(
      Activity a, String title, String unit, int min, int max, int value, Selected selected) {
    show(a, title, unit, value, min, max, false, selected);
  }

  private static void show(
      Activity a,
      String title,
      String hint,
      int value,
      int min,
      int max,
      boolean time,
      Selected selected) {
    Dialog dialog = new Dialog(a);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout content = Ui.column(a);
    content.setBackground(Ui.bg(Ui.SURFACE, Ui.dp(a, 24)));
    content.setPadding(Ui.dp(a, 22), Ui.dp(a, 24), Ui.dp(a, 22), Ui.dp(a, 20));
    TextView heading = Ui.text(a, title, 20, Ui.INK);
    content.addView(heading);
    Ui.gap(a, content, 8);
    content.addView(Ui.text(a, hint, 13, Ui.MUTED));
    Ui.gap(a, content, 16);
    TextView preview = Ui.text(a, "", 28, Ui.GREEN);
    preview.setGravity(Gravity.CENTER);
    preview.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    content.addView(preview, new LinearLayout.LayoutParams(-1, Ui.dp(a, 42)));
    LinearLayout wheels = Ui.row(a);
    wheels.setGravity(Gravity.CENTER);
    NumberPicker first = wheel(a, time ? 0 : min, time ? 23 : max, time ? value / 60 : value, time);
    first.setContentDescription(time ? "小时" : title);
    LinearLayout firstCol = Ui.column(a);
    TextView firstLabel = Ui.text(a, time ? "时" : hint, 13, Ui.MUTED);
    firstLabel.setGravity(Gravity.CENTER);
    firstCol.addView(firstLabel);
    firstCol.addView(first, new LinearLayout.LayoutParams(-1, Ui.dp(a, 176)));
    wheels.addView(firstCol, new LinearLayout.LayoutParams(Ui.dp(a, time ? 112 : 180), -2));
    NumberPicker second = time ? wheel(a, 0, 59, value % 60, true) : null;
    if (time) {
      TextView colon = Ui.text(a, ":", 28, Ui.INK);
      wheels.addView(colon);
      LinearLayout col = Ui.column(a);
      TextView label = Ui.text(a, "分", 13, Ui.MUTED);
      label.setGravity(Gravity.CENTER);
      col.addView(label);
      col.addView(second, new LinearLayout.LayoutParams(-1, Ui.dp(a, 176)));
      second.setContentDescription("分钟");
      wheels.addView(col, new LinearLayout.LayoutParams(Ui.dp(a, 112), -2));
    }
    Runnable update =
        () ->
            preview.setText(
                time
                    ? CalendarEngine.time(first.getValue() * 60 + second.getValue())
                    : first.getValue() + " " + hint);
    first.setOnValueChangedListener((p, old, next) -> update.run());
    if (second != null) second.setOnValueChangedListener((p, old, next) -> update.run());
    update.run();
    content.addView(wheels);
    Ui.gap(a, content, 16);
    LinearLayout actions = Ui.row(a);
    Ui.stretch(a, actions, Ui.button(a, "取消", dialog::dismiss));
    Ui.stretch(
        a,
        actions,
        Ui.primary(
            a,
            "确定",
            () -> {
              int result = time ? first.getValue() * 60 + second.getValue() : first.getValue();
              dialog.dismiss();
              selected.value(result);
            }));
    content.addView(actions);
    dialog.setContentView(content);
    dialog.setCanceledOnTouchOutside(true);
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setGravity(Gravity.BOTTOM);
      window.setLayout(-1, -2);
      window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
  }
}
