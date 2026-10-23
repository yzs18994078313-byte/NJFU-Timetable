package cn.edu.njfu.timetable;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Build;
import android.view.*;
import android.widget.*;

/** Small native design system; all dimensions are dp and work on Android 5+. */
public final class Ui {
  public static int GREEN,
      PRIMARY,
      INK,
      MUTED,
      BG,
      SURFACE,
      SOFT,
      LINE,
      DANGER,
      OUTSIDE,
      SELECTION,
      TODAY;
  public static boolean NIGHT;
  public static ThemeColors colors;

  static {
    setColors(new ThemeColors(false));
  }

  private static void setColors(ThemeColors p) {
    colors = p;
    NIGHT = p.dark;
    GREEN = p.accent;
    PRIMARY = p.primary;
    INK = p.ink;
    MUTED = p.muted;
    BG = p.background;
    SURFACE = p.surface;
    SOFT = p.soft;
    LINE = p.line;
    DANGER = p.danger;
    OUTSIDE = p.outside;
    SELECTION = p.selection;
    TODAY = p.today;
  }

  public static void applyTheme(Activity a) {
    boolean night =
        (a.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    setColors(new ThemeColors(night));
  }

  private Ui() {}

  public static int dp(Activity a, float n) {
    return (int) (n * a.getResources().getDisplayMetrics().density + .5f);
  }

  public static LinearLayout column(Activity a) {
    LinearLayout l = new LinearLayout(a);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  public static LinearLayout row(Activity a) {
    LinearLayout l = new LinearLayout(a);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  public static TextView text(Activity a, String t, int size, int color) {
    TextView v = new TextView(a);
    v.setText(t);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setLineSpacing(dp(a, 3), 1);
    v.setIncludeFontPadding(false);
    return v;
  }

  public static TextView title(Activity a, String s) {
    TextView t = text(a, s, 26, INK);
    t.setTypeface(
        android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    return t;
  }

  public static GradientDrawable bg(int color, int radius) {
    GradientDrawable b = new GradientDrawable();
    b.setColor(color);
    b.setCornerRadius(radius);
    return b;
  }

  public static void pad(Activity a, View v, int n) {
    int p = dp(a, n);
    v.setPadding(p, p, p, p);
  }

  public static void gap(Activity a, LinearLayout parent, int n) {
    parent.addView(new View(a), new LinearLayout.LayoutParams(1, dp(a, n)));
  }

  public static void ripple(Activity a, View v, int fill, int radius) {
    v.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(0x222e63d8),
            bg(fill, dp(a, radius)),
            bg(Color.WHITE, dp(a, radius))));
  }

  public static Button button(Activity a, String s, Runnable click) {
    Button b = new Button(a);
    b.setText(s);
    b.setTextColor(GREEN);
    b.setTextSize(14);
    b.setTypeface(
        android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    b.setAllCaps(false);
    b.setMinHeight(dp(a, 48));
    b.setMinimumHeight(dp(a, 48));
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setStateListAnimator(null);
    b.setGravity(Gravity.CENTER);
    b.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
    ripple(a, b, SOFT, 14);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(a, 5), 0, dp(a, 5));
    b.setLayoutParams(lp);
    b.setOnClickListener(v -> click.run());
    return b;
  }

  public static final int LEFT = -1, RIGHT = 1, DOWN = 2;

  /** Geometry-based icons keep chevrons centered independently of font metrics. */
  public static Drawable chevron(Activity a, int direction, int color) {
    int intrinsic = dp(a, 24);
    Drawable icon =
        new Drawable() {
          private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

          @Override
          public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float unit = Math.min(b.width(), b.height());
            float x = b.exactCenterX(), y = b.exactCenterY();
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(unit / 12f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            Path p = new Path();
            if (direction == DOWN) {
              p.moveTo(x - unit / 4f, y - unit / 6f);
              p.lineTo(x, y + unit / 6f);
              p.lineTo(x + unit / 4f, y - unit / 6f);
            } else {
              float sign = direction == LEFT ? -1 : 1;
              p.moveTo(x - sign * unit / 6f, y - unit / 4f);
              p.lineTo(x + sign * unit / 6f, y);
              p.lineTo(x - sign * unit / 6f, y + unit / 4f);
            }
            canvas.drawPath(p, paint);
          }

          @Override
          public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
          }

          @Override
          public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
          }

          @Override
          public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
          }

          @Override
          public int getIntrinsicWidth() {
            return intrinsic;
          }

          @Override
          public int getIntrinsicHeight() {
            return intrinsic;
          }
        };
    icon.setBounds(0, 0, intrinsic, intrinsic);
    return icon;
  }

  public static ImageView chevronView(Activity a, int direction) {
    ImageView icon = new ImageView(a);
    icon.setImageDrawable(chevron(a, direction, GREEN));
    icon.setScaleType(ImageView.ScaleType.CENTER);
    icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    return icon;
  }

  public static ImageButton iconButton(Activity a, int direction, String label, Runnable action) {
    ImageButton button = new ImageButton(a);
    button.setImageDrawable(chevron(a, direction, GREEN));
    button.setScaleType(ImageView.ScaleType.CENTER);
    button.setPadding(0, 0, 0, 0);
    button.setMinimumWidth(dp(a, 48));
    button.setMinimumHeight(dp(a, 48));
    button.setContentDescription(label);
    button.setStateListAnimator(null);
    ripple(a, button, SOFT, 14);
    button.setOnClickListener(v -> action.run());
    return button;
  }

  public static Button forwardButton(Activity a, String label, Runnable action) {
    Button button = button(a, label, action);
    button.setCompoundDrawables(null, null, chevron(a, RIGHT, GREEN), null);
    button.setCompoundDrawablePadding(dp(a, 8));
    return button;
  }

  public static Button primary(Activity a, String s, Runnable click) {
    Button b = button(a, s, click);
    b.setTextColor(Color.WHITE);
    ripple(a, b, PRIMARY, 15);
    return b;
  }

  public static void stretch(Activity a, LinearLayout row, View v) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(a, 48), 1);
    lp.setMargins(dp(a, 3), 0, dp(a, 3), 0);
    row.addView(v, lp);
  }

  public static LinearLayout card(Activity a) {
    LinearLayout l = column(a);
    pad(a, l, 18);
    l.setBackground(bg(SURFACE, dp(a, 20)));
    l.setElevation(dp(a, 1));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, dp(a, 7), 0, dp(a, 7));
    l.setLayoutParams(lp);
    return l;
  }

  public static ScrollView scroll(Activity a, LinearLayout content) {
    ScrollView s = new ScrollView(a);
    s.setFillViewport(true);
    s.setClipToPadding(false);
    s.setVerticalScrollBarEnabled(false);
    s.addView(content);
    return s;
  }

  public static EditText field(
      Activity a, LinearLayout parent, String label, String value, int type) {
    TextView caption = text(a, label, 12, MUTED);
    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
    cp.setMargins(0, dp(a, 12), 0, dp(a, 7));
    parent.addView(caption, cp);
    EditText e = new EditText(a);
    e.setText(value);
    e.setTextSize(16);
    e.setTextColor(INK);
    e.setInputType(type);
    e.setSingleLine(true);
    e.setMinHeight(dp(a, 50));
    e.setPadding(dp(a, 14), dp(a, 10), dp(a, 14), dp(a, 10));
    GradientDrawable background = bg(BG, dp(a, 12));
    background.setStroke(dp(a, 1), LINE);
    e.setBackground(background);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.setMargins(0, 0, 0, dp(a, 5));
    parent.addView(e, lp);
    return e;
  }

  public static View divider(Activity a) {
    View v = new View(a);
    v.setBackgroundColor(LINE);
    v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(a, 1)));
    return v;
  }

  public static Button nav(Activity a, String label, boolean selected, Runnable click) {
    Button b = button(a, label, click);
    b.setTextSize(13);
    b.setTextColor(selected ? GREEN : MUTED);
    ripple(a, b, selected ? SOFT : SURFACE, 16);
    return b;
  }

  public static void root(Activity a, View root) {
    root.setBackgroundColor(BG);
    a.getWindow().setStatusBarColor(Build.VERSION.SDK_INT >= 23 ? BG : PRIMARY);
    a.getWindow().setNavigationBarColor(BG);
    if (Build.VERSION.SDK_INT >= 23) {
      int flags = a.getWindow().getDecorView().getSystemUiVisibility();
      flags =
          NIGHT
              ? flags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
              : flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      if (Build.VERSION.SDK_INT >= 26)
        flags =
            NIGHT
                ? flags & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                : flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      a.getWindow().getDecorView().setSystemUiVisibility(flags);
    }
    if (Build.VERSION.SDK_INT >= 30) a.getWindow().setDecorFitsSystemWindows(false);
    a.setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          v.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              insets.getSystemWindowInsetBottom());
          return insets;
        });
    root.requestApplyInsets();
  }
}
