package cn.edu.njfu.timetable;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

public final class AlarmScheduler {
  public static final String CHANNEL = "course_reminders";

  private AlarmScheduler() {}

  public static boolean exact(Context c) {
    return Build.VERSION.SDK_INT < 31
        || ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
  }

  public static boolean notifications(Context c) {
    if (Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) return false;
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 24 && !nm.areNotificationsEnabled()) return false;
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch = nm.getNotificationChannel(CHANNEL);
      if (ch != null && ch.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
    }
    return true;
  }

  private static PendingIntent pending(Context c, long revision, long at) {
    Intent i =
        new Intent(c, ReminderReceiver.class)
            .setAction("cn.edu.njfu.timetable.REMIND")
            .putExtra("revision", revision)
            .putExtra("at", at);
    return PendingIntent.getBroadcast(
        c,
        100,
        i,
        PendingIntent.FLAG_UPDATE_CURRENT
            | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
  }

  public static void cancel(Context c) {
    ((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c, 0, 0));
  }

  /** Only the nearest due time is scheduled; its receiver schedules the next one. */
  public static synchronized void schedule(Context c) {
    cancel(c);
    Store s = new Store(c);
    long now = System.currentTimeMillis();
    Set<String> delivered =
        ReminderPolicy.prune(
            s.prefs.getStringSet(ReminderPolicy.PREF, Collections.emptySet()), now);
    // Older records identify only the course, so they cannot describe the current reminder target.
    s.prefs
        .edit()
        .remove("nextAlarm")
        .remove("delivered")
        .putStringSet(ReminderPolicy.PREF, delivered)
        .apply();
    if (!s.enabled() || !s.configured() || s.demo() || !notifications(c)) return;
    try {
      List<CalendarEngine.Event> events =
          CalendarEngine.events(
              s.load(), s.monday(), s.total(), CalendarEngine.periods(s.times()), s.lead(), now);
      long retryAfter = s.prefs.getLong("notificationRetryAfter", 0);
      CalendarEngine.Event next = ReminderPolicy.next(events, delivered, now, retryAfter);
      if (next == null) return;
      long when = ReminderPolicy.at(next, now, retryAfter);
      AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
      PendingIntent pi = pending(c, s.revision(), when);
      boolean precise = exact(c);
      try {
        if (precise) {
          if (Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
          else am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
      } catch (SecurityException e) {
        if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        else am.set(AlarmManager.RTC_WAKEUP, when, pi);
      }
      s.prefs.edit().putLong("nextAlarm", when).remove("alarmError").apply();
    } catch (Exception e) {
      s.prefs.edit().putString("alarmError", "提醒排程失败：" + e.getMessage()).apply();
    }
  }

  public static void channel(Context c) {
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel channel =
          new NotificationChannel(CHANNEL, "上课提醒", NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription("课程开始前的时间与教室提醒");
      channel.enableVibration(true);
      ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
          .createNotificationChannel(channel);
    }
  }

  public static boolean notify(Context c, String title, String body, int id) {
    channel(c);
    if (!notifications(c)) return false;
    Intent i =
        new Intent(c, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            id,
            i,
            PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
    Notification.Builder b =
        Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(c, CHANNEL)
            : new Notification.Builder(c);
    b.setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(new Notification.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setContentIntent(open)
        .setCategory(
            Build.VERSION.SDK_INT >= 23
                ? Notification.CATEGORY_REMINDER
                : Notification.CATEGORY_EVENT)
        .setPriority(Notification.PRIORITY_HIGH)
        .setDefaults(Notification.DEFAULT_ALL);
    try {
      ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE))
          .notify(id, b.build());
      return true;
    } catch (SecurityException ignored) {
      return false;
    }
  }
}
