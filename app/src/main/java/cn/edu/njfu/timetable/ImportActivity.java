package cn.edu.njfu.timetable;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/**
 * Uses the user's current university WebView session, without storing credentials or exposing a JS
 * bridge.
 */
public final class ImportActivity extends Activity {
  private WebView web;
  private TextView status;
  private Button sync;
  private String selectedTerm;
  private TextView termLabel;
  private LinearLayout footer;
  private ProgressBar progress;
  private boolean keyboardVisible;
  private volatile String scheme = "";
  private boolean reading, destroyed;
  private boolean enteredHome;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private static final String ENTRY = "https://jwxt.njfu.edu.cn/jsxsd/framework/xsMainV.jsp";

  static boolean allowed(String url) {
    try {
      Uri u = Uri.parse(url);
      String host = u.getHost();
      return "https".equalsIgnoreCase(u.getScheme())
          && host != null
          && (host.equalsIgnoreCase("njfu.edu.cn")
              || host.toLowerCase(Locale.US).endsWith(".njfu.edu.cn"))
          && (u.getPort() == -1 || u.getPort() == 443)
          && u.getUserInfo() == null;
    } catch (Exception e) {
      return false;
    }
  }

  private void capture(String url) {
    try {
      Uri u = Uri.parse(url);
      if (!"https".equalsIgnoreCase(u.getScheme())
          || !"jwxt.njfu.edu.cn".equalsIgnoreCase(u.getHost())
          || !CourseHttp.PATH.equals(u.getPath())) return;
      String term = u.getQueryParameter("xnxqid");
      if (term != null && term.matches("20\\d{2}-20\\d{2}-[12]"))
        runOnUiThread(
            () -> {
              if (!destroyed) setTerm(term);
            });
      String value = u.getQueryParameter("sjmsValue");
      if (value != null && !value.trim().isEmpty() && value.length() <= 256) {
        scheme = value;
        runOnUiThread(
            () -> {
              if (!destroyed && !reading) status.setText("已识别登录后分配的作息方案，可点“同步课表”。");
            });
      }
    } catch (Exception ignored) {
    }
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    Ui.applyTheme(this);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    LinearLayout root = Ui.column(this);
    root.setTag("import_root");
    LinearLayout toolbar = Ui.row(this);
    Ui.pad(this, toolbar, 10);
    toolbar.setTag("import_toolbar");
    ImageButton back = Ui.iconButton(this, Ui.LEFT, "返回", this::finish);
    toolbar.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
    LinearLayout heading = Ui.column(this);
    heading.setPadding(Ui.dp(this, 12), 0, 0, 0);
    TextView title = Ui.text(this, "连接教务系统", 19, Ui.INK);
    title.setTypeface(
        android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    heading.addView(title);
    Ui.gap(this, heading, 4);
    heading.addView(Ui.text(this, "南京林业大学 · 安全登录", 11, Ui.MUTED));
    toolbar.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    Button menu = Ui.button(this, "···", () -> {});
    menu.setTextSize(22);
    menu.setContentDescription("更多登录选项");
    menu.setOnClickListener(v -> showOptions(v));
    toolbar.addView(menu, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
    root.addView(toolbar);
    status = Ui.text(this, "正在打开学校登录页…", 12, Ui.GREEN);
    status.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
    status.setMaxLines(2);
    status.setEllipsize(TextUtils.TruncateAt.END);
    status.setBackgroundColor(Ui.SOFT);
    root.addView(status);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setIndeterminate(true);
    root.addView(progress, new LinearLayout.LayoutParams(-1, Ui.dp(this, 2)));
    web = new WebView(this);
    web.setTag("university_webview");
    web.setBackgroundColor(Color.WHITE);
    root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
    footer = Ui.column(this);
    footer.setTag("import_footer");
    footer.setBackgroundColor(Ui.SURFACE);
    footer.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 10));
    termLabel = Ui.text(this, "", 12, Ui.MUTED);
    termLabel.setGravity(Gravity.CENTER);
    termLabel.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 6));
    termLabel.setOnClickListener(v -> editTerm());
    footer.addView(termLabel);
    Calendar now = CalendarEngine.calendar(System.currentTimeMillis());
    int year = now.get(Calendar.YEAR), month = now.get(Calendar.MONTH) + 1;
    String suggested = month >= 8 ? year + "-" + (year + 1) + "-1" : (year - 1) + "-" + year + "-2";
    setTerm(state == null ? suggested : state.getString("term", suggested));
    sync =
        Ui.primary(
            this,
            "登录完成，导入课表",
            () -> {
              ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                  .hideSoftInputFromWindow(web.getWindowToken(), 0);
              resolveAndSync();
            });
    sync.setTag("import_sync");
    footer.addView(sync);
    root.addView(Ui.divider(this));
    root.addView(footer);
    Ui.root(this, root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          boolean ime =
              Build.VERSION.SDK_INT >= 30
                  ? insets.isVisible(WindowInsets.Type.ime())
                  : insets.getSystemWindowInsetBottom() > Ui.dp(this, 160);
          int bottom = insets.getSystemWindowInsetBottom();
          if (Build.VERSION.SDK_INT >= 30)
            bottom = Math.max(bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
          v.setPadding(
              insets.getSystemWindowInsetLeft(),
              insets.getSystemWindowInsetTop(),
              insets.getSystemWindowInsetRight(),
              bottom);
          setKeyboardVisible(ime);
          return insets;
        });
    // adjustResize on older Android can report a smaller root instead of IME insets.
    final int[] largest = {0, 0};
    root.getViewTreeObserver()
        .addOnGlobalLayoutListener(
            () -> {
              if (destroyed || Build.VERSION.SDK_INT >= 30) return;
              int width = root.getWidth(), height = root.getHeight();
              if (width != largest[1]) {
                largest[0] = height;
                largest[1] = width;
              }
              largest[0] = Math.max(largest[0], height);
              android.graphics.Rect frame = new android.graphics.Rect();
              root.getWindowVisibleDisplayFrame(frame);
              boolean resized = largest[0] - height > Ui.dp(this, 160);
              boolean covered = root.getRootView().getHeight() - frame.bottom > Ui.dp(this, 160);
              setKeyboardVisible(resized || covered || root.getPaddingBottom() > Ui.dp(this, 160));
            });
    root.requestApplyInsets();
    WebSettings s = web.getSettings();
    s.setUserAgentString(WebSettings.getDefaultUserAgent(this));
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(false);
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    s.setJavaScriptCanOpenWindowsAutomatically(false);
    s.setSupportMultipleWindows(false);
    s.setSaveFormData(false);
    s.setBuiltInZoomControls(true);
    s.setDisplayZoomControls(false);
    s.setUseWideViewPort(true);
    s.setLoadWithOverviewMode(false);
    s.setTextZoom(100);
    CookieManager.getInstance().setAcceptCookie(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
    if (Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);
    web.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView v, String url) {
            return block(url);
          }

          @Override
          public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return block(r.getUrl().toString());
          }

          private boolean block(String url) {
            Uri target = Uri.parse(url);
            if ("http".equalsIgnoreCase(target.getScheme())
                && "jwxt.njfu.edu.cn".equalsIgnoreCase(target.getHost())) {
              web.loadUrl(target.buildUpon().scheme("https").build().toString());
              return true;
            }
            if (allowed(url)) return false;
            status.setText("已拦截校外地址或非 HTTPS 跳转，请从学校入口进入。");
            return true;
          }

          @Override
          public WebResourceResponse shouldInterceptRequest(WebView v, String url) {
            capture(url);
            return null;
          }

          @Override
          public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
            capture(r.getUrl().toString());
            return null;
          }

          @Override
          public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
            status.setText("正在加载 " + Uri.parse(url).getHost());
            progress.setVisibility(View.VISIBLE);
          }

          @Override
          public void onPageFinished(WebView v, String url) {
            progress.setVisibility(View.GONE);
            adaptLogin(v, url);
            if (allowed(url) && url.contains("xsrkxz") && !enteredHome) {
              enteredHome = true;
              v.loadUrl(ENTRY);
              return;
            }
            if (!reading)
              status.setText(scheme.isEmpty() ? "请完成登录并进入教务主页 / 课表页。" : "已识别作息方案，点同步获取完整课表。");
          }

          @Override
          public void onReceivedSslError(WebView v, SslErrorHandler h, SslError e) {
            h.cancel();
            status.setText("网站证书校验失败，连接已停止。请检查设备日期和网络。");
          }

          @Override
          public void onReceivedError(WebView v, int code, String desc, String url) {
            status.setText("页面无法访问：" + desc + "。请检查校园网 / 学校VPN，也可从本地导入 HTML 课表。");
          }
        });
    if (state != null) {
      scheme = state.getString("scheme", "");
      if (web.restoreState(state) == null) web.loadUrl(ENTRY);
    } else
      CookieManager.getInstance()
          .removeAllCookies(
              value -> {
                if (!destroyed) web.loadUrl(ENTRY);
              });
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
              () -> {
                if (web.canGoBack()) web.goBack();
                else finish();
              });
  }

  void setKeyboardVisible(boolean visible) {
    keyboardVisible = visible;
    if (footer != null) {
      footer.setVisibility(View.VISIBLE);
      footer.setPadding(
          Ui.dp(this, 16),
          Ui.dp(this, visible ? 4 : 8),
          Ui.dp(this, 16),
          Ui.dp(this, visible ? 4 : 10));
    }
    if (termLabel != null) termLabel.setVisibility(visible ? View.GONE : View.VISIBLE);
    if (status != null) status.setVisibility(visible ? View.GONE : View.VISIBLE);
  }

  private void setTerm(String term) {
    selectedTerm = term;
    if (termLabel != null) termLabel.setText("学期 " + term + "  ·  点击修改");
  }

  private void editTerm() {
    LinearLayout form = Ui.column(this);
    Ui.pad(this, form, 18);
    EditText value =
        Ui.field(this, form, "学年学期", selectedTerm, android.text.InputType.TYPE_CLASS_TEXT);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("选择导入学期")
            .setView(form)
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
                        String term = value.getText().toString().trim();
                        new CourseQuery("all", "validation", term);
                        setTerm(term);
                        dialog.dismiss();
                      } catch (IllegalArgumentException e) {
                        value.setError(e.getMessage());
                      }
                    }));
    dialog.show();
  }

  private void showOptions(View anchor) {
    PopupMenu menu = new PopupMenu(this, anchor);
    menu.getMenu().add(0, 1, 0, "回到登录主页");
    menu.getMenu().add(0, 2, 1, "查询指定周");
    menu.getMenu().add(0, 3, 2, "修改学期");
    menu.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 1) web.loadUrl(ENTRY);
          else if (item.getItemId() == 2) queryWeek();
          else editTerm();
          return true;
        });
    menu.show();
  }

  private void adaptLogin(WebView view, String url) {
    if (!allowed(url)) return;
    try {
      view.evaluateJavascript(
          new String(Store.read(getAssets().open("login-portrait.js")), "UTF-8"), null);
    } catch (Exception ignored) {
    }
  }

  private void readPageOrSync() {
    if (reading) return;
    if (!allowed(web.getUrl())) {
      status.setText("请先完成学校登录");
      return;
    }
    if (!scheme.isEmpty()) {
      resolveAndSync("all");
      return;
    }
    try {
      String script = new String(Store.read(getAssets().open("page-timetable.js")), "UTF-8");
      web.evaluateJavascript(
          script,
          value -> {
            if (destroyed) return;
            try {
              Object raw = new JSONTokener(value).nextValue();
              String html =
                  raw instanceof String ? new JSONObject((String) raw).optString("html") : "";
              if (html.isEmpty()) {
                resolveAndSync("all");
                return;
              }
              reading = true;
              sync.setEnabled(false);
              executor.execute(
                  () -> {
                    try {
                      NjfuParser.Result parsed = NjfuParser.parse(html);
                      runOnUiThread(
                          () -> {
                            if (destroyed) return;
                            reading = false;
                            sync.setEnabled(true);
                            if (parsed.courses.isEmpty()) {
                              resolveAndSync("all");
                              return;
                            }
                            parsed.warnings.add("本次读取已显示的主页课表，请确认学校页面处于整学期模式。建议通过整学期接口同步核对完整性。");
                            preview(parsed, new CourseQuery("all", "page-session", selectedTerm));
                          });
                    } catch (Exception e) {
                      runOnUiThread(
                          () -> {
                            if (destroyed) return;
                            reading = false;
                            sync.setEnabled(true);
                            status.setText("页面读取失败：" + e.getMessage());
                          });
                    }
                  });
            } catch (Exception e) {
              resolveAndSync("all");
            }
          });
    } catch (Exception e) {
      resolveAndSync("all");
    }
  }

  private void queryWeek() {
    LinearLayout form = Ui.column(this);
    Ui.pad(this, form, 16);
    EditText date =
        Ui.field(
            this,
            form,
            "这一周的周一日期（如2026-8-31）",
            new Store(this).monday(),
            android.text.InputType.TYPE_CLASS_TEXT);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("查询指定周")
            .setView(form)
            .setNegativeButton("取消", null)
            .setPositiveButton("查询", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        String monday =
                            CourseQuery.normalizeMonday(date.getText().toString().trim());
                        dialog.dismiss();
                        resolveAndSync(monday);
                      } catch (IllegalArgumentException e) {
                        date.setError(e.getMessage());
                      }
                    }));
    dialog.show();
  }

  private void resolveAndSync() {
    readPageOrSync();
  }

  private void resolveAndSync(String rq) {
    if (reading) return;
    if (!allowed(web.getUrl())) {
      status.setText("请先打开学校登录页。");
      return;
    }
    if (!scheme.isEmpty()) {
      fetch(rq);
      return;
    }
    try {
      String script = new String(Store.read(getAssets().open("scheme.js")), "UTF-8");
      web.evaluateJavascript(
          script,
          result -> {
            try {
              Object value = new JSONTokener(result).nextValue();
              JSONObject config =
                  value instanceof String ? new JSONObject((String) value) : new JSONObject();
              String detected = config.optString("scheme");
              String term = config.optString("term");
              if (term.matches("20\\d{2}-20\\d{2}-[12]")) setTerm(term);
              if (!detected.trim().isEmpty() && detected.length() <= 256) {
                scheme = detected;
                fetch(rq);
              } else status.setText("尚未发现系统分配的作息方案。请登录后打开教务主页并等待课表加载，再点同步。");
            } catch (Exception e) {
              status.setText("无法读取作息方案，请重新打开教务主页。");
            }
          });
    } catch (Exception e) {
      status.setText("无法读取作息方案：" + e.getMessage());
    }
  }

  private void fetch(String rq) {
    String cookie = CookieManager.getInstance().getCookie(CourseHttp.ORIGIN + "/jsxsd/");
    String agent = web.getSettings().getUserAgentString(),
        value = scheme,
        term = selectedTerm.trim(),
        referer = web.getUrl();
    CourseQuery query;
    try {
      query = new CourseQuery(rq, value, term);
    } catch (IllegalArgumentException e) {
      status.setText(e.getMessage());
      return;
    }
    reading = true;
    sync.setEnabled(false);
    status.setText("all".equals(query.rq) ? "正在获取完整学期课表…" : "正在查询 " + query.rq + " 这一周的课表…");
    executor.execute(
        () -> {
          try {
            String html = CourseHttp.fetch(cookie, query, agent, referer);
            NjfuParser.Result result = NjfuParser.parse(html);
            runOnUiThread(
                () -> {
                  if (destroyed) return;
                  reading = false;
                  sync.setEnabled(true);
                  if (result.courses.isEmpty()) {
                    StringBuilder msg = new StringBuilder("未导入，原课表保留。");
                    for (String warning : result.warnings) msg.append('\n').append(warning);
                    status.setText(msg.toString());
                  } else preview(result, query);
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (destroyed) return;
                  reading = false;
                  sync.setEnabled(true);
                  status.setText("同步失败：" + e.getMessage() + "。原课表保留。");
                });
          }
        });
  }

  private void preview(NjfuParser.Result result, CourseQuery query) {
    List<Course> courses = result.courses;
    StringBuilder text =
        new StringBuilder(
            "学期 "
                + query.term
                + "，识别到 "
                + courses.size()
                + ("all".equals(query.rq)
                    ? " 个课程时段。已合并跨大节重复内容，保留不同周次的节次安排。\n确认后替换本地课表，并关闭提醒，待你确认学期日期和作息后重新开启。\n"
                    : " 个课程时段。查询周的周一为 " + query.rq + "。\n"));
    for (Course c : courses)
      text.append("\n")
          .append(c.name)
          .append("  周")
          .append("一二三四五六日".charAt(c.day - 1))
          .append("  ")
          .append(c.first)
          .append("–")
          .append(c.last)
          .append("节\n")
          .append(c.room)
          .append("  ")
          .append(c.teacher)
          .append("\n周次：")
          .append(c.weekText())
          .append('\n');
    if (!result.warnings.isEmpty()) {
      text.append("\n请核对：");
      for (String w : result.warnings) text.append("\n• ").append(w);
    }
    TextView body = Ui.text(this, text.toString(), 14, Ui.INK);
    Ui.pad(this, body, 16);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(body);
    AlertDialog.Builder previewDialog =
        new AlertDialog.Builder(this)
            .setTitle("all".equals(query.rq) ? "核对导入预览" : "指定周课表 · " + query.rq)
            .setView(scroll);
    if (!"all".equals(query.rq)) {
      previewDialog.setPositiveButton("关闭", null).show();
      return;
    }
    previewDialog
        .setNegativeButton("取消", null)
        .setPositiveButton(
            "确认替换",
            (d, w) -> {
              try {
                Store store = new Store(this);
                store.save(courses, "南京林业大学教务系统", false);
                store
                    .prefs
                    .edit()
                    .putBoolean("enabled", false)
                    .putBoolean("configured", false)
                    .putString("term", query.term)
                    .commit();
                AlarmScheduler.schedule(this);
                Toast.makeText(this, "已导入，请确认学期设置后开启提醒", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
              } catch (Exception e) {
                status.setText("保存失败：" + e.getMessage());
              }
            })
        .show();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    web.saveState(state);
    state.putString("scheme", scheme);
    state.putString("term", selectedTerm);
    super.onSaveInstanceState(state);
  }

  // API 33+ is handled by the native OnBackInvokedDispatcher registered above.
  // Keep this fallback for Android 5–12, where that dispatcher does not exist.
  @android.annotation.SuppressLint("GestureBackNavigation")
  @Override
  public void onBackPressed() {
    if (web != null && web.canGoBack()) web.goBack();
    else super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    destroyed = true;
    executor.shutdownNow();
    scheme = "";
    if (web != null) {
      web.stopLoading();
      web.clearHistory();
      web.clearCache(true);
      web.loadUrl("about:blank");
      ((android.view.ViewGroup) web.getParent()).removeView(web);
      web.destroy();
    }
    if (!isChangingConfigurations()) {
      CookieManager.getInstance().removeAllCookies(null);
      WebStorage.getInstance().deleteAllData();
    }
    super.onDestroy();
  }
}
