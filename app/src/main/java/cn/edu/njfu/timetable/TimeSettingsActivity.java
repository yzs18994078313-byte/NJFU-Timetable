package cn.edu.njfu.timetable;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Draft editing recalculates the tail when a period start time changes. */
public final class TimeSettingsActivity extends Activity {
  private Store store;
  private PeriodTimes draft;
  private boolean dirty;
  private ScrollView scroll;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    Ui.applyTheme(this);
    store = new Store(this);
    if (state == null)
      draft = PeriodTimes.from(store.times(), store.prefs.getInt("breakMinutes", -1));
    else {
      draft =
          new PeriodTimes(
              state.getIntArray("starts"), state.getIntArray("lengths"), state.getInt("gap"));
      dirty = state.getBoolean("dirty");
    }
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::leave);
    render(0);
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    state.putIntArray("starts", draft.starts);
    state.putIntArray("lengths", draft.lengths);
    state.putInt("gap", draft.gap);
    state.putBoolean("dirty", dirty);
    super.onSaveInstanceState(state);
  }

  // Android 5–12 use this fallback; Android 13+ is handled by the dispatcher above.
  @android.annotation.SuppressLint("GestureBackNavigation")
  @SuppressWarnings("deprecation")
  @Override
  public void onBackPressed() {
    leave();
  }

  private void leave() {
    if (!dirty) {
      finish();
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("保存课程时间？")
        .setMessage("修改尚未保存。")
        .setPositiveButton("保存", (d, w) -> save())
        .setNegativeButton("放弃修改", (d, w) -> finish())
        .setNeutralButton("继续编辑", null)
        .show();
  }

  private void changed(PeriodTimes next) {
    int y = scroll.getScrollY();
    draft = next;
    dirty = true;
    render(y);
  }

  private void render(int scrollY) {
    LinearLayout root = Ui.column(this);
    LinearLayout head = Ui.row(this);
    Ui.pad(this, head, 12);
    ImageButton back = Ui.iconButton(this, Ui.LEFT, "返回设置", this::leave);
    head.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
    TextView title = Ui.text(this, "课程时间", 20, Ui.INK);
    title.setPadding(Ui.dp(this, 12), 0, 0, 0);
    head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(head);
    LinearLayout body = Ui.column(this);
    Ui.pad(this, body, 16);
    scroll = Ui.scroll(this, body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout defaults = Ui.card(this);
    defaults.addView(Ui.text(this, "时长与课间", 18, Ui.INK));
    settingRow(
        defaults,
        "课程时长",
        draft.duration() + " 分钟" + (draft.uniform() ? "" : " · 有单节差异"),
        () ->
            WheelPicker.number(
                this,
                "每节课程时长",
                "分钟",
                1,
                180,
                Math.min(180, draft.duration()),
                n -> changed(draft.withDuration(n))));
    settingRow(
        defaults,
        "课间休息",
        draft.gap + " 分钟",
        () ->
            WheelPicker.number(
                this, "默认课间休息", "分钟", 0, 180, draft.gap, n -> changed(draft.withGap(n))));
    settingRow(
        defaults,
        "每天节数",
        draft.starts.length + " 节",
        () ->
            WheelPicker.number(
                this,
                "每天节数",
                "节",
                1,
                20,
                draft.starts.length,
                n -> {
                  try {
                    changed(draft.withCount(n));
                  } catch (Exception e) {
                    toast(e.getMessage());
                  }
                }));
    Ui.gap(this, defaults, 8);
    defaults.addView(Ui.text(this, "时长决定结束时间。修改某节开始时间后，后续节次按课程时长和课间休息自动重排。", 12, Ui.MUTED));
    defaults.addView(Ui.button(this, "按时长与课间重新排列", this::arrange));
    body.addView(defaults);
    Ui.gap(this, body, 12);
    body.addView(Ui.text(this, "每节开始时间", 19, Ui.INK));
    Ui.gap(this, body, 7);
    body.addView(Ui.text(this, "点时间，滑动选择。前面的节次不变，后续自动顺延；午休、晚休请再调整对应节次。", 12, Ui.MUTED));
    LinearLayout periods = Ui.card(this);
    periods.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4));
    for (int i = 0; i < draft.starts.length; i++) {
      final int index = i;
      if (i > 0) periods.addView(Ui.divider(this));
      LinearLayout row = Ui.row(this);
      row.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
      LinearLayout label = Ui.column(this);
      label.addView(Ui.text(this, "第 " + (i + 1) + " 节", 16, Ui.INK));
      String after = i == 0 ? "当天第一节课" : "课前休息 " + (draft.starts[i] - draft.end(i - 1)) + " 分钟";
      Ui.gap(this, label, 6);
      label.addView(Ui.text(this, after, 11, Ui.MUTED));
      row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
      LinearLayout clock = Ui.column(this);
      clock.setGravity(Gravity.END);
      LinearLayout startRow = Ui.row(this);
      TextView startText = Ui.text(this, CalendarEngine.time(draft.starts[i]), 21, Ui.GREEN);
      startText.setGravity(Gravity.END);
      startRow.addView(startText, new LinearLayout.LayoutParams(0, -2, 1));
      LinearLayout.LayoutParams arrowParams =
          new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24));
      arrowParams.leftMargin = Ui.dp(this, 6);
      startRow.addView(Ui.chevronView(this, Ui.RIGHT), arrowParams);
      clock.addView(startRow, new LinearLayout.LayoutParams(-1, -2));
      Ui.gap(this, clock, 5);
      clock.addView(
          Ui.text(
              this,
              draft.end(i) <= 1439 ? CalendarEngine.time(draft.end(i)) + " 结束" : "结束时间超出当天",
              11,
              Ui.MUTED));
      for (int j = 0; j < clock.getChildCount(); j++)
        if (clock.getChildAt(j) instanceof TextView) {
          ((TextView) clock.getChildAt(j)).setGravity(Gravity.END);
          clock.getChildAt(j).setPadding(0, 0, Ui.dp(this, 30), 0);
        }
      row.addView(clock, new LinearLayout.LayoutParams(Ui.dp(this, 136), -2));
      Ui.ripple(this, row, Ui.SURFACE, 10);
      row.setContentDescription(
          "第" + (i + 1) + "节开始时间 " + CalendarEngine.time(draft.starts[i]) + "，点击修改");
      row.setOnClickListener(
          v ->
              WheelPicker.time(
                  this,
                  "第 " + (index + 1) + " 节开始时间",
                  draft.starts[index],
                  n -> {
                    try {
                      changed(draft.withStart(index, n));
                    } catch (IllegalArgumentException e) {
                      toast(e.getMessage());
                    }
                  }));
      periods.addView(row);
    }
    body.addView(periods);
    try {
      draft.validate();
    } catch (Exception e) {
      TextView warning = Ui.text(this, e.getMessage(), 13, Ui.DANGER);
      Ui.pad(this, warning, 12);
      body.addView(warning);
    }
    LinearLayout footer = Ui.column(this);
    footer.setBackgroundColor(Ui.SURFACE);
    footer.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
    footer.addView(Ui.primary(this, "保存课程时间", this::save));
    root.addView(footer);
    Ui.root(this, root);
    scroll.post(() -> scroll.scrollTo(0, scrollY));
  }

  private void settingRow(LinearLayout parent, String label, String value, Runnable action) {
    LinearLayout row = Ui.row(this);
    row.setMinimumHeight(Ui.dp(this, 58));
    row.addView(Ui.text(this, label, 15, Ui.INK), new LinearLayout.LayoutParams(0, -2, 1));
    row.addView(Ui.text(this, value, 14, Ui.GREEN));
    LinearLayout.LayoutParams arrow =
        new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24));
    arrow.leftMargin = Ui.dp(this, 6);
    row.addView(Ui.chevronView(this, Ui.RIGHT), arrow);
    Ui.ripple(this, row, Ui.SURFACE, 10);
    row.setOnClickListener(v -> action.run());
    parent.addView(row);
  }

  private void arrange() {
    try {
      PeriodTimes next = draft.arranged();
      new AlertDialog.Builder(this)
          .setTitle("重新排列全部节次？")
          .setMessage(
              "从 "
                  + CalendarEngine.time(next.starts[0])
                  + " 开始，每节 "
                  + next.duration()
                  + " 分钟、课间 "
                  + next.gap
                  + " 分钟，最后一节 "
                  + CalendarEngine.time(next.end(next.starts.length - 1))
                  + " 结束。\n\n将替换当前单节开始时间，包括午休和晚休。保存前可逐节调整。")
          .setNegativeButton("取消", null)
          .setPositiveButton("重新排列", (d, w) -> changed(next))
          .show();
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  private void save() {
    try {
      String value = draft.serialize();
      for (Course c : store.load())
        if (c.last > draft.starts.length)
          throw new IllegalArgumentException(c.name + "需要第" + c.last + "节，请增加每天节数");
      if (!store.prefs.edit().putString("times", value).putInt("breakMinutes", draft.gap).commit())
        throw new IllegalStateException("保存失败，请重试");
      store.bump();
      AlarmScheduler.schedule(this);
      dirty = false;
      toast("课程时间已保存，提醒已更新");
      finish();
    } catch (Exception e) {
      toast(e.getMessage());
    }
  }

  private void toast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }
}
