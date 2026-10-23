package cn.edu.njfu.timetable;

import android.content.*;
import java.util.*;

public final class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    Store s = new Store(c);
    long now = System.currentTimeMillis(), at = intent.getLongExtra("at", 0);
    if (!s.enabled()
        || !s.configured()
        || s.demo()
        || !AlarmScheduler.notifications(c)
        || intent.getLongExtra("revision", -1) != s.revision()) {
      AlarmScheduler.schedule(c);
      return;
    }
    try {
      Set<String> delivered =
          ReminderPolicy.prune(
              s.prefs.getStringSet(ReminderPolicy.PREF, Collections.emptySet()), now);
      // Only an explicitly scheduled zero-minute alarm may tolerate callback latency after start.
      List<CalendarEngine.Event> events =
          CalendarEngine.events(
              s.load(),
              s.monday(),
              s.total(),
              CalendarEngine.periods(s.times()),
              s.lead(),
              now - ReminderPolicy.ZERO_GRACE);
      boolean failed = false;
      for (CalendarEngine.Event e : ReminderPolicy.due(events, delivered, now, at)) {
        long minutes = Math.max(0, (e.start - now + 59999) / 60000);
        boolean sent =
            AlarmScheduler.notify(
                c,
                e.course.name + (minutes > 0 ? " · " + minutes + "分钟后上课" : " · 即将上课"),
                CalendarEngine.format(e.start, "MM月dd日 HH:mm")
                    + "  "
                    + (e.course.room.isEmpty() ? "教室待确认" : e.course.room)
                    + "\n第"
                    + e.course.first
                    + "–"
                    + e.course.last
                    + "节  "
                    + e.course.teacher,
                e.key().hashCode());
        if (sent) delivered.add(ReminderPolicy.key(e));
        else failed = true;
      }
      s.prefs
          .edit()
          .putStringSet(ReminderPolicy.PREF, delivered)
          .putLong("notificationRetryAfter", failed ? now + 60000L : 0)
          .commit();
    } catch (Exception e) {
      s.prefs
          .edit()
          .putString("alarmError", "提醒数据读取失败：" + e.getMessage())
          .putLong("notificationRetryAfter", now + 60000L)
          .apply();
    }
    AlarmScheduler.schedule(c);
  }
}
