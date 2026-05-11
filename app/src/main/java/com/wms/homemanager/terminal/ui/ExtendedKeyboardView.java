package com.wms.homemanager.terminal.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.wms.homemanager.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展键盘视图
 * 提供特殊按键输入支持
 */
public class ExtendedKeyboardView extends View {

    // 按键定义
    public static final int KEY_CTRL = 1;
    public static final int KEY_ALT = 2;
    public static final int KEY_FN = 3;
    public static final int KEY_ESC = 4;
    public static final int KEY_TAB = 5;
    public static final int KEY_DEL = 6;
    public static final int KEY_UP = 7;
    public static final int KEY_DOWN = 8;
    public static final int KEY_LEFT = 9;
    public static final int KEY_RIGHT = 10;
    public static final int KEY_HOME = 11;
    public static final int KEY_END = 12;
    public static final int KEY_PGUP = 13;
    public static final int KEY_PGDN = 14;
    public static final int KEY_INSERT = 15;
    public static final int KEY_F1 = 16;
    public static final int KEY_F2 = 17;
    public static final int KEY_F3 = 18;
    public static final int KEY_F4 = 19;
    public static final int KEY_F5 = 20;
    public static final int KEY_F6 = 21;
    public static final int KEY_F7 = 22;
    public static final int KEY_F8 = 23;
    public static final int KEY_F9 = 24;
    public static final int KEY_F10 = 25;
    public static final int KEY_F11 = 26;
    public static final int KEY_F12 = 27;
    public static final int KEY_ENTER = 28;  // 添加 Enter 键
    public static final int KEY_CTRLC = 29;  // Ctrl+C 中断信号

    // 按键类
    private static class Key {
        int id;
        String label;
        String longLabel;
        RectF rect;
        boolean isModifier;
        boolean isPressed;
        boolean isLocked;
        boolean visible = true;
        long lastPressTime;

        Key(int id, String label, String longLabel, boolean isModifier) {
            this.id = id;
            this.label = label;
            this.longLabel = longLabel != null ? longLabel : label;
            this.isModifier = isModifier;
            this.rect = new RectF();
        }
    }

    // 按键列表
    private List<Key> keys = new ArrayList<>();

    // 绘制相关
    private Paint keyPaint;
    private Paint textPaint;
    private Paint pressedPaint;
    private Paint lockedPaint;
    private Paint ctrlCPaint; // Ctrl+C 按钮专用

    // 尺寸
    private float keyHeight;
    private float keyWidth;
    private float keySpacing;
    private float keyPadding;

    // 布局模式
    public static final int LAYOUT_COMPACT = 0;  // 简洁布局
    public static final int LAYOUT_FULL = 1;     // 完整布局
    private int layoutMode = LAYOUT_COMPACT;

    // 修饰键状态
    private boolean ctrlPressed = false;
    private boolean altPressed = false;
    private boolean fnPressed = false;
    private boolean ctrlLocked = false;
    private boolean altLocked = false;
    private boolean fnLocked = false;

    // 监听器
    private KeyListener keyListener;

    /**
     * 按键监听器接口
     */
    public interface KeyListener {
        void onKeyPressed(int keyCode, boolean ctrl, boolean alt, boolean fn);
        void onKeyReleased(int keyCode);
        void onModifierStateChanged(boolean ctrl, boolean alt, boolean fn);
        /**
         * Ctrl+C 按钮按下
         */
        void onCtrlC();
    }

    public ExtendedKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public ExtendedKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ExtendedKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化
     */
    private void init(Context context) {
        // 初始化画笔
        keyPaint = new Paint();
        keyPaint.setAntiAlias(true);
        keyPaint.setColor(0xFF252B45); // 使用现有主题色
        keyPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(0xFFE8EAED);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);

        pressedPaint = new Paint();
        pressedPaint.setAntiAlias(true);
        pressedPaint.setColor(0xFF00D4FF);
        pressedPaint.setStyle(Paint.Style.FILL);

        lockedPaint = new Paint();
        lockedPaint.setAntiAlias(true);
        lockedPaint.setColor(0xFF7C4DFF);
        lockedPaint.setStyle(Paint.Style.FILL);

        // Ctrl+C 按钮专用红色画笔
        ctrlCPaint = new Paint();
        ctrlCPaint.setAntiAlias(true);
        ctrlCPaint.setColor(0xFFFF4081); // 红色
        ctrlCPaint.setStyle(Paint.Style.FILL);

        // 初始化尺寸
        keyHeight = 48 * getResources().getDisplayMetrics().density;
        keySpacing = 4 * getResources().getDisplayMetrics().density;
        keyPadding = 8 * getResources().getDisplayMetrics().density;

        // 初始化按键
        initKeys();
    }

    /**
     * 初始化按键
     */
    private void initKeys() {
        keys.clear();

        // 修饰键行
        keys.add(new Key(KEY_CTRL, "CTRL", "Control", true));
        keys.add(new Key(KEY_ALT, "ALT", "Alt", true));
        keys.add(new Key(KEY_FN, "FN", "Function", true));

        // 控制键行
        keys.add(new Key(KEY_ESC, "ESC", "Escape", false));
        keys.add(new Key(KEY_TAB, "TAB", "Tab", false));
        keys.add(new Key(KEY_ENTER, "ENTER", "Enter", false));
        keys.add(new Key(KEY_DEL, "DEL", "Delete", false));

        // 方向键行
        keys.add(new Key(KEY_UP, "↑", "Up", false));
        keys.add(new Key(KEY_DOWN, "↓", "Down", false));
        keys.add(new Key(KEY_LEFT, "←", "Left", false));
        keys.add(new Key(KEY_RIGHT, "→", "Right", false));

        // 特殊键行
        keys.add(new Key(KEY_HOME, "HOME", "Home", false));
        keys.add(new Key(KEY_END, "END", "End", false));
        keys.add(new Key(KEY_PGUP, "PGUP", "Page Up", false));
        keys.add(new Key(KEY_PGDN, "PGDN", "Page Down", false));
        keys.add(new Key(KEY_INSERT, "INS", "Insert", false));

        // 功能键（完整模式）
        keys.add(new Key(KEY_F1, "F1", "F1", false));
        keys.add(new Key(KEY_F2, "F2", "F2", false));
        keys.add(new Key(KEY_F3, "F3", "F3", false));
        keys.add(new Key(KEY_F4, "F4", "F4", false));
        keys.add(new Key(KEY_F5, "F5", "F5", false));
        keys.add(new Key(KEY_F6, "F6", "F6", false));
        keys.add(new Key(KEY_F7, "F7", "F7", false));
        keys.add(new Key(KEY_F8, "F8", "F8", false));
        keys.add(new Key(KEY_F9, "F9", "F9", false));
        keys.add(new Key(KEY_F10, "F10", "F10", false));
        keys.add(new Key(KEY_F11, "F11", "F11", false));
        keys.add(new Key(KEY_F12, "F12", "F12", false));

        // Ctrl+C 中断按钮（单独一行，红色高亮）
        keys.add(new Key(KEY_CTRLC, "^C", "Ctrl+C Interrupt", false));

        updateKeyVisibility();
    }

    /**
     * 更新按键可见性
     */
    private void updateKeyVisibility() {
        for (Key key : keys) {
            // 功能键只在完整模式下可见
            if (key.id >= KEY_F1 && key.id <= KEY_F12) {
                key.visible = (layoutMode == LAYOUT_FULL);
            } else {
                key.visible = true;
            }
        }
        requestLayout();
    }

    /**
     * 设置布局模式
     */
    public void setLayoutMode(int mode) {
        this.layoutMode = mode;
        updateKeyVisibility();
    }

    /**
     * 获取布局模式
     */
    public int getLayoutMode() {
        return layoutMode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int visibleKeys = 0;

        // 计算可见按键数量
        for (Key key : keys) {
            if (key.visible) visibleKeys++;
        }

        // 计算布局
        int keysPerRow;
        if (layoutMode == LAYOUT_COMPACT) {
            keysPerRow = 7;
        } else {
            keysPerRow = 10;
        }

        int rows = (visibleKeys + keysPerRow - 1) / keysPerRow;
        int height = (int) (rows * (keyHeight + keySpacing) + keySpacing);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int width = right - left;
        int keysPerRow = layoutMode == LAYOUT_COMPACT ? 7 : 10;

        // 计算按键宽度
        keyWidth = (width - keySpacing * (keysPerRow + 1)) / keysPerRow;

        int row = 0;
        int col = 0;
        float y = keySpacing;

        for (Key key : keys) {
            if (!key.visible) continue;

            float x = keySpacing + col * (keyWidth + keySpacing);
            key.rect.set(x, y, x + keyWidth, y + keyHeight);

            col++;
            if (col >= keysPerRow) {
                col = 0;
                row++;
                y += keyHeight + keySpacing;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Key key : keys) {
            if (!key.visible) continue;

            Paint paint;
            if (key.id == KEY_CTRLC) {
                // Ctrl+C 按钮使用红色背景
                paint = key.isPressed ? pressedPaint : ctrlCPaint;
            } else if (key.isLocked) {
                paint = lockedPaint;
            } else if (key.isPressed) {
                paint = pressedPaint;
            } else {
                paint = keyPaint;
            }

            // 绘制按键背景
            float radius = 8 * getResources().getDisplayMetrics().density;
            canvas.drawRoundRect(key.rect, radius, radius, paint);

            // 绘制按键文字
            float textX = key.rect.centerX();
            float textY = key.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2;
            canvas.drawText(key.label, textX, textY, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                handleKeyDown(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleKeyUp(x, y);
                performClick();
                break;
            case MotionEvent.ACTION_CANCEL:
                cancelAllKeys();
                break;
        }

        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * 处理按键按下
     */
    private void handleKeyDown(float x, float y) {
        for (Key key : keys) {
            if (!key.visible) continue;

            if (key.rect.contains(x, y)) {
                key.isPressed = true;

                if (key.isModifier) {
                    // 修饰键特殊处理
                } else if (key.id == KEY_CTRLC) {
                    // Ctrl+C 特殊处理
                    if (keyListener != null) {
                        keyListener.onCtrlC();
                    }
                } else {
                    // 普通按键立即触发
                    if (keyListener != null) {
                        keyListener.onKeyPressed(key.id, ctrlPressed || ctrlLocked,
                                altPressed || altLocked, fnPressed || fnLocked);
                    }

                    // 如果修饰键未锁定，释放修饰键
                    if (!ctrlLocked) ctrlPressed = false;
                    if (!altLocked) altPressed = false;
                    if (!fnLocked) fnPressed = false;
                }

                invalidate();
                break;
            }
        }
    }

    /**
     * 处理按键释放
     */
    private void handleKeyUp(float x, float y) {
        for (Key key : keys) {
            if (!key.visible) continue;

            if (key.rect.contains(x, y) && key.isPressed) {
                key.isPressed = false;

                if (key.isModifier) {
                    handleModifierKey(key);
                }

                if (keyListener != null) {
                    keyListener.onKeyReleased(key.id);
                }

                invalidate();
                break;
            }
        }
    }

    /**
     * 处理修饰键
     */
    private void handleModifierKey(Key key) {
        long pressTime = System.currentTimeMillis();

        // 双击检测（锁定模式）
        if (pressTime - key.lastPressTime < 300) {
            // 双击切换锁定状态
            key.isLocked = !key.isLocked;

            switch (key.id) {
                case KEY_CTRL:
                    ctrlLocked = key.isLocked;
                    if (key.isLocked) {
                        Toast.makeText(getContext(), "CTRL 已锁定", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case KEY_ALT:
                    altLocked = key.isLocked;
                    break;
                case KEY_FN:
                    fnLocked = key.isLocked;
                    break;
            }
        } else {
            // 单击切换按下状态
            switch (key.id) {
                case KEY_CTRL:
                    if (!ctrlLocked) {
                        ctrlPressed = !ctrlPressed;
                    }
                    break;
                case KEY_ALT:
                    if (!altLocked) {
                        altPressed = !altPressed;
                    }
                    break;
                case KEY_FN:
                    if (!fnLocked) {
                        fnPressed = !fnPressed;
                    }
                    break;
            }
        }

        key.lastPressTime = pressTime;

        if (keyListener != null) {
            keyListener.onModifierStateChanged(ctrlPressed || ctrlLocked,
                    altPressed || altLocked, fnPressed || fnLocked);
        }
    }

    /**
     * 取消所有按键状态
     */
    private void cancelAllKeys() {
        for (Key key : keys) {
            key.isPressed = false;
        }
        invalidate();
    }

    // ========== 公共方法 ==========

    /**
     * 设置按键监听器
     */
    public void setKeyListener(KeyListener listener) {
        this.keyListener = listener;
    }

    /**
     * 获取 CTRL 状态
     */
    public boolean isCtrlActive() {
        return ctrlPressed || ctrlLocked;
    }

    /**
     * 获取 ALT 状态
     */
    public boolean isAltActive() {
        return altPressed || altLocked;
    }

    /**
     * 获取 FN 状态
     */
    public boolean isFnActive() {
        return fnPressed || fnLocked;
    }

    /**
     * 使用一次 CTRL 组合键后，释放非锁定的 CTRL 状态
     */
    public void consumeCtrl() {
        if (!ctrlLocked) {
            ctrlPressed = false;
            invalidate();
        }
    }

    /**
     * 重置修饰键状态
     */
    public void resetModifiers() {
        ctrlPressed = false;
        altPressed = false;
        fnPressed = false;
        ctrlLocked = false;
        altLocked = false;
        fnLocked = false;

        for (Key key : keys) {
            key.isPressed = false;
            key.isLocked = false;
        }

        invalidate();
    }
}
