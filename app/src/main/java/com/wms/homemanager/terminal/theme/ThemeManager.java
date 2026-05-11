package com.wms.homemanager.terminal.theme;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端主题管理器
 * 管理预设主题和自定义主题
 */
public class ThemeManager {

    private static final String PREFS_NAME = "terminal_themes";
    private static final String KEY_CURRENT_THEME = "current_theme";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_CUSTOM_THEMES = "custom_themes";

    // 预设主题
    private static final TerminalTheme[] PRESET_THEMES = {
            createDefaultGreen(),
            createDarkNight(),
            createSolarizedDark(),
            createSolarizedLight(),
            createMonokai(),
            createDracula()
    };

    // 上下文
    private Context context;

    // 当前主题
    private TerminalTheme currentTheme;
    private String currentThemeName;

    // 自定义主题
    private List<TerminalTheme> customThemes = new ArrayList<>();

    // 字体大小
    private float fontSize = 14f;

    // 监听器
    private ThemeChangeListener listener;

    /**
     * 主题变更监听器
     */
    public interface ThemeChangeListener {
        void onThemeChanged(TerminalTheme theme);
        void onFontSizeChanged(float fontSize);
    }

    public ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        loadSettings();
    }

    // ========== 预设主题创建 ==========

    private static TerminalTheme createDefaultGreen() {
        TerminalTheme theme = new TerminalTheme(
                "Default Green",
                0xFF00FF88, // 霓虹绿
                0xFF0D1117, // 深色背景
                0xFF00FF88,
                0x8000D4FF
        );
        setStandardPalette(theme);
        return theme;
    }

    private static TerminalTheme createDarkNight() {
        TerminalTheme theme = new TerminalTheme(
                "Dark Night",
                0xFFFFFFFF,
                0xFF0A0A0A,
                0xFFFFFFFF,
                0x80FFFFFF
        );
        setStandardPalette(theme);
        return theme;
    }

    private static TerminalTheme createSolarizedDark() {
        TerminalTheme theme = new TerminalTheme(
                "Solarized Dark",
                0xFF839496,
                0xFF002B36,
                0xFF93A1A1,
                0x80839496
        );
        // Solarized 调色板
        int[] palette = {
                0xFF073642, 0xFFDC322F, 0xFF859900, 0xFFB58900,
                0xFF268BD2, 0xFFD33682, 0xFF2AA198, 0xFFEEE8D5,
                0xFF002B36, 0xFFCB4B16, 0xFF586E75, 0xFF657B83,
                0xFF839496, 0xFF6C71C4, 0xFF93A1A1, 0xFFFDF6E3
        };
        theme.setColorPalette(palette);
        return theme;
    }

    private static TerminalTheme createSolarizedLight() {
        TerminalTheme theme = new TerminalTheme(
                "Solarized Light",
                0xFF657B83,
                0xFFFDF6E3,
                0xFF657B83,
                0x80657B83
        );
        // Solarized Light 调色板
        int[] palette = {
                0xFF073642, 0xFFDC322F, 0xFF859900, 0xFFB58900,
                0xFF268BD2, 0xFFD33682, 0xFF2AA198, 0xFFEEE8D5,
                0xFF002B36, 0xFFCB4B16, 0xFF586E75, 0xFF657B83,
                0xFF839496, 0xFF6C71C4, 0xFF93A1A1, 0xFFFDF6E3
        };
        theme.setColorPalette(palette);
        return theme;
    }

    private static TerminalTheme createMonokai() {
        TerminalTheme theme = new TerminalTheme(
                "Monokai",
                0xFFF8F8F2,
                0xFF272822,
                0xFFF8F8F2,
                0x80F8F8F2
        );
        // Monokai 调色板
        int[] palette = {
                0xFF272822, 0xFFF92672, 0xFFA6E22E, 0xFFF4BF75,
                0xFF66D9EF, 0xFFAE81FF, 0xFFA1EFE4, 0xFFF8F8F2,
                0xFF75715E, 0xFFF92672, 0xFFA6E22E, 0xFFF4BF75,
                0xFF66D9EF, 0xFFAE81FF, 0xFFA1EFE4, 0xFFF9F8F5
        };
        theme.setColorPalette(palette);
        return theme;
    }

    private static TerminalTheme createDracula() {
        TerminalTheme theme = new TerminalTheme(
                "Dracula",
                0xFFF8F8F2,
                0xFF282A36,
                0xFFF8F8F2,
                0x80BD93F9
        );
        // Dracula 调色板
        int[] palette = {
                0xFF21222C, 0xFFFF5555, 0xFF50FA7B, 0xFFF1FA8C,
                0xFFBD93F9, 0xFFFF79C6, 0xFF8BE9FD, 0xFFF8F8F2,
                0xFF6272A4, 0xFFFF5555, 0xFF50FA7B, 0xFFF1FA8C,
                0xFFBD93F9, 0xFFFF79C6, 0xFF8BE9FD, 0xFFFFFFFF
        };
        theme.setColorPalette(palette);
        return theme;
    }

    private static void setStandardPalette(TerminalTheme theme) {
        // 标准 16色调色板
        int[] palette = {
                0xFF000000, 0xFFCD0000, 0xFF00CD00, 0xFFCDCD00,
                0xFF0000EE, 0xFFCD00CD, 0xFF00CDCD, 0xFFE5E5E5,
                0xFF7F7F7F, 0xFFFF0000, 0xFF00FF00, 0xFFFFFF00,
                0xFF5C5CFF, 0xFFFF00FF, 0xFF00FFFF, 0xFFFFFFFF
        };
        theme.setColorPalette(palette);
    }

    // ========== 主题管理 ==========

    /**
     * 获取预设主题列表
     */
    public List<TerminalTheme> getPresetThemes() {
        List<TerminalTheme> themes = new ArrayList<>();
        for (TerminalTheme theme : PRESET_THEMES) {
            themes.add(theme);
        }
        return themes;
    }

    /**
     * 获取自定义主题列表
     */
    public List<TerminalTheme> getCustomThemes() {
        return new ArrayList<>(customThemes);
    }

    /**
     * 获取所有主题
     */
    public List<TerminalTheme> getAllThemes() {
        List<TerminalTheme> themes = getPresetThemes();
        themes.addAll(customThemes);
        return themes;
    }

    /**
     * 获取当前主题
     */
    public TerminalTheme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * 设置当前主题
     */
    public void setCurrentTheme(TerminalTheme theme) {
        this.currentTheme = theme;
        this.currentThemeName = theme.getName();
        saveSettings();

        if (listener != null) {
            listener.onThemeChanged(theme);
        }
    }

    /**
     * 根据名称设置主题
     */
    public boolean setThemeByName(String name) {
        // 查找预设主题
        for (TerminalTheme theme : PRESET_THEMES) {
            if (theme.getName().equals(name)) {
                setCurrentTheme(theme);
                return true;
            }
        }
        // 查找自定义主题
        for (TerminalTheme theme : customThemes) {
            if (theme.getName().equals(name)) {
                setCurrentTheme(theme);
                return true;
            }
        }
        return false;
    }

    /**
     * 添加自定义主题
     */
    public void addCustomTheme(TerminalTheme theme) {
        customThemes.add(theme);
        saveCustomThemes();
    }

    /**
     * 删除自定义主题
     */
    public void removeCustomTheme(int index) {
        if (index >= 0 && index < customThemes.size()) {
            customThemes.remove(index);
            saveCustomThemes();
        }
    }

    // ========== 字体大小 ==========

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = Math.max(8f, Math.min(32f, fontSize));
        saveSettings();

        if (listener != null) {
            listener.onFontSizeChanged(this.fontSize);
        }
    }

    // ========== 设置保存/加载 ==========

    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 加载字体大小
        fontSize = prefs.getFloat(KEY_FONT_SIZE, 14f);

        // 加载当前主题名称
        currentThemeName = prefs.getString(KEY_CURRENT_THEME, "Default Green");

        // 加载自定义主题
        loadCustomThemes();

        // 设置当前主题
        if (!setThemeByName(currentThemeName)) {
            currentTheme = PRESET_THEMES[0];
        }
    }

    private void saveSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CURRENT_THEME, currentThemeName)
                .putFloat(KEY_FONT_SIZE, fontSize)
                .apply();
    }

    private void loadCustomThemes() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String themesJson = prefs.getString(KEY_CUSTOM_THEMES, "[]");

        try {
            JSONArray array = new JSONArray(themesJson);
            customThemes.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                TerminalTheme theme = jsonToTheme(obj);
                if (theme != null) {
                    customThemes.add(theme);
                }
            }
        } catch (JSONException e) {
            // 忽略解析错误
        }
    }

    private void saveCustomThemes() {
        try {
            JSONArray array = new JSONArray();
            for (TerminalTheme theme : customThemes) {
                array.put(themeToJson(theme));
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_CUSTOM_THEMES, array.toString()).apply();
        } catch (Exception e) {
            // 忽略保存错误
        }
    }

    private JSONObject themeToJson(TerminalTheme theme) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", theme.getName());
        obj.put("foreground", theme.getForegroundColor());
        obj.put("background", theme.getBackgroundColor());
        obj.put("cursor", theme.getCursorColor());
        obj.put("selection", theme.getSelectionColor());

        JSONArray palette = new JSONArray();
        for (int color : theme.getColorPalette()) {
            palette.put(color);
        }
        obj.put("palette", palette);

        return obj;
    }

    private TerminalTheme jsonToTheme(JSONObject obj) {
        try {
            TerminalTheme theme = new TerminalTheme();
            theme.setName(obj.getString("name"));
            theme.setForegroundColor(obj.getInt("foreground"));
            theme.setBackgroundColor(obj.getInt("background"));
            theme.setCursorColor(obj.getInt("cursor"));
            theme.setSelectionColor(obj.getInt("selection"));

            JSONArray palette = obj.getJSONArray("palette");
            int[] colors = new int[16];
            for (int i = 0; i < 16 && i < palette.length(); i++) {
                colors[i] = palette.getInt(i);
            }
            theme.setColorPalette(colors);

            return theme;
        } catch (JSONException e) {
            return null;
        }
    }

    // ========== 监听器 ==========

    public void setListener(ThemeChangeListener listener) {
        this.listener = listener;
    }
}
