package com.wms.homemanager.terminal.buffer;

/**
 * 终端缓冲区类
 * 管理屏幕缓冲区和滚动历史
 */
public class TerminalBuffer {

    // 屏幕缓冲区
    private char[][] screenBuffer;
    // 字符样式缓冲区
    private TextStyle[][] styleBuffer;
    // 滚动缓冲区（环形）
    private char[][] scrollBuffer;
    private TextStyle[][] scrollStyleBuffer;

    // 缓冲区尺寸
    private int columns;
    private int rows;
    private int scrollBufferSize;

    // 环形缓冲区指针
    private int scrollBufferHead;
    private int scrollBufferCount;

    // 光标位置
    private int cursorRow;
    private int cursorCol;

    // 滚动区域
    private int scrollTop;
    private int scrollBottom;

    // 滚动偏移（用于查看历史）
    private int scrollOffset;

    // 光标可见性
    private boolean cursorVisible;

    // 光标样式
    private CursorStyle cursorStyle;

    /**
     * 光标样式枚举
     */
    public enum CursorStyle {
        BLOCK,      // 块状
        UNDERLINE,  // 下划线
        BAR         // 竖线
    }

    /**
     * 文本样式类
     */
    public static class TextStyle {
        public int foregroundColor = -1;  // -1 表示默认颜色
        public int backgroundColor = -1;
        public boolean bold = false;
        public boolean italic = false;
        public boolean underline = false;
        public boolean blink = false;
        public boolean inverse = false;
        public boolean hidden = false;
        public boolean strikethrough = false;

        public TextStyle() {}

        public TextStyle(TextStyle other) {
            this.foregroundColor = other.foregroundColor;
            this.backgroundColor = other.backgroundColor;
            this.bold = other.bold;
            this.italic = other.italic;
            this.underline = other.underline;
            this.blink = other.blink;
            this.inverse = other.inverse;
            this.hidden = other.hidden;
            this.strikethrough = other.strikethrough;
        }

        public void reset() {
            foregroundColor = -1;
            backgroundColor = -1;
            bold = false;
            italic = false;
            underline = false;
            blink = false;
            inverse = false;
            hidden = false;
            strikethrough = false;
        }
    }

    /**
     * 构造函数
     * @param columns 列数
     * @param rows 行数
     * @param scrollBufferSize 滚动缓冲区大小
     */
    public TerminalBuffer(int columns, int rows, int scrollBufferSize) {
        this.columns = columns;
        this.rows = rows;
        this.scrollBufferSize = scrollBufferSize;

        initBuffers();
    }

    /**
     * 初始化缓冲区
     */
    private void initBuffers() {
        screenBuffer = new char[rows][columns];
        styleBuffer = new TextStyle[rows][columns];

        scrollBuffer = new char[scrollBufferSize][columns];
        scrollStyleBuffer = new TextStyle[scrollBufferSize][columns];

        // 初始化样式缓冲区
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                styleBuffer[i][j] = new TextStyle();
                screenBuffer[i][j] = ' ';
            }
        }

        for (int i = 0; i < scrollBufferSize; i++) {
            for (int j = 0; j < columns; j++) {
                scrollStyleBuffer[i][j] = new TextStyle();
                scrollBuffer[i][j] = ' ';
            }
        }

        // 初始化光标
        cursorRow = 0;
        cursorCol = 0;
        cursorVisible = true;
        cursorStyle = CursorStyle.BLOCK;

        // 初始化滚动区域
        scrollTop = 0;
        scrollBottom = rows - 1;
        scrollOffset = 0;

        scrollBufferHead = 0;
        scrollBufferCount = 0;
    }

    /**
     * 调整缓冲区大小
     */
    public void resize(int newColumns, int newRows) {
        // TODO: 实现缓冲区大小调整
        this.columns = newColumns;
        this.rows = newRows;
        initBuffers();
    }

    /**
     * 在当前光标位置写入字符
     */
    public void writeChar(char c) {
        if (cursorCol >= columns) {
            cursorCol = 0;
            cursorRow++;
            if (cursorRow > scrollBottom) {
                scrollUp();
                cursorRow = scrollBottom;
            }
        }
        screenBuffer[cursorRow][cursorCol] = c;
        cursorCol++;
    }

    /**
     * 写入带样式的字符
     */
    public void writeChar(char c, TextStyle style) {
        if (cursorCol >= columns) {
            cursorCol = 0;
            cursorRow++;
            if (cursorRow > scrollBottom) {
                scrollUp();
                cursorRow = scrollBottom;
            }
        }
        screenBuffer[cursorRow][cursorCol] = c;
        styleBuffer[cursorRow][cursorCol] = new TextStyle(style);
        cursorCol++;
    }

    /**
     * 滚动屏幕向上一行
     */
    private void scrollUp() {
        // 将顶部行移入滚动缓冲区
        if (scrollBufferCount < scrollBufferSize) {
            scrollBufferCount++;
        }

        int scrollIndex = scrollBufferHead;
        System.arraycopy(screenBuffer[scrollTop], 0, scrollBuffer[scrollIndex], 0, columns);
        System.arraycopy(styleBuffer[scrollTop], 0, scrollStyleBuffer[scrollIndex], 0, columns);

        scrollBufferHead = (scrollBufferHead + 1) % scrollBufferSize;

        // 滚动屏幕缓冲区
        for (int i = scrollTop; i < scrollBottom; i++) {
            System.arraycopy(screenBuffer[i + 1], 0, screenBuffer[i], 0, columns);
            System.arraycopy(styleBuffer[i + 1], 0, styleBuffer[i], 0, columns);
        }

        // 清除底部行
        clearLine(scrollBottom);
    }

    /**
     * 清除指定行
     */
    public void clearLine(int row) {
        for (int i = 0; i < columns; i++) {
            screenBuffer[row][i] = ' ';
            styleBuffer[row][i].reset();
        }
    }

    /**
     * 清除屏幕
     */
    public void clearScreen() {
        for (int i = 0; i < rows; i++) {
            clearLine(i);
        }
        cursorRow = 0;
        cursorCol = 0;
    }

    // ========== 光标操作 ==========

    public void setCursor(int row, int col) {
        cursorRow = Math.max(0, Math.min(row, rows - 1));
        cursorCol = Math.max(0, Math.min(col, columns - 1));
    }

    public void moveCursor(int deltaRow, int deltaCol) {
        cursorRow = Math.max(0, Math.min(cursorRow + deltaRow, rows - 1));
        cursorCol = Math.max(0, Math.min(cursorCol + deltaCol, columns - 1));
    }

    public void cursorUp(int n) { moveCursor(-n, 0); }
    public void cursorDown(int n) { moveCursor(n, 0); }
    public void cursorForward(int n) { moveCursor(0, n); }
    public void cursorBackward(int n) { moveCursor(0, -n); }

    public void cursorHome() {
        cursorRow = 0;
        cursorCol = 0;
    }

    public void carriageReturn() {
        cursorCol = 0;
    }

    public void lineFeed() {
        cursorRow++;
        if (cursorRow > scrollBottom) {
            scrollUp();
            cursorRow = scrollBottom;
        }
    }

    public void backspace() {
        if (cursorCol > 0) {
            cursorCol--;
        }
    }

    public void tab() {
        int nextTab = ((cursorCol / 8) + 1) * 8;
        cursorCol = Math.min(nextTab, columns - 1);
    }

    // ========== 光标位置保存/恢复 ==========

    private int savedCursorRow = 0;
    private int savedCursorCol = 0;

    public void saveCursorPosition() {
        savedCursorRow = cursorRow;
        savedCursorCol = cursorCol;
    }

    public void restoreCursorPosition() {
        cursorRow = savedCursorRow;
        cursorCol = savedCursorCol;
    }

    // ========== 字符操作 ==========

    /**
     * 设置指定位置的字符和样式
     */
    public void setChar(int row, int col, char c, TextStyle style) {
        if (row >= 0 && row < rows && col >= 0 && col < columns) {
            screenBuffer[row][col] = c;
            if (style != null) {
                styleBuffer[row][col] = new TextStyle(style);
            } else {
                styleBuffer[row][col] = new TextStyle();
            }
        }
    }

    /**
     * 清除指定位置的字符
     */
    public void clearChar(int row, int col) {
        if (row >= 0 && row < rows && col >= 0 && col < columns) {
            screenBuffer[row][col] = ' ';
            styleBuffer[row][col].reset();
        }
    }

    /**
     * 清除从光标到行尾
     */
    public void clearToEndOfLine() {
        int row = cursorRow;
        for (int col = cursorCol; col < columns; col++) {
            screenBuffer[row][col] = ' ';
            styleBuffer[row][col].reset();
        }
    }

    /**
     * 清除从行首到光标
     */
    public void clearFromStartOfLine() {
        int row = cursorRow;
        for (int col = 0; col <= cursorCol; col++) {
            screenBuffer[row][col] = ' ';
            styleBuffer[row][col].reset();
        }
    }

    /**
     * 插入行
     */
    public void insertLines(int n) {
        int row = cursorRow;
        for (int i = 0; i < n; i++) {
            // 将底部行移出滚动区域
            // 将行向下移动
            for (int r = scrollBottom; r > row; r--) {
                System.arraycopy(screenBuffer[r - 1], 0, screenBuffer[r], 0, columns);
                System.arraycopy(styleBuffer[r - 1], 0, styleBuffer[r], 0, columns);
            }
            // 清除当前行
            clearLine(row);
        }
    }

    /**
     * 删除行
     */
    public void deleteLines(int n) {
        int row = cursorRow;
        for (int i = 0; i < n; i++) {
            // 将行向上移动
            for (int r = row; r < scrollBottom; r++) {
                System.arraycopy(screenBuffer[r + 1], 0, screenBuffer[r], 0, columns);
                System.arraycopy(styleBuffer[r + 1], 0, styleBuffer[r], 0, columns);
            }
            // 清除底部行
            clearLine(scrollBottom);
        }
    }

    /**
     * 删除字符（左侧字符向左移动）
     */
    public void deleteCharacters(int n) {
        int row = cursorRow;
        int col = cursorCol;
        // 将右侧字符向左移动
        for (int i = col; i < columns - n; i++) {
            if (i + n < columns) {
                screenBuffer[row][i] = screenBuffer[row][i + n];
                styleBuffer[row][i] = new TextStyle(styleBuffer[row][i + n]);
            }
        }
        // 清除右侧空出的位置
        for (int i = columns - n; i < columns; i++) {
            screenBuffer[row][i] = ' ';
            styleBuffer[row][i].reset();
        }
    }

    /**
     * 插入字符（右侧字符向右移动）
     */
    public void insertCharacters(int n) {
        int row = cursorRow;
        int col = cursorCol;
        // 将字符向右移动
        for (int i = columns - 1; i >= col + n; i--) {
            if (i - n >= 0) {
                screenBuffer[row][i] = screenBuffer[row][i - n];
                styleBuffer[row][i] = new TextStyle(styleBuffer[row][i - n]);
            }
        }
        // 清除插入的位置
        for (int i = col; i < col + n && i < columns; i++) {
            screenBuffer[row][i] = ' ';
            styleBuffer[row][i].reset();
        }
    }

    /**
     * 清除字符（不移动其他字符）
     */
    public void eraseCharacters(int n) {
        int row = cursorRow;
        int col = cursorCol;
        for (int i = 0; i < n && (col + i) < columns; i++) {
            screenBuffer[row][col + i] = ' ';
            styleBuffer[row][col + i].reset();
        }
    }

    /**
     * 向下滚动（内容向下移动，顶部插入空行）
     */
    public void scrollDown(int n) {
        for (int i = 0; i < n; i++) {
            // 将行向下移动
            for (int r = scrollTop; r < scrollBottom; r++) {
                System.arraycopy(screenBuffer[r + 1], 0, screenBuffer[r], 0, columns);
                System.arraycopy(styleBuffer[r + 1], 0, styleBuffer[r], 0, columns);
            }
            // 清除底部行
            clearLine(scrollBottom);
        }
    }

    // ========== Getters ==========

    public int getColumns() { return columns; }
    public int getRows() { return rows; }
    public int getCursorRow() { return cursorRow; }
    public int getCursorCol() { return cursorCol; }
    public boolean isCursorVisible() { return cursorVisible; }
    public CursorStyle getCursorStyle() { return cursorStyle; }

    public char getChar(int row, int col) {
        return screenBuffer[row][col];
    }

    public TextStyle getStyle(int row, int col) {
        return styleBuffer[row][col];
    }

    public char[] getRow(int row) {
        return screenBuffer[row];
    }

    public TextStyle[] getRowStyles(int row) {
        return styleBuffer[row];
    }

    // ========== 滚动缓冲区 ==========

    public int getScrollOffset() { return scrollOffset; }

    public void setScrollOffset(int offset) {
        scrollOffset = Math.max(0, Math.min(offset, scrollBufferCount));
    }

    public boolean canScrollUp() {
        return scrollOffset < scrollBufferCount;
    }

    public boolean canScrollDown() {
        return scrollOffset > 0;
    }

    /**
     * 获取滚动缓冲区中的行
     */
    public char[] getScrollLine(int offset) {
        if (offset >= scrollBufferCount) {
            return null;
        }
        int index = (scrollBufferHead - scrollBufferCount + offset + scrollBufferSize) % scrollBufferSize;
        return scrollBuffer[index];
    }

    /**
     * 获取滚动缓冲区中的行样式
     */
    public TextStyle[] getScrollStyleLine(int offset) {
        if (offset >= scrollBufferCount) {
            return null;
        }
        int index = (scrollBufferHead - scrollBufferCount + offset + scrollBufferSize) % scrollBufferSize;
        return scrollStyleBuffer[index];
    }

    /**
     * 获取滚动缓冲区行数
     */
    public int getScrollBufferCount() {
        return scrollBufferCount;
    }

    // ========== 滚动区域 ==========

    public void setScrollRegion(int top, int bottom) {
        scrollTop = Math.max(0, top);
        scrollBottom = Math.min(rows - 1, bottom);
    }

    public void resetScrollRegion() {
        scrollTop = 0;
        scrollBottom = rows - 1;
    }
}
