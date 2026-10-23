package cn.edu.njfu.timetable;

import android.app.Activity;
import android.graphics.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** A true clock-time week calendar, sized for a portrait phone. */
public final class TimetableView extends ViewGroup {
  public interface OnCourseClick {
    void click(Course course);
  }

  private final Activity activity;
  private final int header, leftWidth, startMinute, endMinute;
  private final int[][] periods;
  private final PeriodAxis axis;
  private final long weekStart;
  private final List<Slot> slots = new ArrayList<>();
  private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG),
      todayPaint = new Paint(),
      nowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final int today, nowMinute;

  private static final class Slot {
    View view;
    int day, start, end, lane, lanes;
    boolean course;

    Slot(View view, int day, int start, int end, boolean course, int lane, int lanes) {
      this.view = view;
      this.day = day;
      this.start = start;
      this.end = end;
      this.course = course;
      this.lane = lane;
      this.lanes = lanes;
    }
  }

  public TimetableView(
      Activity a, List<Course> courses, long weekStart, int[][] periods, OnCourseClick listener) {
    super(a);
    activity = a;
    header = Ui.dp(a, 52);
    this.periods = periods;
    this.weekStart = weekStart;
    axis = new PeriodAxis(periods, 52 * Math.max(1, a.getResources().getConfiguration().fontScale));
    leftWidth = Ui.dp(a, 42);
    startMinute = periods[0][0];
    endMinute = periods[periods.length - 1][1];
    setWillNotDraw(false);
    setTag("clock_week_calendar");
    line.setColor(Ui.LINE);
    line.setStrokeWidth(Ui.dp(a, .5f));
    todayPaint.setColor(Ui.TODAY);
    nowPaint.setColor(Ui.DANGER);
    nowPaint.setStrokeWidth(Ui.dp(a, 1));
    Calendar now = CalendarEngine.calendar(System.currentTimeMillis());
    nowMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    int offset = (int) ((CalendarDays.day(now.getTimeInMillis()) - weekStart) / CalendarDays.DAY);
    today = offset >= 0 && offset < 7 ? offset + 1 : 0;
    label(a, "时间", 0, -1, 10, Ui.MUTED);
    for (int day = 1; day <= 7; day++)
      label(
          a,
          "周"
              + "一二三四五六日".charAt(day - 1)
              + "\n"
              + CalendarEngine.format(weekStart + (day - 1) * CalendarDays.DAY, "M/d"),
          day,
          -1,
          11,
          day == today ? Ui.GREEN : Ui.MUTED);
    for (int i = 0; i < periods.length; i++) {
      LinearLayout label = Ui.column(a);
      TextView start = Ui.text(a, CalendarEngine.time(periods[i][0]), 9, Ui.MUTED);
      start.setGravity(Gravity.CENTER);
      label.addView(start);
      TextView number = Ui.text(a, "第" + (i + 1) + "节", 8, Ui.MUTED);
      number.setGravity(Gravity.CENTER);
      label.addView(number, new LinearLayout.LayoutParams(-1, 0, 1));
      TextView end = Ui.text(a, CalendarEngine.time(periods[i][1]), 9, Ui.MUTED);
      end.setGravity(Gravity.CENTER);
      label.addView(end);
      label.setContentDescription(
          "第"
              + (i + 1)
              + "节，"
              + CalendarEngine.time(periods[i][0])
              + "至"
              + CalendarEngine.time(periods[i][1]));
      slots.add(new Slot(label, 0, periods[i][0], periods[i][1], false, 0, 1));
      addView(label);
    }
    for (CalendarLayout.Entry e : CalendarLayout.arrange(courses, periods)) {
      Course c = e.course;
      LinearLayout card = Ui.column(a);
      card.setPadding(Ui.dp(a, 3), Ui.dp(a, 6), Ui.dp(a, 2), Ui.dp(a, 3));
      int color = MonthCalendarView.color(c);
      Ui.ripple(a, card, (color & 0x00ffffff) | 0x24000000, 6);
      TextView name = Ui.text(a, c.name, 10, color);
      name.setMaxLines(3);
      name.setEllipsize(TextUtils.TruncateAt.END);
      name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      card.addView(name);
      Ui.gap(a, card, 4);
      TextView clock = Ui.text(a, CalendarEngine.time(e.start), 9, color);
      clock.setMaxLines(1);
      card.addView(clock);
      TextView room =
          Ui.text(a, c.room.replace("白马校区", "").replace("新庄校区", "").replace("淮安校区", ""), 9, color);
      room.setMaxLines(2);
      room.setEllipsize(TextUtils.TruncateAt.END);
      card.addView(room);
      card.setContentDescription(
          c.name
              + "，周"
              + "一二三四五六日".charAt(c.day - 1)
              + "，"
              + CalendarEngine.time(e.start)
              + "至"
              + CalendarEngine.time(e.end)
              + "，"
              + c.room);
      card.setOnClickListener(v -> listener.click(c));
      slots.add(new Slot(card, c.day, e.start, e.end, true, e.lane, e.lanes));
      addView(card);
    }
  }

  private void label(Activity a, String text, int day, int minute, int size, int color) {
    TextView v = Ui.text(a, text, size, color);
    v.setGravity(minute < 0 ? Gravity.CENTER : Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    v.setMaxLines(2);
    slots.add(new Slot(v, day, minute, minute, false, 0, 1));
    addView(v);
  }

  private int y(int minute) {
    return header + Ui.dp(activity, axis.position(minute));
  }

  @Override
  protected void onMeasure(int w, int h) {
    int width = MeasureSpec.getSize(w);
    setMeasuredDimension(width, y(endMinute));
    float col = (width - leftWidth) / 7f;
    int gap = Ui.dp(activity, 1);
    for (Slot s : slots) {
      int cw = s.day == 0 ? leftWidth : Math.round(col / (s.course ? s.lanes : 1));
      int ch = s.start < 0 ? header : y(s.end) - y(s.start);
      s.view.measure(
          MeasureSpec.makeMeasureSpec(
              Math.max(1, cw - (s.course ? 2 * gap : 0)), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(
              Math.max(1, ch - (s.course ? 2 * gap : 0)), MeasureSpec.EXACTLY));
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    float col = (getWidth() - leftWidth) / 7f;
    int gap = Ui.dp(activity, 1);
    for (Slot s : slots) {
      int x =
          s.day == 0
              ? 0
              : Math.round(leftWidth + (s.day - 1) * col + (s.course ? s.lane * col / s.lanes : 0));
      int top = s.start < 0 ? 0 : y(s.start);
      if (s.course) {
        x += gap;
        top += gap;
      }
      s.view.layout(x, top, x + s.view.getMeasuredWidth(), top + s.view.getMeasuredHeight());
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float col = (getWidth() - leftWidth) / 7f;
    if (today > 0)
      canvas.drawRect(
          leftWidth + (today - 1) * col, 0, leftWidth + today * col, getHeight(), todayPaint);
    for (int[] period : periods) {
      canvas.drawLine(leftWidth, y(period[0]), getWidth(), y(period[0]), line);
      canvas.drawLine(leftWidth, y(period[1]), getWidth(), y(period[1]), line);
    }
    for (int d = 0; d <= 7; d++)
      canvas.drawLine(leftWidth + d * col, header, leftWidth + d * col, getHeight(), line);
    Calendar clock = CalendarEngine.calendar(System.currentTimeMillis());
    int nowMinute = clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE);
    int day =
        (int) ((CalendarDays.day(clock.getTimeInMillis()) - weekStart) / CalendarDays.DAY) + 1;
    if (day >= 1 && day <= 7 && nowMinute >= startMinute && nowMinute < endMinute) {
      float x = leftWidth + (day - 1) * col;
      canvas.drawLine(x, y(nowMinute), x + col, y(nowMinute), nowPaint);
      canvas.drawCircle(x + Ui.dp(activity, 2), y(nowMinute), Ui.dp(activity, 2.5f), nowPaint);
    }
    postInvalidateDelayed(60000L);
  }
}
