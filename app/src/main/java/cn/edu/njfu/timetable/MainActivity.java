package cn.edu.njfu.timetable;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

public final class MainActivity extends Activity {
  private Store store;
  private List<Course> courses = new ArrayList<>();
  private int week = 1, tab = 0;
  private int calendarMode;
  private long selectedDay = CalendarDays.day(System.currentTimeMillis());
  private long month = CalendarDays.month(selectedDay);
  private LinearLayout body;
  private String error = "";
  private boolean loaded;
  private static final int OPEN = 10, EXPORT = 11, NOTIFY = 12;

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    Ui.applyTheme(this);
    store = new Store(this);
    if (b != null) {
      week = b.getInt("week", 1);
      tab = b.getInt("tab", 0);
      calendarMode = b.getInt("calendarMode", 0);
      selectedDay = b.getLong("selectedDay", selectedDay);
      month = b.getLong("month", month);
    } else
      week =
          Math.max(
              1,
              Math.min(
                  store.total(),
                  CalendarEngine.currentWeek(store.monday(), System.currentTimeMillis())));
    AlarmScheduler.channel(this);
    load();
    render();
  }

  @Override
  protected void onResume() {
    super.onResume();
    load();
    AlarmScheduler.schedule(this);
    render();
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    b.putInt("week", week);
    b.putInt("tab", tab);
    b.putInt("calendarMode", calendarMode);
    b.putLong("selectedDay", selectedDay);
    b.putLong("month", month);
    super.onSaveInstanceState(b);
  }

  private void load() {
    try {
      courses = store.load();
      error = "";
      loaded = true;
    } catch (Exception e) {
      error = "本地课表读取失败：" + e.getMessage() + "。可先导出原文件备份，避免直接覆盖。";
      loaded = false;
    }
  }

  private void render() {
    Ui.applyTheme(this);
    LinearLayout root = Ui.column(this);
    LinearLayout head = Ui.row(this);
    head.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
    TextView appTitle = Ui.text(this, tab == 0 ? "南林课表" : tab == 1 ? "课程导入" : "提醒与设置", 18, Ui.INK);
    appTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    head.addView(appTitle, new LinearLayout.LayoutParams(0, Ui.dp(this, 32), 1));
    appTitle.setGravity(Gravity.CENTER_VERTICAL);
    head.addView(
        Ui.text(this, CalendarEngine.format(System.currentTimeMillis(), "MM.dd  E"), 12, Ui.MUTED));
    root.addView(head);
    body = Ui.column(this);
    Ui.pad(this, body, 16);
    ScrollView scroll = Ui.scroll(this, body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout nav = Ui.row(this);
    String[] names = {"课表", "导入", "设置"};
    for (int i = 0; i < names.length; i++) {
      final int index = i;
      Button b =
          Ui.nav(
              this,
              names[i],
              tab == i,
              () -> {
                tab = index;
                render();
              });
      b.setTextColor(tab == i ? Ui.GREEN : Ui.MUTED);
      b.setTextSize(13);
      Ui.stretch(this, nav, b);
    }
    nav.setBackgroundColor(Ui.SURFACE);
    nav.setPadding(Ui.dp(this, 12), Ui.dp(this, 9), Ui.dp(this, 12), Ui.dp(this, 9));
    root.addView(Ui.divider(this));
    root.addView(nav);
    Ui.root(this, root);
    if (!error.isEmpty()) note(error);
    if (tab == 0) timetable();
    else if (tab == 1) manage();
    else settings();
  }

  private void note(String text) {
    LinearLayout card = Ui.card(this);
    Ui.pad(this, card, 12);
    card.addView(Ui.text(this, text, 12, Ui.MUTED));
    body.addView(card);
  }

  private void timetable() {
    long weekStart = CalendarEngine.start(store.monday(), week, 1, 0);
    LinearLayout summary = Ui.row(this);
    TextView title =
        Ui.title(
            this, calendarMode == 0 ? CalendarEngine.format(month, "yyyy年M月") : "第 " + week + " 周");
    title.setTextSize(25);
    title.setOnClickListener(
        v -> {
          if (calendarMode != 0) {
            chooseWeek();
            return;
          }
          Calendar c = CalendarEngine.calendar(selectedDay);
          new DatePickerDialog(
                  this,
                  (p, y, m, d) -> {
                    Calendar day = CalendarEngine.calendar(selectedDay);
                    day.set(y, m, d);
                    selectDay(CalendarDays.day(day.getTimeInMillis()));
                  },
                  c.get(Calendar.YEAR),
                  c.get(Calendar.MONTH),
                  c.get(Calendar.DAY_OF_MONTH))
              .show();
        });
    if (calendarMode != 0) {
      title.setCompoundDrawables(null, null, Ui.chevron(this, Ui.DOWN, Ui.INK), null);
      title.setCompoundDrawablePadding(Ui.dp(this, 6));
    }
    LinearLayout titleWrap = Ui.row(this);
    titleWrap.addView(title, new LinearLayout.LayoutParams(-2, -2));
    summary.addView(titleWrap, new LinearLayout.LayoutParams(0, -2, 1));
    ImageButton previous =
        Ui.iconButton(this, Ui.LEFT, calendarMode == 0 ? "上个月" : "上一周", () -> moveCalendar(-1));
    summary.addView(previous, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
    ImageButton next =
        Ui.iconButton(this, Ui.RIGHT, calendarMode == 0 ? "下个月" : "下一周", () -> moveCalendar(1));
    LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48));
    np.setMargins(Ui.dp(this, 5), 0, Ui.dp(this, 8), 0);
    summary.addView(next, np);
    summary.addView(
        Ui.button(this, "同步", this::openSchool),
        new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 48)));
    body.addView(summary);
    LinearLayout info = Ui.row(this);
    info.addView(
        Ui.text(
            this,
            calendarMode == 0
                ? "点选日期，查看当天课程"
                : CalendarEngine.format(weekStart, "M月d日")
                    + " — "
                    + CalendarEngine.format(weekStart + 6 * CalendarDays.DAY, "M月d日"),
            12,
            Ui.MUTED),
        new LinearLayout.LayoutParams(0, -2, 1));
    TextView today = Ui.text(this, "回到今天", 12, Ui.GREEN);
    Ui.pad(this, today, 10);
    today.setOnClickListener(
        v -> {
          selectedDay = CalendarDays.day(System.currentTimeMillis());
          month = CalendarDays.month(selectedDay);
          week =
              Math.max(
                  1,
                  Math.min(store.total(), CalendarEngine.currentWeek(store.monday(), selectedDay)));
          render();
        });
    info.addView(today);
    body.addView(info);
    LinearLayout modes = Ui.row(this);
    String[] names = {"月历平铺", "周历", "日程"};
    for (int i = 0; i < names.length; i++) {
      final int mode = i;
      Ui.stretch(
          this,
          modes,
          Ui.nav(
              this,
              names[i],
              calendarMode == i,
              () -> {
                calendarMode = mode;
                render();
              }));
    }
    body.addView(modes);
    Ui.gap(this, body, 10);
    if (store.demo()) note("演示课表 · 登录教务系统后导入自己的课程。");
    else if (!store.configured()) note("请到设置确认学期日期和课程时间。");
    if (courses.isEmpty()) {
      LinearLayout empty = Ui.card(this);
      empty.addView(Ui.title(this, "新的一周，从这里开始"));
      Ui.gap(this, empty, 12);
      empty.addView(Ui.text(this, "连接教务系统，把课程安排留在手机里。", 14, Ui.MUTED));
      Ui.gap(this, empty, 12);
      empty.addView(Ui.primary(this, "登录并导入课表", this::openSchool));
      empty.addView(Ui.button(this, "先看看演示课表", this::demo));
      body.addView(empty);
      return;
    }
    int[][] periods = CalendarEngine.periods(store.times());
    List<Course> active = new ArrayList<>();
    for (Course c : courses) if (c.weeks.contains(week)) active.add(c);
    Collections.sort(
        active,
        (a, b) ->
            a.day == b.day ? Integer.compare(a.first, b.first) : Integer.compare(a.day, b.day));
    if (calendarMode == 0) {
      body.addView(
          new MonthCalendarView(
              this, courses, store.monday(), store.total(), month, selectedDay, this::openDay),
          new LinearLayout.LayoutParams(-1, -2));
      Ui.gap(this, body, 20);
      int w = CalendarEngine.currentWeek(store.monday(), selectedDay);
      String label =
          CalendarEngine.format(selectedDay, "M月d日 E")
              + (w >= 1 && w <= store.total() ? " · 第" + w + "周" : " · 学期外");
      body.addView(Ui.text(this, label, 18, Ui.INK));
      List<Course> daily =
          CalendarDays.coursesOn(courses, store.monday(), store.total(), selectedDay);
      dayCards(daily, periods);
      if (daily.isEmpty()) note(w >= 1 && w <= store.total() ? "今天没有课程安排。" : "此日期不在设置的教学周内。");
      if (hasConflict(daily)) note("当天有课程时间重叠，请核对各门课程。");
    } else {
      TextView count =
          Ui.text(
              this,
              "本周 "
                  + active.size()
                  + " 个课程时段"
                  + (store.enabled() ? " · 提前" + store.lead() + "分钟提醒" : " · 点课程查看详情"),
              12,
              Ui.MUTED);
      count.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
      body.addView(count);
      if (calendarMode == 1) {
        LinearLayout wrap = Ui.column(this);
        wrap.setBackground(Ui.bg(Ui.SURFACE, Ui.dp(this, 18)));
        Ui.pad(this, wrap, 4);
        wrap.addView(
            new TimetableView(this, active, weekStart, periods, this::details),
            new LinearLayout.LayoutParams(-1, -2));
        body.addView(wrap);
      } else {
        for (int d = 1; d <= 7; d++) {
          List<Course> daily = new ArrayList<>();
          for (Course c : active) if (c.day == d) daily.add(c);
          if (daily.isEmpty()) continue;
          Ui.gap(this, body, 14);
          body.addView(
              Ui.text(
                  this,
                  CalendarEngine.format(weekStart + (d - 1) * CalendarDays.DAY, "M月d日 E"),
                  16,
                  Ui.INK));
          dayCards(daily, periods);
        }
      }
      if (active.isEmpty()) note("这一周没有课程安排。");
      if (hasConflict(active)) note("本周有课程时间重叠，请在日程中逐项核对。");
    }
    long updated = store.prefs.getLong("updated", 0);
    if (updated > 0) {
      Ui.gap(this, body, 12);
      body.addView(
          Ui.text(
              this,
              "最近更新 "
                  + CalendarEngine.format(updated, "MM-dd HH:mm")
                  + " · "
                  + store.prefs.getString("source", ""),
              11,
              Ui.MUTED));
    }
  }

  private void openDay(long date) {
    selectDay(date);
    int w = CalendarEngine.currentWeek(store.monday(), date);
    List<Course> daily = CalendarDays.coursesOn(courses, store.monday(), store.total(), date);
    LinearLayout content = Ui.column(this);
    Ui.pad(this, content, 14);
    content.setBackgroundColor(Ui.BG);
    content.addView(
        Ui.text(
            this,
            w >= 1 && w <= store.total() ? "第" + w + "周 · " + daily.size() + "个课程时段" : "此日期在学期外",
            13,
            Ui.MUTED));
    dayCards(content, daily, CalendarEngine.periods(store.times()));
    if (daily.isEmpty()) {
      Ui.gap(this, content, 24);
      content.addView(Ui.text(this, "今天没有课程安排", 16, Ui.MUTED));
      Ui.gap(this, content, 24);
    }
    if (hasConflict(daily)) content.addView(Ui.text(this, "有课程时间重叠，请核对。", 13, Ui.DANGER));
    ScrollView scroll = Ui.scroll(this, content);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(CalendarEngine.format(date, "M月d日 E"))
            .setView(scroll)
            .setNegativeButton("关闭", null)
            .create();
    for (int i = 0; i < daily.size(); i++) {
      Course course = daily.get(i);
      content
          .getChildAt(i + 1)
          .setOnClickListener(
              v -> {
                dialog.dismiss();
                details(course);
              });
    }
    dialog.show();
    Window window = dialog.getWindow();
    if (window != null) {
      window.setGravity(Gravity.BOTTOM);
      window.setLayout(-1, -2);
    }
  }

  private void selectDay(long date) {
    selectedDay = CalendarDays.day(date);
    month = CalendarDays.month(date);
    week = Math.max(1, Math.min(store.total(), CalendarEngine.currentWeek(store.monday(), date)));
    render();
  }

  private void moveCalendar(int delta) {
    if (calendarMode == 0) selectDay(CalendarDays.shiftMonth(month, delta));
    else {
      week = Math.max(1, Math.min(store.total(), week + delta));
      selectedDay = CalendarEngine.start(store.monday(), week, 1, 0);
      month = CalendarDays.month(selectedDay);
      render();
    }
  }

  private boolean hasConflict(List<Course> list) {
    for (int i = 0; i < list.size(); i++)
      for (int j = i + 1; j < list.size(); j++) {
        Course a = list.get(i), b = list.get(j);
        if (a.day == b.day && a.first <= b.last && b.first <= a.last) return true;
      }
    return false;
  }

  private String courseTime(Course c, int[][] periods) {
    return c.last <= periods.length
        ? CalendarEngine.time(periods[c.first - 1][0])
            + "–"
            + CalendarEngine.time(periods[c.last - 1][1])
        : "请补充节次时间";
  }

  private void dayCards(List<Course> daily, int[][] periods) {
    dayCards(body, daily, periods);
  }

  private void dayCards(LinearLayout target, List<Course> daily, int[][] periods) {
    for (Course c : daily) {
      LinearLayout card = Ui.card(this);
      LinearLayout row = Ui.row(this);
      View stripe = new View(this);
      stripe.setBackground(Ui.bg(MonthCalendarView.color(c), Ui.dp(this, 3)));
      row.addView(stripe, new LinearLayout.LayoutParams(Ui.dp(this, 4), Ui.dp(this, 64)));
      LinearLayout content = Ui.column(this);
      content.setPadding(Ui.dp(this, 14), 0, 0, 0);
      content.addView(Ui.text(this, c.name, 17, Ui.INK));
      Ui.gap(this, content, 7);
      content.addView(
          Ui.text(
              this, courseTime(c, periods) + " · 第" + c.first + "–" + c.last + "节", 13, Ui.GREEN));
      Ui.gap(this, content, 6);
      content.addView(
          Ui.text(
              this,
              (c.room.isEmpty() ? "教室待确认" : c.room)
                  + (c.teacher.isEmpty() ? "" : " · " + c.teacher),
              12,
              Ui.MUTED));
      row.addView(content, new LinearLayout.LayoutParams(0, -2, 1));
      card.addView(row);
      card.setOnClickListener(v -> details(c));
      target.addView(card);
    }
  }

  private void chooseWeek() {
    String[] choices = new String[store.total()];
    for (int i = 0; i < choices.length; i++) choices[i] = "第 " + (i + 1) + " 周";
    new AlertDialog.Builder(this)
        .setTitle("选择教学周")
        .setSingleChoiceItems(
            choices,
            week - 1,
            (d, w) -> {
              week = w + 1;
              selectedDay = CalendarEngine.start(store.monday(), week, 1, 0);
              month = CalendarDays.month(selectedDay);
              d.dismiss();
              render();
            })
        .setNegativeButton("取消", null)
        .show();
  }

  private void details(Course c) {
    new AlertDialog.Builder(this)
        .setTitle(c.name)
        .setMessage(
            "周"
                + "一二三四五六日".charAt(c.day - 1)
                + " 第"
                + c.first
                + "–"
                + c.last
                + "节  "
                + courseTime(c, CalendarEngine.periods(store.times()))
                + "\n教室："
                + c.room
                + "\n教师："
                + c.teacher
                + "\n教学周："
                + c.weekText())
        .setPositiveButton("编辑", (d, w) -> edit(c))
        .setNeutralButton(
            "删除",
            (d, w) ->
                new AlertDialog.Builder(this)
                    .setTitle("删除该课程时段？")
                    .setMessage(c.name)
                    .setNegativeButton("取消", null)
                    .setPositiveButton(
                        "删除",
                        (dd, ww) -> {
                          List<Course> next = new ArrayList<>(courses);
                          next.remove(c);
                          save(next, "本地编辑", store.demo());
                        })
                    .show())
        .setNegativeButton("关闭", null)
        .show();
  }

  private void openSchool() {
    startActivity(new Intent(this, ImportActivity.class));
  }

  private void manage() {
    LinearLayout card = Ui.card(this);
    card.addView(Ui.title(this, "从学校到你的课表"));
    card.addView(Ui.text(this, "在学校网页完成登录、选择学期，读取后逐项核对再导入。登录会话在离开导入页时清理。", 14, Ui.MUTED));
    card.addView(Ui.primary(this, "登录南京林业大学教务系统", this::openSchool));
    body.addView(card);
    LinearLayout local = Ui.card(this);
    local.addView(Ui.text(this, "本地管理", 18, Ui.INK));
    local.addView(Ui.button(this, "添加课程", () -> edit(null)));
    local.addView(
        Ui.button(
            this,
            "导入 JSON / HTML 课表",
            () -> {
              Intent i =
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("*/*")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              try {
                startActivityForResult(i, OPEN);
              } catch (ActivityNotFoundException e) {
                toast("设备没有文件选择器");
              }
            }));
    local.addView(
        Ui.button(
            this,
            "导出 JSON 课表",
            () -> {
              Intent i =
                  new Intent(Intent.ACTION_CREATE_DOCUMENT)
                      .setType("application/json")
                      .addCategory(Intent.CATEGORY_OPENABLE)
                      .putExtra(Intent.EXTRA_TITLE, "njfu-timetable.json");
              try {
                startActivityForResult(i, EXPORT);
              } catch (ActivityNotFoundException e) {
                toast("设备没有文件选择器");
              }
            }));
    local.addView(Ui.button(this, "查看演示课表", this::demo));
    body.addView(local);
    note("JSON / HTML 导入仅替换课程。学期日期、作息和提醒开关在设置中单独保存。文件不含账号、密码或登录会话。\n课程时段数：" + courses.size());
    if (!courses.isEmpty()) {
      TextView title = Ui.text(this, "全部课程 · 点开修改", 17, Ui.INK);
      body.addView(title);
      List<Course> all = new ArrayList<>(courses);
      Collections.sort(
          all,
          (a, b) ->
              a.day == b.day ? Integer.compare(a.first, b.first) : Integer.compare(a.day, b.day));
      for (Course c : all)
        body.addView(
            Ui.button(
                this,
                c.name + " · 周" + "一二三四五六日".charAt(c.day - 1) + " " + c.first + "–" + c.last + "节",
                () -> details(c)));
    }
  }

  private void save(List<Course> next, String source, boolean demo) {
    try {
      store.save(next, source, demo);
      courses = next;
      loaded = true;
      error = "";
      AlarmScheduler.schedule(this);
      render();
    } catch (Exception e) {
      toast("保存失败：" + e.getMessage());
    }
  }

  private void edit(Course existing) {
    if (!loaded) {
      toast("请先处理课表文件读取错误");
      return;
    }
    LinearLayout form = Ui.column(this);
    Ui.pad(this, form, 18);
    int normal = InputType.TYPE_CLASS_TEXT;
    EditText name = Ui.field(this, form, "课程名称", existing == null ? "" : existing.name, normal);
    EditText teacher = Ui.field(this, form, "教师", existing == null ? "" : existing.teacher, normal);
    EditText room = Ui.field(this, form, "教室", existing == null ? "" : existing.room, normal);
    EditText day =
        Ui.field(
            this,
            form,
            "星期（1=周一，7=周日）",
            existing == null ? "1" : "" + existing.day,
            InputType.TYPE_CLASS_NUMBER);
    EditText first =
        Ui.field(
            this,
            form,
            "开始节次",
            existing == null ? "1" : "" + existing.first,
            InputType.TYPE_CLASS_NUMBER);
    EditText last =
        Ui.field(
            this,
            form,
            "结束节次",
            existing == null ? "2" : "" + existing.last,
            InputType.TYPE_CLASS_NUMBER);
    EditText weeks =
        Ui.field(
            this,
            form,
            "周次，例如 1-16单 或 1-8,10-16",
            existing == null ? "1-16" : existing.weekText(),
            normal);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(existing == null ? "添加课程" : "编辑课程")
            .setView(Ui.scroll(this, form))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        Course c =
                            new Course(
                                existing == null ? null : existing.id,
                                name.getText().toString(),
                                teacher.getText().toString(),
                                room.getText().toString(),
                                Integer.parseInt(day.getText().toString()),
                                Integer.parseInt(first.getText().toString()),
                                Integer.parseInt(last.getText().toString()),
                                Weeks.parse(weeks.getText().toString()));
                        if (c.last > CalendarEngine.periods(store.times()).length)
                          throw new IllegalArgumentException("请先在设置中补充至第" + c.last + "节的时间");
                        if (existing == null && courses.size() >= 500)
                          throw new IllegalArgumentException("最多支持500个课程时段");
                        List<Course> next = new ArrayList<>(courses);
                        if (existing != null) next.remove(existing);
                        next.add(c);
                        store.save(next, "本地编辑", store.demo());
                        load();
                        AlarmScheduler.schedule(this);
                        dialog.dismiss();
                        render();
                      } catch (Exception e) {
                        toast(e.getMessage());
                      }
                    }));
    dialog.show();
  }

  private void demo() {
    new AlertDialog.Builder(this)
        .setTitle("载入演示课表？")
        .setMessage("将替换当前本地课程；演示课程不会触发提醒。需要保留现有数据时，请先导出 JSON。")
        .setNegativeButton("取消", null)
        .setPositiveButton(
            "载入",
            (d, w) -> {
              List<Course> data = new ArrayList<>();
              data.add(new Course(null, "高等数学", "演示教师", "教五楼 101", 1, 1, 2, Weeks.parse("1-18")));
              data.add(new Course(null, "大学英语", "演示教师", "教学楼 203", 2, 3, 4, Weeks.parse("1-16单")));
              data.add(new Course(null, "森林生态学", "演示教师", "教学楼 302", 3, 5, 6, Weeks.parse("1-16双")));
              data.add(new Course(null, "大学体育", "演示教师", "田径场", 5, 7, 8, Weeks.parse("1-16")));
              save(data, "演示数据", true);
              tab = 0;
              render();
            })
        .show();
  }

  private void settings() {
    LinearLayout card = Ui.card(this);
    card.addView(Ui.text(this, "学期与节次", 18, Ui.INK));
    card.addView(Ui.text(this, "请按本学期校历和实际校区作息设置。课程时间可逐节调整。", 13, Ui.MUTED));
    EditText monday =
        Ui.field(
            this,
            card,
            "第一教学周周一（yyyy-MM-dd）",
            store.monday(),
            InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE);
    EditText total =
        Ui.field(this, card, "学期周数（1–40）", "" + store.total(), InputType.TYPE_CLASS_NUMBER);
    PeriodTimes currentTimes =
        PeriodTimes.from(store.times(), store.prefs.getInt("breakMinutes", -1));
    Ui.gap(this, card, 14);
    card.addView(Ui.text(this, "课程时间", 16, Ui.INK));
    card.addView(
        Ui.text(
            this,
            "每天 "
                + currentTimes.starts.length
                + " 节 · 每节 "
                + currentTimes.duration()
                + " 分钟 · 默认课间 "
                + currentTimes.gap
                + " 分钟",
            12,
            Ui.MUTED));
    card.addView(
        Ui.forwardButton(
            this, "设置每节开始时间", () -> startActivity(new Intent(this, TimeSettingsActivity.class))));
    card.addView(
        Ui.button(
            this,
            "确认并保存学期设置",
            () -> {
              try {
                String start = monday.getText().toString().trim();
                Calendar date = CalendarEngine.date(start);
                if (date.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY)
                  throw new IllegalArgumentException("第一教学周的起始日期必须是周一");
                int n = Integer.parseInt(total.getText().toString());
                if (n < 1 || n > 40) throw new IllegalArgumentException("学期周数须为1至40");
                String value = store.times();
                int[][] periods = CalendarEngine.periods(value);
                for (Course c : courses) {
                  if (c.last > periods.length)
                    throw new IllegalArgumentException(c.name + "需要第" + c.last + "节，请补充作息");
                  for (int w : c.weeks)
                    if (w > n)
                      throw new IllegalArgumentException(c.name + "包含第" + w + "周，请增加学期周数或修改课程");
                }
                store
                    .prefs
                    .edit()
                    .putString("monday", start)
                    .putInt("total", n)
                    .putString("times", value)
                    .putBoolean("configured", true)
                    .commit();
                store.bump();
                week = Math.min(week, n);
                AlarmScheduler.schedule(this);
                toast("已保存并更新提醒");
                render();
              } catch (Exception e) {
                toast(e.getMessage());
              }
            }));
    body.addView(card);
    LinearLayout reminders = Ui.card(this);
    reminders.addView(Ui.text(this, "上课提醒", 18, Ui.INK));
    Switch enabled = new Switch(this);
    enabled.setText("开启上课提醒");
    enabled.setTextColor(Ui.INK);
    enabled.setChecked(store.enabled());
    reminders.addView(enabled);
    enabled.setOnCheckedChangeListener(
        (b, on) -> {
          if (on && (!store.configured() || store.demo() || courses.isEmpty())) {
            b.setChecked(false);
            toast("先确认学期设置并导入真实课程");
            return;
          }
          store.prefs.edit().putBoolean("enabled", on).commit();
          store.bump();
          AlarmScheduler.schedule(this);
          if (on
              && Build.VERSION.SDK_INT >= 33
              && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                  != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, NOTIFY);
          render();
        });
    reminders.addView(
        Ui.forwardButton(
            this,
            "提前 " + store.lead() + " 分钟提醒",
            () ->
                WheelPicker.number(
                    this,
                    "课前提前时间",
                    "分钟",
                    0,
                    180,
                    store.lead(),
                    n -> {
                      store.prefs.edit().putInt("lead", n).commit();
                      store.bump();
                      AlarmScheduler.schedule(this);
                      toast("已更新为提前" + n + "分钟");
                      render();
                    })));
    reminders.addView(
        Ui.text(
            this,
            "通知："
                + (AlarmScheduler.notifications(this) ? "已允许" : "未允许，请开启通知及上课提醒渠道")
                + "\n精确提醒："
                + (AlarmScheduler.exact(this) ? "可用" : "未授权，当前提醒可能延迟"),
            14,
            Ui.MUTED));
    reminders.addView(
        Ui.button(
            this,
            "通知权限 / 系统设置",
            () -> {
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                      != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, NOTIFY);
              } else {
                Intent i;
                if (Build.VERSION.SDK_INT >= 26)
                  i =
                      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                else
                  i =
                      new Intent(
                          Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                          Uri.parse("package:" + getPackageName()));
                launch(i);
              }
            }));
    if (Build.VERSION.SDK_INT >= 31)
      reminders.addView(
          Ui.button(
              this,
              "允许精确闹钟与提醒",
              () ->
                  launch(
                      new Intent(
                          Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + getPackageName())))));
    reminders.addView(
        Ui.button(
            this,
            "发送测试通知",
            () -> {
              if (!AlarmScheduler.notifications(this)) {
                toast("请先开启通知权限");
                return;
              }
              boolean sent =
                  AlarmScheduler.notify(this, "上课提醒测试", "通知已正常显示。正式提醒还需要开启开关并确认学期时间。", 999);
              toast(sent ? "已发送测试通知" : "通知发送失败，请检查系统通知设置");
            }));
    long next = store.prefs.getLong("nextAlarm", 0);
    reminders.addView(
        Ui.text(
            this,
            next > 0 ? "下一次提醒：" + CalendarEngine.format(next, "MM-dd E HH:mm") : "当前没有已排程的课程提醒",
            13,
            Ui.GREEN));
    String alarmError = store.prefs.getString("alarmError", "");
    if (!alarmError.isEmpty()) reminders.addView(Ui.text(this, alarmError, 13, Ui.DANGER));
    body.addView(reminders);
    note(
        "提醒时间固定按 Asia/Shanghai 计算。设备关机、强行停止应用、通知关闭或厂商后台限制可能导致提醒缺失。重启会恢复排程；强行停止后需重新打开应用。\n"
            + "关闭精确闹钟授权后，请重新打开应用以启用可能延迟的备用提醒。\n"
            + "登录会话只用于当前导入，不提供后台自动同步。");
  }

  private void launch(Intent i) {
    try {
      startActivity(i);
    } catch (ActivityNotFoundException e) {
      toast("此设备没有对应设置页，请在系统应用设置中操作");
    }
  }

  private void toast(String s) {
    Toast.makeText(this, s == null ? "输入无效" : s, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(request, permissions, grants);
    AlarmScheduler.schedule(this);
    render();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    try {
      if (request == OPEN) {
        String json =
            new String(Store.read(getContentResolver().openInputStream(data.getData())), "UTF-8")
                .replace("\uFEFF", "");
        List<Course> imported;
        String importNotes = "";
        if (json.trim().startsWith("<")) {
          NjfuParser.Result parsed = NjfuParser.parse(json);
          imported = parsed.courses;
          for (String warning : parsed.warnings) importNotes += "\n" + warning;
        } else imported = Store.decode(json);
        if (imported.isEmpty()) throw new IllegalArgumentException("文件没有课程");
        StringBuilder preview =
            new StringBuilder(
                "将替换当前课表，共" + imported.size() + "个课程时段。学期日期与节次请在设置中确认。\n" + importNotes);
        for (Course c : imported)
          preview
              .append("\n")
              .append(c.name)
              .append(" · 周")
              .append(c.day)
              .append(' ')
              .append(c.first)
              .append('–')
              .append(c.last)
              .append("节 · ")
              .append(c.weekText())
              .append("周");
        TextView text = Ui.text(this, preview.toString(), 14, Ui.INK);
        Ui.pad(this, text, 18);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(text);
        new AlertDialog.Builder(this)
            .setTitle("核对文件课程")
            .setView(scroll)
            .setNegativeButton("取消", null)
            .setPositiveButton(
                "确认替换",
                (d, w) -> {
                  store
                      .prefs
                      .edit()
                      .putBoolean("enabled", false)
                      .putBoolean("configured", false)
                      .commit();
                  save(imported, "本地课表文件", false);
                  toast("请重新确认学期设置，再开启提醒");
                })
            .show();
      } else if (request == EXPORT) {
        byte[] bytes;
        if (loaded) bytes = Store.encode(courses).getBytes("UTF-8");
        else bytes = Store.read(new FileInputStream(new File(getFilesDir(), "schedule.json")));
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
          if (out == null) throw new IOException("无法写入文件");
          out.write(bytes);
        }
        toast("已导出课表");
      }
    } catch (Exception e) {
      toast("文件操作失败：" + e.getMessage());
    }
  }
}
