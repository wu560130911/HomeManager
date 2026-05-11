package com.wms.homemanager.terminal;

import com.wms.homemanager.terminal.buffer.TerminalBuffer;
import com.wms.homemanager.terminal.parser.AnsiParser;

/**
 * 终端模拟器核心类
 * 连接 TerminalBuffer 和 AnsiParser，处理终端状态
 */
public class TerminalEmulator implements AnsiParser.AnsiParserCallback {

    // 终端缓冲区
    private TerminalBuffer buffer;

    // ANSI 解析器
    private AnsiParser parser;

    // 当前文本样式
    private TerminalBuffer.TextStyle currentStyle;

    // 终端配置
    private int columns = 80;
    private int rows = 24;
    private int scrollBufferSize = 10000;

    // 终端标题
    private String title;

    // 终端模式
    private boolean insertMode = false;
    private boolean originMode = false;
    private boolean autoWrapMode = true;
    private boolean newlineMode = false;

    // 监听器
    private TerminalEmulatorListener listener;

    /**
     * 终端模拟器监听器
     */
    public interface TerminalEmulatorListener {
        /**
         * 终端标题变化
         */
        void onTitleChanged(String title);

        /**
         * 终端内容更新
         */
        void onBufferUpdated();

        /**
         * 终端响铃
         */
        void onBell();
    }

    public TerminalEmulator() {
        this.currentStyle = new TerminalBuffer.TextStyle();
        this.buffer = new TerminalBuffer(columns, rows, scrollBufferSize);
        this.parser = new AnsiParser(this);
    }

    public TerminalEmulator(int columns, int rows, int scrollBufferSize) {
        this.columns = columns;
        this.rows = rows;
        this.scrollBufferSize = scrollBufferSize;
        this.currentStyle = new TerminalBuffer.TextStyle();
        this.buffer = new TerminalBuffer(columns, rows, scrollBufferSize);
        this.parser = new AnsiParser(this);
    }

    /**
     * 处理输入数据
     */
    public void processInput(byte[] data, int offset, int length) {
        // Log.d("TerminalEmulator", "processInput: " + length + " bytes");
        parser.parse(data, offset, length);
        notifyBufferUpdated();
    }

    // ========== AnsiParser.AnsiParserCallback 实现 ==========

    @Override
    public void onChar(char c) {
        buffer.writeChar(c);
    }

    @Override
    public void onChar(char c, TerminalBuffer.TextStyle style) {
        if (style != null) {
            buffer.writeChar(c, style);
        } else {
            buffer.writeChar(c);
        }
    }

    @Override
    public void onCsiSequence(char command, int[] params) {
        handleCsiSequence(command, params);
    }

    @Override
    public void onOscSequence(int command, String data) {
        handleOscSequence(command, data);
    }

    @Override
    public void onControlChar(char c) {
        handleControlChar(c);
    }

    @Override
    public TerminalBuffer.TextStyle getCurrentStyle() {
        return currentStyle;
    }

    @Override
    public void setCurrentStyle(TerminalBuffer.TextStyle style) {
        if (style != null) {
            this.currentStyle = style;
        }
    }

    // ========== CSI 序列处理 ==========

    private void handleCsiSequence(char command, int[] params) {
        // 大多数命令参数缺失时默认为 1，但 J 和 K 命令默认为 0
        int defaultParam = (command == 'J' || command == 'K') ? 0 : 1;
        int p1 = params.length > 0 ? params[0] : defaultParam;
        int p2 = params.length > 1 ? params[1] : 1;

        switch (command) {
            // 光标移动
            case 'A': // CUU - Cursor Up
                buffer.cursorUp(p1);
                break;
            case 'B': // CUD - Cursor Down
                buffer.cursorDown(p1);
                break;
            case 'C': // CUF - Cursor Forward
                buffer.cursorForward(p1);
                break;
            case 'D': // CUB - Cursor Backward
                buffer.cursorBackward(p1);
                break;
            case 'E': // CNL - Cursor Next Line
                buffer.cursorDown(p1);
                buffer.carriageReturn();
                break;
            case 'F': // CPL - Cursor Previous Line
                buffer.cursorUp(p1);
                buffer.carriageReturn();
                break;
            case 'G': // CHA - Cursor Horizontal Absolute
                buffer.setCursor(buffer.getCursorRow(), p1 - 1);
                break;
            case 'H': // CUP - Cursor Position
            case 'f': // HVP - Horizontal and Vertical Position
                buffer.setCursor(p1 - 1, p2 - 1);
                break;
            case 'J': // ED - Erase in Display
                eraseInDisplay(p1);
                break;
            case 'K': // EL - Erase in Line
                eraseInLine(p1);
                break;
            case 'L': // IL - Insert Lines
                insertLines(p1);
                break;
            case 'M': // DL - Delete Lines
                deleteLines(p1);
                break;
            case 'P': // DCH - Delete Characters
                deleteCharacters(p1);
                break;
            case '@': // ICH - Insert Characters
                insertCharacters(p1);
                break;
            case 'S': // SU - Scroll Up
                scrollUp(p1);
                break;
            case 'T': // SD - Scroll Down
                scrollDown(p1);
                break;
            case 'X': // ECH - Erase Characters
                eraseCharacters(p1);
                break;
            case 'd': // VPA - Vertical Position Absolute
                buffer.setCursor(p1 - 1, buffer.getCursorCol());
                break;
            case 'm': // SGR - Select Graphic Rendition
                handleSgr(params);
                break;
            case 'r': // DECSTBM - Set Scrolling Region
                buffer.setScrollRegion(p1 - 1, p2 - 1);
                break;
            case 's': // SCP - Save Cursor Position
                saveCursorPosition();
                break;
            case 'u': // RCP - Restore Cursor Position
                restoreCursorPosition();
                break;
            case 'h': // SM - Set Mode
                setMode(params, true);
                break;
            case 'l': // RM - Reset Mode
                setMode(params, false);
                break;
            case 'n': // DSR - Device Status Report
                // TODO: 实现设备状态报告
                break;
            case 'c': // DA - Device Attributes
                // TODO: 实现设备属性
                break;
            default:
                // 未知命令，忽略
                break;
        }
    }

    // ========== SGR 颜色处理 ==========

    private void handleSgr(int[] params) {
        if (params.length == 0) {
            currentStyle.reset();
            return;
        }

        for (int i = 0; i < params.length; i++) {
            int p = params[i];

            if (p == 0) {
                // 重置所有属性
                currentStyle.reset();
            } else if (p == 1) {
                currentStyle.bold = true;
            } else if (p == 3) {
                currentStyle.italic = true;
            } else if (p == 4) {
                currentStyle.underline = true;
            } else if (p == 5 || p == 6) {
                currentStyle.blink = true;
            } else if (p == 7) {
                currentStyle.inverse = true;
            } else if (p == 8) {
                currentStyle.hidden = true;
            } else if (p == 9) {
                currentStyle.strikethrough = true;
            } else if (p == 22) {
                currentStyle.bold = false;
            } else if (p == 23) {
                currentStyle.italic = false;
            } else if (p == 24) {
                currentStyle.underline = false;
            } else if (p == 25) {
                currentStyle.blink = false;
            } else if (p == 27) {
                currentStyle.inverse = false;
            } else if (p == 28) {
                currentStyle.hidden = false;
            } else if (p == 29) {
                currentStyle.strikethrough = false;
            } else if (p >= 30 && p <= 37) {
                // 标准16色前景色
                currentStyle.foregroundColor = p - 30;
            } else if (p == 38) {
                // 扩展前景色 (256色或真彩色)
                i = handleExtendedColor(params, i, true);
            } else if (p == 39) {
                // 默认前景色
                currentStyle.foregroundColor = -1;
            } else if (p >= 40 && p <= 47) {
                // 标准16色背景色
                currentStyle.backgroundColor = p - 40;
            } else if (p == 48) {
                // 扩展背景色 (256色或真彩色)
                i = handleExtendedColor(params, i, false);
            } else if (p == 49) {
                // 默认背景色
                currentStyle.backgroundColor = -1;
            } else if (p >= 90 && p <= 97) {
                // 高亮前景色
                currentStyle.foregroundColor = p - 90 + 8;
            } else if (p >= 100 && p <= 107) {
                // 高亮背景色
                currentStyle.backgroundColor = p - 100 + 8;
            }
        }
    }

    /**
     * 处理扩展颜色（256色或真彩色）
     */
    private int handleExtendedColor(int[] params, int index, boolean foreground) {
        if (index + 1 >= params.length) return index;

        int mode = params[index + 1];
        if (mode == 5 && index + 2 < params.length) {
            // 256色调色板
            int color = params[index + 2];
            if (foreground) {
                currentStyle.foregroundColor = color + 16;
            } else {
                currentStyle.backgroundColor = color + 16;
            }
            return index + 2;
        } else if (mode == 2 && index + 4 < params.length) {
            // 真彩色 RGB
            int r = params[index + 2];
            int g = params[index + 3];
            int b = params[index + 4];
            int color = (r << 16) | (g << 8) | b | 0x1000000;
            if (foreground) {
                currentStyle.foregroundColor = color;
            } else {
                currentStyle.backgroundColor = color;
            }
            return index + 4;
        }
        return index;
    }

    // ========== OSC 序列处理 ==========

    private void handleOscSequence(int command, String data) {
        switch (command) {
            case 0: // 设置图标和标题
            case 1: // 设置图标
            case 2: // 设置标题
                this.title = data;
                notifyTitleChanged(data);
                break;
            default:
                // 忽略其他 OSC 命令
                break;
        }
    }

    // ========== 控制字符处理 ==========

    private void handleControlChar(char c) {
        switch (c) {
            case '\n': // LF
                buffer.lineFeed();
                break;
            case '\r': // CR
                buffer.carriageReturn();
                break;
            case '\b': // BS
                buffer.backspace();
                break;
            case '\t': // HT
                buffer.tab();
                break;
            case '\007': // BEL
                notifyBell();
                break;
            default:
                // 其他控制字符
                break;
        }
    }

    // ========== 辅助方法 ==========

    private void eraseInDisplay(int mode) {
        switch (mode) {
            case 0: // 从光标到屏幕末尾
                eraseFromCursorToEnd();
                break;
            case 1: // 从屏幕开头到光标
                eraseFromStartToCursor();
                break;
            case 2: // 整个屏幕
                buffer.clearScreen();
                break;
        }
    }

    private void eraseFromCursorToEnd() {
        int row = buffer.getCursorRow();
        int col = buffer.getCursorCol();
        // 清除当前行从光标到末尾（直接设置字符，不移动光标）
        for (int i = col; i < buffer.getColumns(); i++) {
            buffer.setChar(row, i, ' ', currentStyle);
        }
        // 清除后续行
        for (int r = row + 1; r < buffer.getRows(); r++) {
            buffer.clearLine(r);
        }
    }

    private void eraseFromStartToCursor() {
        int row = buffer.getCursorRow();
        int col = buffer.getCursorCol();
        // 清除前面的行
        for (int r = 0; r < row; r++) {
            buffer.clearLine(r);
        }
        // 清除当前行从开头到光标
        buffer.setCursor(row, 0);
        for (int i = 0; i <= col; i++) {
            buffer.setChar(row, i, ' ', null);
        }
        buffer.setCursor(row, col);
    }

    private void eraseInLine(int mode) {
        switch (mode) {
            case 0: // 从光标到行尾
                buffer.clearToEndOfLine();
                break;
            case 1: // 从行首到光标
                buffer.clearFromStartOfLine();
                break;
            case 2: // 整行
                buffer.clearLine(buffer.getCursorRow());
                break;
        }
    }

    private void insertLines(int n) {
        buffer.insertLines(n);
    }

    private void deleteLines(int n) {
        buffer.deleteLines(n);
    }

    private void deleteCharacters(int n) {
        buffer.deleteCharacters(n);
    }

    private void insertCharacters(int n) {
        buffer.insertCharacters(n);
    }

    private void scrollUp(int n) {
        for (int i = 0; i < n; i++) {
            buffer.lineFeed();
            buffer.cursorUp(1);
        }
    }

    private void scrollDown(int n) {
        buffer.scrollDown(n);
    }

    private void eraseCharacters(int n) {
        buffer.eraseCharacters(n);
    }

    private void saveCursorPosition() {
        buffer.saveCursorPosition();
    }

    private void restoreCursorPosition() {
        buffer.restoreCursorPosition();
    }

    private void setMode(int[] params, boolean enable) {
        for (int p : params) {
            switch (p) {
                case 4: // IRM - Insert/Replace Mode
                    insertMode = enable;
                    break;
                case 6: // DECOM - Origin Mode
                    originMode = enable;
                    break;
                case 7: // DECAWM - Auto Wrap Mode
                    autoWrapMode = enable;
                    break;
                case 20: // LNM - Newline Mode
                    newlineMode = enable;
                    break;
            }
        }
    }

    // ========== 通知方法 ==========

    private void notifyTitleChanged(String title) {
        if (listener != null) {
            listener.onTitleChanged(title);
        }
    }

    private void notifyBufferUpdated() {
        if (listener != null) {
            listener.onBufferUpdated();
        }
    }

    private void notifyBell() {
        if (listener != null) {
            listener.onBell();
        }
    }

    // ========== Getters/Setters ==========

    public TerminalBuffer getBuffer() {
        return buffer;
    }

    public String getTitle() {
        return title;
    }

    public void setListener(TerminalEmulatorListener listener) {
        this.listener = listener;
    }

    public void resize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        buffer.resize(columns, rows);
    }
}
