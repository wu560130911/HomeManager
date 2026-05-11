package com.wms.homemanager.terminal.theme;

/**
 * 终端主题配置
 */
public class TerminalTheme {

    private String name;
    private int foregroundColor;
    private int backgroundColor;
    private int cursorColor;
    private int selectionColor;
    private int[] colorPalette; // 16色调色板

    public TerminalTheme() {
        colorPalette = new int[16];
    }

    public TerminalTheme(String name, int foreground, int background, int cursor, int selection) {
        this.name = name;
        this.foregroundColor = foreground;
        this.backgroundColor = background;
        this.cursorColor = cursor;
        this.selectionColor = selection;
        this.colorPalette = new int[16];
    }

    // ========== Getters/Setters ==========

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(int foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getCursorColor() {
        return cursorColor;
    }

    public void setCursorColor(int cursorColor) {
        this.cursorColor = cursorColor;
    }

    public int getSelectionColor() {
        return selectionColor;
    }

    public void setSelectionColor(int selectionColor) {
        this.selectionColor = selectionColor;
    }

    public int[] getColorPalette() {
        return colorPalette;
    }

    public void setColorPalette(int[] colorPalette) {
        if (colorPalette != null && colorPalette.length == 16) {
            this.colorPalette = colorPalette;
        }
    }

    public int getColor(int index) {
        if (index >= 0 && index < 16) {
            return colorPalette[index];
        }
        return foregroundColor;
    }

    public void setColor(int index, int color) {
        if (index >= 0 && index < 16) {
            colorPalette[index] = color;
        }
    }
}
