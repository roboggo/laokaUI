package com.inklauncher.minimal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    // 首次启动时按应用名匹配的默认常用应用；之后以用户长按修改并保存的结果为准
    private static final String[] DEFAULT_FAVORITES = {
            "Wi-Fi传书", "微信读书", "喜马拉雅", "京东读书",
            "老卡书库", "文件", "文本记事", "系统设置"
    };
    // 首页只保留 8 个应用
    private static final int MAX_HOME_APPS = 8;
    // favorites 结构版本：v1 是旧的 12 个，升级到 v2 时重置为 8 个默认
    private static final int FAV_VERSION = 2;

    private PackageManager pm;
    private SharedPreferences prefs;
    // 首页应用，按用户排序的顺序保存
    private final List<String> favPkgs = new ArrayList<>();

    private LinearLayout homePage;
    private LinearLayout allPage;
    private GridView homeGrid;
    private GridView allGrid;
    private AppAdapter homeAdapter;
    private AppAdapter allAdapter;
    private List<AppInfo> allApps = new ArrayList<>();
    private EditText search;
    private TextView editBtn;
    private boolean onAllPage = false;
    private boolean editMode = false;

    private static class AppInfo {
        String label;
        String pkg;
        String activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        prefs = getSharedPreferences("inkdesktop", MODE_PRIVATE);
        loadFavorites();

        int pad = dp(12);
        int padSm = dp(8);

        // 根布局：纯白底、竖向
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setPadding(pad, pad, pad, pad);

        // 顶部：时间 + 日期，居中，高度约占屏幕四分之一（两页共用）
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);

        TextView clock = new TextView(this);
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()));
        clock.setTextColor(Color.BLACK);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 144);
        clock.setTypeface(clock.getTypeface(), android.graphics.Typeface.BOLD);
        clock.setGravity(Gravity.CENTER);

        TextView date = new TextView(this);
        date.setText(new SimpleDateFormat("yyyy年M月d日  EEEE", Locale.CHINA).format(new java.util.Date()));
        date.setTextColor(Color.parseColor("#333333"));
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        date.setGravity(Gravity.CENTER);

        header.addView(clock);
        header.addView(date);

        // 时间放大 50%（144sp）、日期 36sp，顶部区域约 32% 屏高
        int headerH = (int) (screenHeightPx() * 0.32f);
        root.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, headerH));
        root.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        // ============ 首页：常用应用大图标 ============
        homePage = new LinearLayout(this);
        homePage.setOrientation(LinearLayout.VERTICAL);

        homeGrid = new GridView(this);
        homeGrid.setNumColumns(4);
        homeGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        homeGrid.setGravity(Gravity.CENTER);
        homeGrid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        homeGrid.setVerticalScrollBarEnabled(false);
        homeGrid.setHorizontalScrollBarEnabled(false);
        homeGrid.setPadding(0, 0, 0, 0);
        homeAdapter = new AppAdapter(96, 16);
        homeGrid.setAdapter(homeAdapter);

        // 外层容器占满图标区并垂直居中，GridView 给固定两行高度，
        // 这样 8 个图标作为整体在区域内上下左右居中
        LinearLayout homeGridWrap = new LinearLayout(this);
        homeGridWrap.setOrientation(LinearLayout.VERTICAL);
        homeGridWrap.setGravity(Gravity.CENTER);
        homeGridWrap.addView(homeGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(260)));
        homePage.addView(homeGridWrap, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        homePage.addView(divider(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        // 底部三块 tab：左「系统设置」、中「全部应用」(大)、右「安卓设置」
        LinearLayout homeBottom = new LinearLayout(this);
        homeBottom.setOrientation(LinearLayout.HORIZONTAL);
        homeBottom.setGravity(Gravity.CENTER_VERTICAL);
        homeBottom.setPadding(0, padSm, 0, 0);
        int tabH = dp(84);

        TextView sysTab = blockTab("系统设置", 15, 1f, tabH);
        TextView allTab = blockTab("全部应用", 20, 2f, tabH);
        TextView andTab = blockTab("安卓设置", 15, 1f, tabH);
        LinearLayout.LayoutParams mlp = (LinearLayout.LayoutParams) allTab.getLayoutParams();
        mlp.setMargins(padSm, 0, padSm, 0);
        homeBottom.addView(sysTab);
        homeBottom.addView(allTab);
        homeBottom.addView(andTab);
        homePage.addView(homeBottom, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(homePage, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // ============ 全部应用页 ============
        allPage = new LinearLayout(this);
        allPage.setOrientation(LinearLayout.VERTICAL);
        allPage.setVisibility(View.GONE);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, padSm, 0, 0);
        TextView backBtn = borderButton("← 返回");
        editBtn = borderButton("管理首页");
        TextView sortBtn = borderButton("排序首页");
        TextView setHomeBtn = borderButton("设为桌面");
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        backBtn.setLayoutParams(backLp);
        LinearLayout.LayoutParams topSpacer = new LinearLayout.LayoutParams(0, 1, 1f);
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topRow.addView(backBtn);
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        editLp.setMargins(padSm, 0, 0, 0);
        topRow.addView(editBtn, editLp);
        LinearLayout topGap = new LinearLayout(this);
        topGap.setLayoutParams(topSpacer);
        topRow.addView(topGap);
        topRow.addView(sortBtn, setLp);
        LinearLayout.LayoutParams shLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        shLp.setMargins(padSm, 0, 0, 0);
        topRow.addView(setHomeBtn, shLp);
        allPage.addView(topRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(0, padSm, 0, padSm);
        search = new EditText(this);
        search.setHint("搜索应用");
        search.setTextColor(Color.BLACK);
        search.setHintTextColor(Color.parseColor("#777777"));
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        search.setSingleLine(true);
        search.setBackground(borderDrawable());
        search.setPadding(pad, dp(6), pad, dp(6));
        search.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView refreshBtn = borderButton("刷新");
        LinearLayout.LayoutParams refLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        refLp.setMargins(padSm, 0, 0, 0);
        refreshBtn.setLayoutParams(refLp);
        tools.addView(search);
        tools.addView(refreshBtn);
        allPage.addView(tools, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        allGrid = new GridView(this);
        // 更多应用页图标 72dp，列数按屏宽自适应
        int cols = Math.max(3, Math.min(6, (int) (screenWidthDp() / 110)));
        allGrid.setNumColumns(cols);
        allGrid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        allGrid.setVerticalScrollBarEnabled(false);
        allGrid.setHorizontalScrollBarEnabled(false);
        // 图标放大后给网格顶部留白，避免第一行被搜索框压住
        allGrid.setPadding(0, dp(8), 0, dp(8));
        allGrid.setClipToPadding(false);
        allAdapter = new AppAdapter(72, 14);
        allGrid.setAdapter(allAdapter);
        allPage.addView(allGrid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        root.addView(allPage, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // ============ 事件 ============
        allTab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchPage(true); }
        });
        sysTab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { launchByPkg("hanvon.aebr.hvsettings"); }
        });
        andTab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { launchByPkg("com.android.settings"); }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchPage(false); }
        });
        sortBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showSortDialog(); }
        });
        setHomeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHomeGuide(); }
        });
        editBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleEditMode(); }
        });

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) { filter(s.toString()); }
        });
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                loadApps();
                Toast.makeText(MainActivity.this, "已刷新应用", Toast.LENGTH_SHORT).show();
            }
        });

        homeGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                launch((AppInfo) parent.getItemAtPosition(pos));
            }
        });
        homeGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                showItemMenu((AppInfo) parent.getItemAtPosition(pos), true);
                return true;
            }
        });
        allGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                AppInfo info = (AppInfo) parent.getItemAtPosition(pos);
                if (editMode) {
                    toggleFavorite(info);
                } else {
                    launch(info);
                }
            }
        });
        allGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                showItemMenu((AppInfo) parent.getItemAtPosition(pos), false);
                return true;
            }
        });

        setContentView(root);
        loadApps();
    }

    @Override
    public void onBackPressed() {
        if (editMode) {
            toggleEditMode();
        } else if (onAllPage) {
            switchPage(false);
        }
        // 首页不响应返回键
    }

    private void switchPage(boolean toAll) {
        onAllPage = toAll;
        if (!toAll && editMode) toggleEditMode();
        homePage.setVisibility(toAll ? View.GONE : View.VISIBLE);
        allPage.setVisibility(toAll ? View.VISIBLE : View.GONE);
        // 每次进入更多应用页都从顶部开始，避免上次滚动位置残留导致首行被压住
        if (toAll) allGrid.setSelection(0);
    }

    private void toggleEditMode() {
        editMode = !editMode;
        editBtn.setText(editMode ? "完成" : "管理首页");
        // 让网格重绘以显示/隐藏勾选标记
        allAdapter.notifyDataSetChanged();
        Toast.makeText(this, editMode ? "点击应用加入或移出首页" : "已完成", Toast.LENGTH_SHORT).show();
    }

    private void toggleFavorite(AppInfo info) {
        if (favPkgs.contains(info.pkg)) {
            favPkgs.remove(info.pkg);
        } else {
            if (favPkgs.size() >= MAX_HOME_APPS) {
                Toast.makeText(this, "首页最多 " + MAX_HOME_APPS + " 个，请先移除一个", Toast.LENGTH_SHORT).show();
                return;
            }
            favPkgs.add(info.pkg);
        }
        saveFavorites();
        refreshHome();
        allAdapter.notifyDataSetChanged();
    }

    // 上移一个首页应用
    private void moveFavoriteUp(int index) {
        if (index <= 0 || index >= favPkgs.size()) return;
        String pkg = favPkgs.remove(index);
        favPkgs.add(index - 1, pkg);
        saveFavorites();
        refreshHome();
    }

    // 排序对话框：点某个应用把它上移一位，反复点即可排到想要的位置
    private void showSortDialog() {
        if (favPkgs.isEmpty()) {
            Toast.makeText(this, "首页还没有应用", Toast.LENGTH_SHORT).show();
            return;
        }
        showSortDialogAt();
    }

    private void showSortDialogAt() {
        int n = favPkgs.size();
        CharSequence[] items = new CharSequence[n];
        for (int i = 0; i < n; i++) {
            AppInfo a = findApp(favPkgs.get(i));
            String name = a != null ? a.label : favPkgs.get(i);
            items[i] = (i + 1) + ". " + name + (i == 0 ? "  （首位）" : "   ↑上移");
        }
        new AlertDialog.Builder(this)
                .setTitle("排序首页 · 点应用上移")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (which > 0) {
                            moveFavoriteUp(which);
                            // 重建对话框以刷新顺序
                            showSortDialogAt();
                        }
                    }
                })
                .setPositiveButton("完成", null)
                .show();
    }

    private void launch(AppInfo info) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(info.pkg, info.activity);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动：" + info.label, Toast.LENGTH_SHORT).show();
        }
    }

    // 按包名启动应用（用系统提供的默认启动 Intent，避免写死 Activity）
    private void launchByPkg(String pkg) {
        Intent intent = pm.getLaunchIntentForPackage(pkg);
        if (intent == null) {
            Toast.makeText(this, "未找到应用：" + pkg, Toast.LENGTH_SHORT).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法启动：" + pkg, Toast.LENGTH_SHORT).show();
        }
    }

    // 底部块 tab：带边框、居中文字、按权重分配宽度、固定高度
    private TextView blockTab(String text, int sp, float weight, int height) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.BLACK);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setGravity(Gravity.CENTER);
        t.setBackground(borderDrawable());
        t.setLayoutParams(new LinearLayout.LayoutParams(0, height, weight));
        t.setClickable(true);
        return t;
    }

    private void showAppInfo(AppInfo info) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + info.pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开应用信息", Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemMenu(final AppInfo info, final boolean fromHome) {
        final CharSequence[] items;
        if (fromHome) {
            items = new CharSequence[]{"从常用移除", "应用信息"};
        } else {
            items = new CharSequence[]{
                    favPkgs.contains(info.pkg) ? "从常用移除" : "添加到常用", "应用信息"};
        }
        new AlertDialog.Builder(this)
                .setTitle(info.label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            if ("从常用移除".equals(items[0])) {
                                favPkgs.remove(info.pkg);
                                saveFavorites();
                                refreshHome();
                                Toast.makeText(MainActivity.this, "已从常用移除", Toast.LENGTH_SHORT).show();
                            } else {
                                if (favPkgs.size() >= MAX_HOME_APPS) {
                                    Toast.makeText(MainActivity.this,
                                            "首页最多 " + MAX_HOME_APPS + " 个，请先移除一个",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                favPkgs.add(info.pkg);
                                saveFavorites();
                                refreshHome();
                                Toast.makeText(MainActivity.this, "已添加到常用", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            showAppInfo(info);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadFavorites() {
        favPkgs.clear();
        // 旧版存的是 12 个，升级到 v2 时丢弃旧数据，重建为 8 个默认
        int ver = prefs.getInt("fav_version", 1);
        if (ver < FAV_VERSION) {
            // 必须清掉旧的 favorites 键，否则 initDefaultFavoritesIfNeeded 会误以为已初始化
            prefs.edit().remove("favorites").putInt("fav_version", FAV_VERSION).apply();
            return; // 等 loadApps 后按默认应用名建立 8 个
        }
        String saved = prefs.getString("favorites", null);
        if (saved == null) return; // 首次启动，等 loadApps 后按默认应用名建立
        for (String p : saved.split(",")) {
            if (!p.isEmpty() && favPkgs.size() < MAX_HOME_APPS) favPkgs.add(p);
        }
    }

    private void saveFavorites() {
        StringBuilder sb = new StringBuilder();
        for (String p : favPkgs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p);
        }
        prefs.edit().putString("favorites", sb.toString()).apply();
    }

    private void initDefaultFavoritesIfNeeded() {
        if (prefs.getString("favorites", null) != null) return;
        for (String label : DEFAULT_FAVORITES) {
            if (favPkgs.size() >= MAX_HOME_APPS) break;
            for (AppInfo a : allApps) {
                if (label.equals(a.label)) {
                    favPkgs.add(a.pkg);
                    break;
                }
            }
        }
        saveFavorites();
    }

    private void loadApps() {
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        List<AppInfo> apps = new ArrayList<>();
        String self = getPackageName();
        for (ResolveInfo ri : list) {
            if (ri.activityInfo == null) continue;
            if (self.equals(ri.activityInfo.packageName)) continue;
            AppInfo a = new AppInfo();
            CharSequence label = null;
            try {
                label = ri.loadLabel(pm);
            } catch (Throwable ignored) {}
            a.label = (label != null && label.length() > 0)
                    ? label.toString() : ri.activityInfo.packageName;
            a.pkg = ri.activityInfo.packageName;
            a.activity = ri.activityInfo.name;
            apps.add(a);
        }
        Collections.sort(apps, new Comparator<AppInfo>() {
            public int compare(AppInfo a, AppInfo b) { return a.label.compareTo(b.label); }
        });
        allApps = apps;
        initDefaultFavoritesIfNeeded();
        refreshHome();
        filter(search != null ? search.getText().toString() : "");
    }

    private void refreshHome() {
        // 严格按 favPkgs 的顺序排列，用户排序才能生效
        List<AppInfo> favs = new ArrayList<>();
        for (String pkg : favPkgs) {
            AppInfo found = findApp(pkg);
            if (found != null) favs.add(found);
        }
        // 极端情况下（首次匹配不到任何默认应用）兜底，避免首页空白
        if (favs.isEmpty() && !allApps.isEmpty()) {
            for (int i = 0; i < Math.min(allApps.size(), 12); i++) {
                favPkgs.add(allApps.get(i).pkg);
                favs.add(allApps.get(i));
            }
            saveFavorites();
        }
        homeAdapter.setData(favs);
    }

    private AppInfo findApp(String pkg) {
        for (AppInfo a : allApps) {
            if (a.pkg.equals(pkg)) return a;
        }
        return null;
    }

    private void filter(String q) {
        List<AppInfo> result;
        if (q == null || q.trim().isEmpty()) {
            result = allApps;
        } else {
            String key = q.trim().toLowerCase();
            result = new ArrayList<>();
            for (AppInfo a : allApps) {
                if (a.label.toLowerCase().contains(key) || a.pkg.toLowerCase().contains(key)) {
                    result.add(a);
                }
            }
        }
        allAdapter.setData(result);
    }

    private void showHomeGuide() {
        new AlertDialog.Builder(this)
                .setTitle("设为默认桌面")
                .setMessage("两种方式任选其一：\n\n1）按设备的 Home 键，在弹出列表中选择「墨屏桌面」，勾选「始终」。\n\n2）点下方按钮打开系统默认应用设置，手动选择桌面应用。")
                .setPositiveButton("打开系统设置", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                        } catch (Exception e) {
                            try {
                                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
                            } catch (Exception e2) {
                                Toast.makeText(MainActivity.this, "本机未开放该设置入口，请用 Home 键选择", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .setNegativeButton("知道了", null)
                .show();
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#888888"));
        return v;
    }

    private GradientDrawable borderDrawable() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.WHITE);
        gd.setStroke(1, Color.parseColor("#555555"));
        gd.setCornerRadius(dp(2));
        return gd;
    }

    private TextView borderButton(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.BLACK);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setGravity(Gravity.CENTER);
        t.setBackground(borderDrawable());
        t.setPadding(dp(10), dp(6), dp(10), dp(6));
        t.setClickable(true);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int screenWidthDp() {
        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);
        return (int) (m.widthPixels / m.density);
    }

    private int screenHeightPx() {
        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);
        return m.heightPixels;
    }

    private class AppAdapter extends BaseAdapter {
        private final List<AppInfo> data = new ArrayList<>();
        private final int iconSize;
        private final int textSize;

        AppAdapter(int iconSize, int textSize) {
            this.iconSize = iconSize;
            this.textSize = textSize;
        }

        void setData(List<AppInfo> d) {
            data.clear();
            data.addAll(d);
            notifyDataSetChanged();
        }

        public int getCount() { return data.size(); }
        public Object getItem(int p) { return data.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View convertView, ViewGroup parent) {
            LinearLayout item;
            ImageView icon;
            TextView label;
            if (convertView == null) {
                item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                item.setBackgroundColor(Color.WHITE);
                item.setStateListAnimator(null);
                icon = new ImageView(MainActivity.this);
                icon.setLayoutParams(new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                label = new TextView(MainActivity.this);
                label.setTextColor(Color.BLACK);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setPadding(0, dp(2), 0, 0);
                item.addView(icon);
                item.addView(label);
                item.setTag(new View[]{icon, label});
            } else {
                item = (LinearLayout) convertView;
                View[] tags = (View[]) item.getTag();
                icon = (ImageView) tags[0];
                label = (TextView) tags[1];
            }
            AppInfo a = data.get(pos);
            // 管理模式下用 [✓]/[ ] 前缀标明该应用是否在首页
            String text = a.label;
            if (editMode) {
                text = (favPkgs.contains(a.pkg) ? "✓ " : "· ") + a.label;
            }
            label.setText(text);
            DrawableHolder.set(icon, pm, a.pkg, a.activity);
            return item;
        }
    }

    // 缓存已灰度化的应用图标 Bitmap：像素级转换，不依赖 ColorFilter
    private static class DrawableHolder {
        private static final android.util.LruCache<String, android.graphics.Bitmap> cache =
                new android.util.LruCache<>(120);

        static void set(ImageView iv, PackageManager pm, String pkg, String act) {
            String key = pkg + "/" + act;
            android.graphics.Bitmap bm = cache.get(key);
            if (bm == null) {
                android.graphics.drawable.Drawable d = null;
                try {
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    i.setClassName(pkg, act);
                    ResolveInfo ri = pm.resolveActivity(i, 0);
                    d = ri != null ? ri.loadIcon(pm) : pm.getApplicationIcon(pkg);
                } catch (Exception e) {
                    try { d = pm.getApplicationIcon(pkg); } catch (Exception e2) { d = null; }
                }
                if (d != null) {
                    bm = toGray(d);
                    if (bm != null) cache.put(key, bm);
                }
            }
            iv.setImageBitmap(bm);
        }

        private static android.graphics.Bitmap toGray(android.graphics.drawable.Drawable d) {
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            if (w <= 0 || h <= 0) return null;
            float scale = Math.min(1f, 256f / (float) Math.max(w, h));
            w = Math.max(1, (int) (w * scale));
            h = Math.max(1, (int) (h * scale));
            android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(
                    w, h, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(bm);
            d.setBounds(0, 0, w, h);
            d.draw(c);
            int[] px = new int[w * h];
            bm.getPixels(px, 0, w, 0, 0, w, h);
            for (int i = 0; i < px.length; i++) {
                int p = px[i];
                int a = (p >>> 24) & 0xFF;
                int r = (p >>> 16) & 0xFF;
                int g = (p >>> 8) & 0xFF;
                int b = p & 0xFF;
                int gray = (int) (0.299f * r + 0.587f * g + 0.114f * b);
                px[i] = (a << 24) | (gray << 16) | (gray << 8) | gray;
            }
            bm.setPixels(px, 0, w, 0, 0, w, h);
            return bm;
        }
    }
}
