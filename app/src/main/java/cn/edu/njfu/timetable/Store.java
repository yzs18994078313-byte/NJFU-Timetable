package cn.edu.njfu.timetable;

import android.content.*;
import android.util.AtomicFile;
import java.io.*;
import java.util.*;
import org.json.*;

public final class Store {
  public static final String DEFAULT_TIMES =
      "08:00-08:45\n"
          + "08:55-09:40\n"
          + "10:00-10:45\n"
          + "10:55-11:40\n"
          + "13:30-14:15\n"
          + "14:25-15:10\n"
          + "15:30-16:15\n"
          + "16:25-17:10\n"
          + "18:30-19:15\n"
          + "19:25-20:10\n"
          + "20:20-21:05\n"
          + "21:15-22:00";
  public final SharedPreferences prefs;
  private final AtomicFile file;

  public Store(Context c) {
    prefs = c.getSharedPreferences("settings", Context.MODE_PRIVATE);
    file = new AtomicFile(new File(c.getFilesDir(), "schedule.json"));
  }

  public String monday() {
    return prefs.getString("monday", CalendarEngine.mondayNow(System.currentTimeMillis()));
  }

  public String times() {
    return prefs.getString("times", DEFAULT_TIMES);
  }

  public int total() {
    return prefs.getInt("total", 20);
  }

  public int lead() {
    return prefs.getInt("lead", 15);
  }

  public boolean enabled() {
    return prefs.getBoolean("enabled", false);
  }

  public boolean configured() {
    return prefs.getBoolean("configured", false);
  }

  public boolean demo() {
    return prefs.getBoolean("demo", false);
  }

  public long revision() {
    return prefs.getLong("revision", 0);
  }

  public void bump() {
    prefs.edit().putLong("revision", revision() + 1).commit();
  }

  public List<Course> load() throws Exception {
    if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists())
      return new ArrayList<>();
    try (FileInputStream in = file.openRead()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) {
        out.write(b, 0, n);
        if (out.size() > 2 * 1024 * 1024) throw new IOException("课表文件过大");
      }
      return decode(new String(out.toByteArray(), "UTF-8"));
    }
  }

  public static List<Course> decode(String text) throws JSONException {
    JSONObject root = new JSONObject(text);
    if (root.getInt("version") != 1) throw new JSONException("不支持的课表版本");
    JSONArray a = root.getJSONArray("courses");
    if (a.length() > 500) throw new JSONException("最多支持500个课程时段");
    List<Course> result = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      String id = o.optString("id", UUID.randomUUID().toString());
      if (!ids.add(id)) throw new JSONException("课表包含重复ID");
      result.add(
          new Course(
              id,
              o.getString("name"),
              o.optString("teacher"),
              o.optString("room"),
              o.getInt("day"),
              o.getInt("first"),
              o.getInt("last"),
              Weeks.parse(o.getString("weeks"))));
    }
    return result;
  }

  public static String encode(List<Course> courses) throws JSONException {
    JSONArray a = new JSONArray();
    for (Course c : courses) {
      JSONObject o = new JSONObject();
      o.put("id", c.id)
          .put("name", c.name)
          .put("teacher", c.teacher)
          .put("room", c.room)
          .put("day", c.day)
          .put("first", c.first)
          .put("last", c.last)
          .put("weeks", c.weekText());
      a.put(o);
    }
    return new JSONObject().put("version", 1).put("courses", a).toString(2);
  }

  public void save(List<Course> courses, String source, boolean demo) throws Exception {
    byte[] bytes = encode(courses).getBytes("UTF-8");
    FileOutputStream stream = null;
    try {
      stream = file.startWrite();
      stream.write(bytes);
      file.finishWrite(stream);
    } catch (Exception e) {
      if (stream != null) file.failWrite(stream);
      throw e;
    }
    prefs
        .edit()
        .putString("source", source)
        .putBoolean("demo", demo)
        .putLong("updated", System.currentTimeMillis())
        .remove("delivered")
        .putStringSet(ReminderPolicy.PREF, new HashSet<>())
        .remove("notificationRetryAfter")
        .commit();
    bump();
  }

  public static byte[] read(InputStream in) throws IOException {
    try (InputStream input = in;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] b = new byte[8192];
      int n;
      while ((n = input.read(b)) != -1) {
        out.write(b, 0, n);
        if (out.size() > 2 * 1024 * 1024) throw new IOException("文件不能超过2MB");
      }
      return out.toByteArray();
    }
  }
}
