package cn.edu.njfu.timetable;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** A flat, Monday-first month calendar with native accessible day tiles. */
public final class MonthCalendarView extends LinearLayout {
  public interface OnDayClick {
    void click(long date);
  }

  public static int color(Course c) {
    return Ui.colors.courses[Math.abs(c.name.hashCode() % Ui.colors.courses.length)];
  }

  public MonthCalendarView(
      Activity a,
      List<Course> courses,
      String monday,
      int total,
      long month,
      long selected,
      OnDayClick listener) {
    super(a);
    setOrientation(VERTICAL);
    setBackground(Ui.bg(Ui.SURFACE, Ui.dp(a, 18)));
    setPadding(Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8));
    LinearLayout headings = Ui.row(a);
    for (int d = 0; d < 7; d++) {
      TextView label = Ui.text(a, "" + "一二三四五六日".charAt(d), 12, Ui.MUTED);
      label.setGravity(Gravity.CENTER);
      headings.addView(label, new LayoutParams(0, Ui.dp(a, 30), 1));
    }
    addView(headings);
    long start = CalendarDays.gridStart(month),
        today = CalendarDays.day(System.currentTimeMillis());
    int count = CalendarDays.cells(month);
    for (int r = 0; r < count / 7; r++) {
      LinearLayout row = Ui.row(a);
      row.setBaselineAligned(false);
      for (int d = 0; d < 7; d++) {
        long date = start + (r * 7 + d) * CalendarDays.DAY;
        boolean inMonth = CalendarDays.month(date) == CalendarDays.month(month);
        boolean chosen = date == selected;
        List<Course> daily = CalendarDays.coursesOn(courses, monday, total, date);
        LinearLayout cell = Ui.column(a);
        cell.setPadding(Ui.dp(a, 2), Ui.dp(a, 5), Ui.dp(a, 2), Ui.dp(a, 4));
        GradientDrawable bg = Ui.bg(chosen ? Ui.SOFT : Ui.SURFACE, Ui.dp(a, 9));
        bg.setStroke(Ui.dp(a, 1), chosen ? Ui.SELECTION : Ui.LINE);
        cell.setBackground(bg);
        TextView number =
            Ui.text(
                a,
                CalendarEngine.format(date, "d"),
                14,
                date == today ? Color.WHITE : inMonth ? Ui.INK : Ui.OUTSIDE);
        number.setGravity(Gravity.CENTER);
        if (date == today) number.setBackground(Ui.bg(Ui.PRIMARY, Ui.dp(a, 14)));
        LayoutParams dayParams = new LayoutParams(Ui.dp(a, 26), Ui.dp(a, 26));
        dayParams.gravity = Gravity.CENTER_HORIZONTAL;
        cell.addView(number, dayParams);
        Ui.gap(a, cell, 4);
        for (int i = 0; i < Math.min(2, daily.size()); i++) {
          Course c = daily.get(i);
          TextView chip = Ui.text(a, c.name, 9, inMonth ? color(c) : Ui.MUTED);
          chip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
          chip.setMaxLines(1);
          chip.setEllipsize(TextUtils.TruncateAt.END);
          chip.setGravity(Gravity.CENTER_VERTICAL);
          chip.setPadding(Ui.dp(a, 2), 0, Ui.dp(a, 1), 0);
          chip.setBackground(Ui.bg((color(c) & 0x00ffffff) | 0x14000000, Ui.dp(a, 3)));
          LayoutParams cp = new LayoutParams(-1, Ui.dp(a, 18));
          cp.bottomMargin = Ui.dp(a, 2);
          cell.addView(chip, cp);
        }
        if (daily.size() > 2) cell.addView(Ui.text(a, "+" + (daily.size() - 2), 9, Ui.MUTED));
        StringBuilder description = new StringBuilder(CalendarEngine.format(date, "yyyy年M月d日 E"));
        description.append("，").append(daily.size()).append("个课程时段");
        for (Course c : daily) description.append("，").append(c.name);
        if (chosen) description.append("，已选中");
        cell.setContentDescription(description.toString());
        cell.setOnClickListener(v -> listener.click(date));
        LayoutParams cp = new LayoutParams(0, Ui.dp(a, 96), 1);
        cp.setMargins(Ui.dp(a, 1), Ui.dp(a, 1), Ui.dp(a, 1), Ui.dp(a, 1));
        row.addView(cell, cp);
      }
      addView(row);
    }
  }
}
