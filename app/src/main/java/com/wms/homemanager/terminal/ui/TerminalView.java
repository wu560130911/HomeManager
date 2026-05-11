package com.wms.homemanager.terminal.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.wms.homemanager.R;
import com.wms.homemanager.terminal.TerminalEmulator;
import com.wms.homemanager.terminal.buffer.TerminalBuffer;

/**
 * 终端显示视图
 * 使用 Canvas 直接绘制终端内容
 */
public class TerminalView extends View implements TerminalEmulator.TerminalEmulatorListener {

    // 终端模拟器
    private TerminalEmulator emulator;

    // 绘制相关
    private Paint textPaint;
    private Paint backgroundPaint;
    private Paint cursorPaint;
    private Paint selectionPaint;

    // 字体设置
    private Typeface typeface;
    private float fontSize = 14f; // sp
    private float charWidth;
    private float charHeight;
    private float lineHeight;

    // 颜色主题
    private int defaultForegroundColor = 0xFF00FF88; // 霓虹绿
    private int defaultBackgroundColor = 0xFF0D1117; // 深色背景
    private int cursorColor = 0xFF00FF88;
    private int selectionColor = 0x8000D4FF; // 半透明选择色

    // 光标闪烁
    private boolean cursorBlink = true;
    private boolean cursorVisible = true;
    private long lastBlinkTime;
    private static final long BLINK_INTERVAL = 500; // ms

    // 缓冲区大小
    private int columns = 80;
    private int rows = 24;

    // ========== 滚动相关 ==========
    private int scrollOffset = 0; // 滚动偏移量
    private float lastTouchY = 0;
    private float totalScrollDistance = 0;
    private static final float SCROLL_THRESHOLD = 30f; // 滚动阈值

    // ========== 文本选择相关 ==========
    private enum SelectionMode {
        NONE,           // 无选择
        SELECTING,      // 正在选择
        SELECTED        // 已选择
    }

    private SelectionMode selectionMode = SelectionMode.NONE;
    private int selectionStartRow = -1;
    private int selectionStartCol = -1;
    private int selectionEndRow = -1;
    private int selectionEndCol = -1;

    // 手势检测
    private GestureDetector gestureDetector;
    private boolean isLongPress = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    // 选择监听器
    private SelectionListener selectionListener;

    // 尺寸变化监听器
    private ResizeListener resizeListener;

    /**
     * 尺寸变化监听器接口
     */
    public interface ResizeListener {
        void onResize(int columns, int rows);
    }

    /**
     * 选择监听器接口
     */
    public interface SelectionListener {
        void onSelectionStarted();
        void onSelectionChanged(int startRow, int startCol, int endRow, int endCol);
        void onSelectionCopied(String text);
    }

    public TerminalView(Context context) {
        super(context);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TerminalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化
     */
    private void init(Context context) {
        // 创建终端模拟器
        emulator = new TerminalEmulator(columns, rows, 10000);
        emulator.setListener(this);

        // 初始化画笔 - 使用系统等宽字体
        // 使用 create("monospace") 而不是 Typeface.MONOSPACE 常量
        // 因为它会选择系统最佳的等宽字体（通常是 Noto Sans Mono）
        // Noto Sans Mono 支持所有 Unicode 字符，包括 Box Drawing 和 Powerline 符号
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        textPaint.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setColor(defaultForegroundColor);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(defaultBackgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);

        cursorPaint = new Paint();
        cursorPaint.setColor(cursorColor);
        cursorPaint.setStyle(Paint.Style.FILL);

        selectionPaint = new Paint();
        selectionPaint.setColor(selectionColor);
        selectionPaint.setStyle(Paint.Style.FILL);

        // 计算字符尺寸
        measureCharSize();

        // 设置背景色
        setBackgroundColor(defaultBackgroundColor);

        lastBlinkTime = System.currentTimeMillis();

        // 初始化手势检测
        initGestureDetector(context);
    }

    /**
     * 初始化手势检测
     */
    private void initGestureDetector(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (selectionMode == SelectionMode.SELECTED) {
                    // 点击清除选择
                    clearSelection();
                }
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // 长按开始选择
                isLongPress = true;
                float x = e.getX();
                float y = e.getY();
                int col = (int) (x / charWidth);
                int row = (int) (y / lineHeight);

                startSelection(row, col);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // 双击选择整行
                float y = e.getY();
                int row = (int) (y / lineHeight);
                selectLine(row);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        if (selectionMode == SelectionMode.SELECTING) {
            handleSelectionTouch(event);
            return true;
        }

        // 处理滚动
        handleScrollTouch(event);

        // 处理点击事件以支持无障碍访问
        if (event.getAction() == MotionEvent.ACTION_UP && selectionMode != SelectionMode.SELECTING) {
            performClick();
        }

        return true;
    }

    /**
     * 处理滚动触摸事件
     */
    private void handleScrollTouch(MotionEvent event) {
        TerminalBuffer buffer = emulator.getBuffer();
        if (buffer == null) return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                totalScrollDistance = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - lastTouchY;
                totalScrollDistance += Math.abs(deltaY);

                // 只有在明显滑动时才处理滚动
                if (totalScrollDistance > SCROLL_THRESHOLD || scrollOffset > 0) {
                    // 将滑动距离转换为行数
                    // deltaY > 0 表示手指向下滑动，用户期望看到更旧的内容（scrollOffset 增加）
                    float scrollLines = deltaY / lineHeight;

                    // 直接将滑动行数加到 scrollOffset 上
                    int newOffset = scrollOffset + (int) Math.round(scrollLines);

                    // 限制滚动范围：0 到 滚动缓冲区行数
                    int maxOffset = buffer.getScrollBufferCount();
                    newOffset = Math.max(0, Math.min(newOffset, maxOffset));

                    if (newOffset != scrollOffset) {
                        scrollOffset = newOffset;
                        invalidate();
                    }

                    lastTouchY = event.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                totalScrollDistance = 0;
                break;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * 处理选择触摸事件
     */
    private void handleSelectionTouch(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int col = (int) (x / charWidth);
        int row = (int) (y / lineHeight);

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (selectionMode == SelectionMode.SELECTING) {
                    extendSelection(row, col);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (selectionMode == SelectionMode.SELECTING) {
                    selectionMode = SelectionMode.SELECTED;
                    showCopyPrompt();
                }
                break;
        }
    }

    /**
     * 开始选择
     */
    private void startSelection(int row, int col) {
        selectionStartRow = row;
        selectionStartCol = col;
        selectionEndRow = row;
        selectionEndCol = col;
        selectionMode = SelectionMode.SELECTING;

        if (selectionListener != null) {
            selectionListener.onSelectionStarted();
        }

        invalidate();
    }

    /**
     * 扩展选择
     */
    private void extendSelection(int row, int col) {
        selectionEndRow = row;
        selectionEndCol = col;

        if (selectionListener != null) {
            selectionListener.onSelectionChanged(
                    selectionStartRow, selectionStartCol,
                    selectionEndRow, selectionEndCol);
        }

        invalidate();
    }

    /**
     * 选择整行
     */
    private void selectLine(int row) {
        selectionStartRow = row;
        selectionStartCol = 0;
        selectionEndRow = row;
        selectionEndCol = columns;
        selectionMode = SelectionMode.SELECTED;

        if (selectionListener != null) {
            selectionListener.onSelectionStarted();
        }

        invalidate();
        showCopyPrompt();
    }

    /**
     * 清除选择
     */
    public void clearSelection() {
        selectionStartRow = -1;
        selectionStartCol = -1;
        selectionEndRow = -1;
        selectionEndCol = -1;
        selectionMode = SelectionMode.NONE;
        invalidate();
    }

    /**
     * 显示复制提示
     */
    private void showCopyPrompt() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            Toast.makeText(getContext(), R.string.tap_to_copy, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制选中文本到剪贴板
     */
    public void copySelectedText() {
        String text = getSelectedText();
        if (text != null && !text.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("terminal text", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                if (selectionListener != null) {
                    selectionListener.onSelectionCopied(text);
                }
            }
        }
        clearSelection();
    }

    /**
     * 获取选中的文本
     */
    public String getSelectedText() {
        if (selectionStartRow < 0 || selectionEndRow < 0) {
            return null;
        }

        TerminalBuffer buffer = emulator.getBuffer();
        if (buffer == null) return null;

        StringBuilder sb = new StringBuilder();

        // 确保起始位置在结束位置之前
        int startRow = Math.min(selectionStartRow, selectionEndRow);
        int endRow = Math.max(selectionStartRow, selectionEndRow);
        int startCol = (startRow == endRow) ?
                Math.min(selectionStartCol, selectionEndCol) :
                (startRow == selectionStartRow ? selectionStartCol : selectionEndCol);
        int endCol = (startRow == endRow) ?
                Math.max(selectionStartCol, selectionEndCol) :
                (endRow == selectionEndRow ? selectionEndCol : selectionStartCol);

        if (startRow == endRow) {
            // 单行选择
            char[] rowChars = buffer.getRow(startRow);
            int colStart = Math.min(startCol, rowChars.length);
            int colEnd = Math.min(endCol, rowChars.length);
            sb.append(rowChars, colStart, colEnd - colStart);
        } else {
            // 多行选择
            // 第一行
            char[] firstRow = buffer.getRow(startRow);
            int firstColStart = Math.min(startCol, firstRow.length);
            sb.append(firstRow, firstColStart, firstRow.length - firstColStart);
            sb.append('\n');

            // 中间行
            for (int row = startRow + 1; row < endRow; row++) {
                char[] rowChars = buffer.getRow(row);
                sb.append(rowChars);
                sb.append('\n');
            }

            // 最后一行
            char[] lastRow = buffer.getRow(endRow);
            int lastColEnd = Math.min(endCol, lastRow.length);
            sb.append(lastRow, 0, lastColEnd);
        }

        return sb.toString().trim();
    }

    /**
     * 是否有选择文本
     */
    public boolean hasSelection() {
        return selectionMode != SelectionMode.NONE && selectionStartRow >= 0;
    }

    /**
     * 测量字符尺寸
     */
    private void measureCharSize() {
        charWidth = textPaint.measureText("M");
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        charHeight = fm.descent - fm.ascent;
        lineHeight = charHeight * 1.2f;
    }

    /**
     * 检测字符是否是双宽字符（CJK 字符）
     * 包括：中文、日文、韩文、全角字符等
     */
    private boolean isWideChar(char c) {
        // CJK 统一汉字
        if (c >= 0x4E00 && c <= 0x9FFF) return true;
        // CJK 统一汉字扩展 A
        if (c >= 0x3400 && c <= 0x4DBF) return true;
        // CJK 统一汉字扩展 B-F
        if (c >= 0x20000 && c <= 0x2CEAF) return true;
        // CJK 兼容汉字
        if (c >= 0xF900 && c <= 0xFAFF) return true;
        // 日文平假名
        if (c >= 0x3040 && c <= 0x309F) return true;
        // 日文片假名
        if (c >= 0x30A0 && c <= 0x30FF) return true;
        // 韩文字母
        if (c >= 0xAC00 && c <= 0xD7AF) return true;
        // 全角 ASCII、全角标点
        if (c >= 0xFF00 && c <= 0xFFEF) return true;
        return false;
    }

    /**
     * 获取字符的显示宽度（以字符单位计）
     * ASCII 字符返回 1，CJK 双宽字符返回 2
     */
    private int getCharWidth(char c) {
        if (isWideChar(c)) {
            return 2;
        }
        return 1;
    }

    /**
     * 设置字体大小
     */
    public void setFontSize(float size) {
        this.fontSize = size;
        textPaint.setTextSize(fontSize * getResources().getDisplayMetrics().scaledDensity);
        measureCharSize();
        recalculateSize();
        invalidate();
    }

    /**
     * 设置字体
     */
    public void setFontTypeface(Typeface typeface) {
        this.typeface = typeface;
        textPaint.setTypeface(typeface != null ? typeface : Typeface.MONOSPACE);
        measureCharSize();
        recalculateSize();
        invalidate();
    }

    /**
     * 重新计算终端尺寸
     */
    private void recalculateSize() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth > 0 && viewHeight > 0 && charWidth > 0 && lineHeight > 0) {
            int newCols = Math.max(1, (int) (viewWidth / charWidth));
            int newRows = Math.max(1, (int) (viewHeight / lineHeight));

            if (newCols != columns || newRows != rows) {
                columns = newCols;
                rows = newRows;
                emulator.resize(columns, rows);

                // 通知尺寸变化
                if (resizeListener != null) {
                    resizeListener.onResize(columns, rows);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalculateSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        TerminalBuffer buffer = emulator.getBuffer();
        if (buffer == null) return;

        // 绘制背景
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // 绘制文本
        if (scrollOffset > 0) {
            // 滚动模式：显示滚动缓冲区内容
            int scrollBufferCount = buffer.getScrollBufferCount();

            for (int displayRow = 0; displayRow < rows; displayRow++) {
                // 计算要显示哪一行
                // scrollOffset 表示向上滚动了多少行（看到更旧的内容）
                // scrollOffset=1 时，显示滚动缓冲区最新的那行在屏幕顶部
                int scrollLineIndex = scrollBufferCount - scrollOffset + displayRow;

                if (scrollLineIndex < 0) {
                    // 这部分在滚动缓冲区之前，显示空白
                    continue;
                } else if (scrollLineIndex < scrollBufferCount) {
                    // 显示滚动缓冲区内容
                    drawRowFromScrollBuffer(canvas, buffer, scrollLineIndex, displayRow);
                } else {
                    // 显示屏幕缓冲区内容
                    int screenRow = scrollLineIndex - scrollBufferCount;
                    if (screenRow < buffer.getRows()) {
                        drawRowAtPosition(canvas, buffer, screenRow, displayRow);
                    }
                }
            }
        } else {
            // 正常模式：显示屏幕缓冲区内容
            for (int row = 0; row < buffer.getRows(); row++) {
                drawRow(canvas, buffer, row);
            }

            // 绘制光标
            if (cursorBlink && buffer.isCursorVisible()) {
                drawCursor(canvas, buffer);
            }
        }

        // 绘制选择区域
        if (selectionMode != SelectionMode.NONE) {
            drawSelection(canvas);
        }

        // 绘制滚动指示器
        if (buffer.getScrollBufferCount() > 0 || scrollOffset > 0) {
            drawScrollIndicator(canvas, buffer);
        }
    }

    /**
     * 从滚动缓冲区绘制一行
     */
    private void drawRowFromScrollBuffer(Canvas canvas, TerminalBuffer buffer, int scrollRow, int displayRow) {
        char[] chars = buffer.getScrollLine(scrollRow);
        TerminalBuffer.TextStyle[] styles = buffer.getScrollStyleLine(scrollRow);
        if (chars == null) return;

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = displayRow * lineHeight - fm.ascent;

        for (int col = 0; col < chars.length && col < columns; col++) {
            char c = chars[col];
            if (c == ' ' || c == '\0') continue;

            boolean isWide = isWideChar(c);
            float charDisplayWidth = isWide ? charWidth * 2 : charWidth;
            float x = col * charWidth;

            int fgColor = (styles != null && styles[col].foregroundColor >= 0) ?
                    mapColor(styles[col].foregroundColor) : defaultForegroundColor;

            textPaint.setColor(fgColor);
            canvas.drawText(chars, col, 1, x, baseline, textPaint);

            if (isWide && col + 1 < chars.length) {
                col++;
            }
        }

        // 重置画笔状态
        textPaint.setFakeBoldText(false);
        textPaint.setTextSkewX(0);
        textPaint.setUnderlineText(false);
        backgroundPaint.setColor(defaultBackgroundColor);
    }

    /**
     * 从缓冲区绘制一行到指定显示位置
     */
    private void drawRowFromBuffer(Canvas canvas, TerminalBuffer buffer, int bufferRow, int displayRow, boolean applyStyle) {
        char[] chars = buffer.getRow(bufferRow);
        TerminalBuffer.TextStyle[] styles = buffer.getRowStyles(bufferRow);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = displayRow * lineHeight - fm.ascent;

        for (int col = 0; col < chars.length && col < buffer.getColumns(); col++) {
            char c = chars[col];
            if (c == ' ' || c == '\0') continue;

            boolean isWide = isWideChar(c);
            float charDisplayWidth = isWide ? charWidth * 2 : charWidth;
            float x = col * charWidth;

            int fgColor = defaultForegroundColor;
            if (applyStyle && styles != null) {
                TerminalBuffer.TextStyle style = styles[col];
                fgColor = style.foregroundColor >= 0 ? mapColor(style.foregroundColor) : defaultForegroundColor;
            }

            textPaint.setColor(fgColor);
            canvas.drawText(chars, col, 1, x, baseline, textPaint);

            if (isWide && col + 1 < chars.length) {
                col++;
            }
        }

        // 重置画笔状态
        textPaint.setFakeBoldText(false);
        textPaint.setTextSkewX(0);
        textPaint.setUnderlineText(false);
        backgroundPaint.setColor(defaultBackgroundColor);
    }

    /**
     * 绘制滚动指示器
     */
    private void drawScrollIndicator(Canvas canvas, TerminalBuffer buffer) {
        int scrollBufferCount = buffer.getScrollBufferCount();
        if (scrollBufferCount == 0) return;

        // 指示器高度为屏幕高度的 1/4
        int indicatorHeight = getHeight() / 4;
        int indicatorWidth = 4;

        // 计算指示器位置
        int maxScroll = scrollBufferCount + rows;
        float scrollRatio = (float) scrollOffset / maxScroll;
        float indicatorY = scrollRatio * (getHeight() - indicatorHeight);

        // 绘制指示器背景轨道
        Paint trackPaint = new Paint();
        trackPaint.setColor(0x40808080);
        trackPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(getWidth() - indicatorWidth - 2, 0, getWidth() - 2, getHeight(), trackPaint);

        // 绘制指示器
        Paint indicatorPaint = new Paint();
        indicatorPaint.setColor(0x8000D4FF);
        indicatorPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(getWidth() - indicatorWidth - 2, indicatorY, getWidth() - 2, indicatorY + indicatorHeight, indicatorPaint);
    }

    /**
     * 绘制选择区域
     */
    private void drawSelection(Canvas canvas) {
        if (selectionStartRow < 0 || selectionEndRow < 0) return;

        int startRow = Math.min(selectionStartRow, selectionEndRow);
        int endRow = Math.max(selectionStartRow, selectionEndRow);

        for (int row = startRow; row <= endRow; row++) {
            float left, right;
            if (startRow == endRow) {
                // 单行
                left = Math.min(selectionStartCol, selectionEndCol) * charWidth;
                right = Math.max(selectionStartCol, selectionEndCol) * charWidth;
            } else if (row == startRow) {
                // 第一行
                left = selectionStartRow < selectionEndRow ? selectionStartCol * charWidth : selectionEndCol * charWidth;
                right = columns * charWidth;
            } else if (row == endRow) {
                // 最后一行
                left = 0;
                right = (selectionEndRow > selectionStartRow ? selectionEndCol : selectionStartCol) * charWidth;
            } else {
                // 中间行
                left = 0;
                right = columns * charWidth;
            }

            float top = row * lineHeight;
            float bottom = (row + 1) * lineHeight;
            canvas.drawRect(left, top, right, bottom, selectionPaint);
        }
    }

    /**
     * 绘制一行文本
     */
    private void drawRow(Canvas canvas, TerminalBuffer buffer, int row) {
        char[] chars = buffer.getRow(row);
        TerminalBuffer.TextStyle[] styles = buffer.getRowStyles(row);

        // 计算基线 Y 坐标：行顶部 + |ascent|
        // ascent 是负数，表示 baseline 以上到字符顶部的距离
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = row * lineHeight - fm.ascent;

        for (int col = 0; col < chars.length && col < buffer.getColumns(); col++) {
            char c = chars[col];
            if (c == ' ' || c == '\0') continue;

            TerminalBuffer.TextStyle style = styles[col];

            // 检测是否是双宽字符（CJK）
            boolean isWide = isWideChar(c);
            float charDisplayWidth = isWide ? charWidth * 2 : charWidth;
            float x = col * charWidth;

            // 设置颜色
            int fgColor = style.foregroundColor >= 0 ?
                    mapColor(style.foregroundColor) : defaultForegroundColor;

            // 处理反色
            if (style.inverse) {
                int bgColor = style.backgroundColor >= 0 ?
                        mapColor(style.backgroundColor) : defaultBackgroundColor;
                // 绘制背景 - 双宽字符使用双倍宽度
                backgroundPaint.setColor(fgColor);
                canvas.drawRect(x, row * lineHeight, x + charDisplayWidth, (row + 1) * lineHeight, backgroundPaint);
                textPaint.setColor(bgColor);
            } else {
                // 绘制背景色 - 双宽字符使用双倍宽度
                if (style.backgroundColor >= 0) {
                    backgroundPaint.setColor(mapColor(style.backgroundColor));
                    canvas.drawRect(x, row * lineHeight, x + charDisplayWidth, (row + 1) * lineHeight, backgroundPaint);
                }
                textPaint.setColor(fgColor);
            }

            // 设置样式
            textPaint.setFakeBoldText(style.bold);
            textPaint.setTextSkewX(style.italic ? -0.25f : 0);
            textPaint.setUnderlineText(style.underline);

            // 绘制字符
            canvas.drawText(chars, col, 1, x, baseline, textPaint);

            // 如果是双宽字符，跳过下一个列位置
            if (isWide && col + 1 < chars.length) {
                col++; // 跳过下一列
            }
        }

        // 重置画笔状态
        textPaint.setFakeBoldText(false);
        textPaint.setTextSkewX(0);
        textPaint.setUnderlineText(false);
        // 重置背景画笔颜色为默认值
        backgroundPaint.setColor(defaultBackgroundColor);
    }

    /**
     * 绘制一行文本到指定显示位置（用于滚动模式）
     * @param bufferRow 缓冲区行号
     * @param displayRow 显示位置行号
     */
    private void drawRowAtPosition(Canvas canvas, TerminalBuffer buffer, int bufferRow, int displayRow) {
        char[] chars = buffer.getRow(bufferRow);
        TerminalBuffer.TextStyle[] styles = buffer.getRowStyles(bufferRow);

        // 使用 displayRow 计算基线位置
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = displayRow * lineHeight - fm.ascent;

        // 使用 displayRow 计算 Y 坐标范围
        float rowTop = displayRow * lineHeight;
        float rowBottom = (displayRow + 1) * lineHeight;

        for (int col = 0; col < chars.length && col < buffer.getColumns(); col++) {
            char c = chars[col];
            if (c == ' ' || c == '\0') continue;

            TerminalBuffer.TextStyle style = styles[col];

            boolean isWide = isWideChar(c);
            float charDisplayWidth = isWide ? charWidth * 2 : charWidth;
            float x = col * charWidth;

            int fgColor = style.foregroundColor >= 0 ?
                    mapColor(style.foregroundColor) : defaultForegroundColor;

            if (style.inverse) {
                int bgColor = style.backgroundColor >= 0 ?
                        mapColor(style.backgroundColor) : defaultBackgroundColor;
                backgroundPaint.setColor(fgColor);
                canvas.drawRect(x, rowTop, x + charDisplayWidth, rowBottom, backgroundPaint);
                textPaint.setColor(bgColor);
            } else {
                if (style.backgroundColor >= 0) {
                    backgroundPaint.setColor(mapColor(style.backgroundColor));
                    canvas.drawRect(x, rowTop, x + charDisplayWidth, rowBottom, backgroundPaint);
                }
                textPaint.setColor(fgColor);
            }

            textPaint.setFakeBoldText(style.bold);
            textPaint.setTextSkewX(style.italic ? -0.25f : 0);
            textPaint.setUnderlineText(style.underline);

            canvas.drawText(chars, col, 1, x, baseline, textPaint);

            if (isWide && col + 1 < chars.length) {
                col++;
            }
        }

        // 重置画笔状态
        textPaint.setFakeBoldText(false);
        textPaint.setTextSkewX(0);
        textPaint.setUnderlineText(false);
        backgroundPaint.setColor(defaultBackgroundColor);
    }

    /**
     * 绘制光标
     */
    private void drawCursor(Canvas canvas, TerminalBuffer buffer) {
        // 处理光标闪烁
        long now = System.currentTimeMillis();
        if (cursorBlink && now - lastBlinkTime > BLINK_INTERVAL) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = now;
        }

        if (!cursorVisible) return;

        int cursorRow = buffer.getCursorRow();
        int cursorCol = buffer.getCursorCol();

        float left = cursorCol * charWidth;
        float top = cursorRow * lineHeight;
        float right = left + charWidth;
        float bottom = top + lineHeight;

        TerminalBuffer.CursorStyle style = buffer.getCursorStyle();
        switch (style) {
            case BLOCK:
                // 块状光标：反转背景色
                cursorPaint.setColor(cursorColor);
                cursorPaint.setAlpha(128);
                canvas.drawRect(left, top, right, bottom, cursorPaint);
                break;
            case UNDERLINE:
                // 下划线光标
                cursorPaint.setColor(cursorColor);
                canvas.drawRect(left, bottom - 4, right, bottom, cursorPaint);
                break;
            case BAR:
                // 竖线光标
                cursorPaint.setColor(cursorColor);
                canvas.drawRect(left, top, left + 3, bottom, cursorPaint);
                break;
        }
    }

    /**
     * 映射颜色索引到实际颜色值
     */
    private int mapColor(int colorIndex) {
        // 标准16色
        if (colorIndex < 16) {
            return getStandardColor(colorIndex);
        }
        // 256色调色板
        if (colorIndex < 256 + 16) {
            return get256Color(colorIndex - 16);
        }
        // 真彩色（存储时设置了标志位）
        if ((colorIndex & 0x1000000) != 0) {
            return colorIndex & 0xFFFFFF;
        }
        return defaultForegroundColor;
    }

    /**
     * 获取标准16色
     */
    private int getStandardColor(int index) {
        int[] colors = {
                0xFF000000, // 0: 黑
                0xFFCD0000, // 1: 红
                0xFF00CD00, // 2: 绿
                0xFFCDCD00, // 3: 黄
                0xFF0000EE, // 4: 蓝
                0xFFCD00CD, // 5: 品红
                0xFF00CDCD, // 6: 青
                0xFFE5E5E5, // 7: 白
                0xFF7F7F7F, // 8: 亮黑（灰）
                0xFFFF0000, // 9: 亮红
                0xFF00FF00, // 10: 亮绿
                0xFFFFFF00, // 11: 亮黄
                0xFF5C5CFF, // 12: 亮蓝
                0xFFFF00FF, // 13: 亮品红
                0xFF00FFFF, // 14: 亮青
                0xFFFFFFFF  // 15: 亮白
        };
        return colors[index];
    }

    /**
     * 获取256色调色板颜色
     */
    private int get256Color(int index) {
        if (index < 16) {
            return getStandardColor(index);
        }
        if (index < 232) {
            // 6x6x6 颜色立方
            int n = index - 16;
            int r = (n / 36) % 6;
            int g = (n / 6) % 6;
            int b = n % 6;
            r = r == 0 ? 0 : (55 + r * 40);
            g = g == 0 ? 0 : (55 + g * 40);
            b = b == 0 ? 0 : (55 + b * 40);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        // 灰度色阶
        int gray = (index - 232) * 10 + 8;
        return 0xFF000000 | (gray << 16) | (gray << 8) | gray;
    }

    // ========== TerminalEmulatorListener 实现 ==========

    @Override
    public void onTitleChanged(String title) {
        // 标题变化，可以通知 Activity 更新
    }

    @Override
    public void onBufferUpdated() {
        // 缓冲区更新，如果有新内容则自动滚动到底部
        if (scrollOffset > 0) {
            scrollOffset = 0;
        }
        postInvalidate();
    }

    @Override
    public void onBell() {
        // 响铃
        // TODO: 实现震动或声音提示
    }

    // ========== 公共方法 ==========

    /**
     * 获取终端模拟器
     */
    public TerminalEmulator getEmulator() {
        return emulator;
    }

    /**
     * 设置终端模拟器（用于切换会话）
     */
    public void setEmulator(TerminalEmulator emulator) {
        if (this.emulator != null) {
            this.emulator.setListener(null);
        }
        this.emulator = emulator;
        if (emulator != null) {
            emulator.setListener(this);
            // 更新尺寸（从缓冲区获取）
            TerminalBuffer buffer = emulator.getBuffer();
            if (buffer != null) {
                this.columns = buffer.getColumns();
                this.rows = buffer.getRows();
            }
            // 重置滚动偏移
            this.scrollOffset = 0;
            // 重置选择状态
            clearSelection();
        }
        invalidate();
    }

    /**
     * 处理输入数据
     */
    public void processInput(byte[] data, int offset, int length) {
        android.util.Log.d("TerminalView", "processInput: " + length + " bytes, columns=" + columns + ", rows=" + rows);
        emulator.processInput(data, offset, length);
    }

    /**
     * 设置前景色
     */
    public void setDefaultForegroundColor(int color) {
        this.defaultForegroundColor = color;
        invalidate();
    }

    /**
     * 设置背景色
     */
    public void setDefaultBackgroundColor(int color) {
        this.defaultBackgroundColor = color;
        backgroundPaint.setColor(color);
        setBackgroundColor(color);
        invalidate();
    }

    /**
     * 设置光标颜色
     */
    public void setCursorColor(int color) {
        this.cursorColor = color;
        invalidate();
    }

    /**
     * 设置光标闪烁
     */
    public void setCursorBlink(boolean blink) {
        this.cursorBlink = blink;
        invalidate();
    }

    /**
     * 设置选择颜色
     */
    public void setSelectionColor(int color) {
        this.selectionColor = color;
        selectionPaint.setColor(color);
        invalidate();
    }

    /**
     * 获取列数
     */
    public int getColumns() {
        return columns;
    }

    /**
     * 获取行数
     */
    public int getRows() {
        return rows;
    }

    /**
     * 设置选择监听器
     */
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * 设置尺寸变化监听器
     */
    public void setResizeListener(ResizeListener listener) {
        this.resizeListener = listener;
    }
}
