package cn.edu.njfu.timetable;

import java.util.*;

/** One recurring meeting; multiple days or week patterns are stored as separate meetings. */
public final class Course {
  public final String id, name, teacher, room;
  public final int day, first, last;
  public final Set<Integer> weeks;

  public Course(
      String id,
      String name,
      String teacher,
      String room,
      int day,
      int first,
      int last,
      Set<Integer> weeks) {
    if (name == null || name.trim().isEmpty() || name.length() > 160)
      throw new IllegalArgumentException("课程名称不能为空且不超过160字");
    if (day < 1 || day > 7 || first < 1 || last < first || last > 20)
      throw new IllegalArgumentException("星期或节次无效");
    if (weeks == null || weeks.isEmpty()) throw new IllegalArgumentException("没有有效教学周");
    for (int w : weeks) if (w < 1 || w > 40) throw new IllegalArgumentException("教学周须在1至40之间");
    this.id = id == null ? UUID.randomUUID().toString() : id;
    this.name = name.trim();
    this.teacher = teacher == null ? "" : teacher.trim();
    this.room = room == null ? "" : room.trim();
    this.day = day;
    this.first = first;
    this.last = last;
    this.weeks = Collections.unmodifiableSet(new TreeSet<>(weeks));
  }

  public String weekText() {
    return Weeks.format(weeks);
  }
}
