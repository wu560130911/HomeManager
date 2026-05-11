package com.wms.homemanager.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.wms.homemanager.R;
import com.wms.homemanager.databinding.FragmentShellNewBinding;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.terminal.session.SessionManager;
import com.wms.homemanager.terminal.session.TerminalSession;
import com.wms.homemanager.terminal.theme.TerminalTheme;
import com.wms.homemanager.terminal.theme.ThemeManager;
import com.wms.homemanager.terminal.ui.ExtendedKeyboardView;
import com.wms.homemanager.terminal.ui.TerminalView;
import com.wms.homemanager.utils.LoginInfoUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 新版终端 Fragment
 * 使用自研终端模拟器
 */
public class ShellFragmentNew extends Fragment {

    private FragmentShellNewBinding binding;

    // 终端组件
    private TerminalView terminalView;
    private ExtendedKeyboardView extendedKeyboard;

    // 会话管理
    private SessionManager sessionManager;
    private TerminalSession currentSession;

    // 主题管理
    private ThemeManager themeManager;

    // SSH 服务
    private SshService sshService;
    private LoginInfo loginInfo;

    // 状态
    private boolean isInitialized = false;
    
    // 防止 TextWatcher 递归
    private boolean isProcessingTextChange = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShellNewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化服务
        sshService = SshService.getInstance(requireContext());
        loginInfo = LoginInfoUtils.getCurrentLoginInfo(requireContext());

        // 初始化主题管理
        themeManager = new ThemeManager(requireContext());
        themeManager.setListener(new ThemeManager.ThemeChangeListener() {
            @Override
            public void onThemeChanged(TerminalTheme theme) {
                applyTheme(theme);
            }

            @Override
            public void onFontSizeChanged(float fontSize) {
                if (terminalView != null) {
                    terminalView.setFontSize(fontSize);
                }
            }
        });

        // 初始化会话管理
        sessionManager = new SessionManager(requireContext());
        sessionManager.setListener(new SessionManager.SessionManagerListener() {
            @Override
            public void onSessionCreated(TerminalSession session, int index) {
                requireActivity().runOnUiThread(() -> updateSessionTabs());
            }

            @Override
            public void onSessionClosed(int index) {
                requireActivity().runOnUiThread(() -> updateSessionTabs());
            }

            @Override
            public void onSessionSwitched(int fromIndex, int toIndex) {
                requireActivity().runOnUiThread(() -> {
                    if (binding.tabLayout != null) {
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(toIndex));
                    }
                    // 切换会话时，切换终端模拟器
                    switchTerminalEmulator(toIndex);
                });
            }

            @Override
            public void onSessionRenamed(int index, String newName) {
                requireActivity().runOnUiThread(() -> updateSessionTabs());
            }
        });

        // 设置终端视图
        setupTerminalView();

        // 设置扩展键盘
        setupExtendedKeyboard();

        // 设置输入框
        setupInputField();

        // 设置功能按钮
        setupActionButtons();

        // 连接 SSH 会话
        connectToSsh();
    }

    /**
     * 设置终端视图
     */
    private void setupTerminalView() {
        // 动态创建 TerminalView
        terminalView = new TerminalView(requireContext());
        binding.terminalContainer.addView(terminalView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        // 应用主题
        applyTheme(themeManager.getCurrentTheme());

        // 设置选择监听
        terminalView.setSelectionListener(new TerminalView.SelectionListener() {
            @Override
            public void onSelectionStarted() {
                // 显示复制按钮
            }

            @Override
            public void onSelectionChanged(int startRow, int startCol, int endRow, int endCol) {
                // 选择变化
            }

            @Override
            public void onSelectionCopied(String text) {
                // 复制完成
            }
        });

        // 设置尺寸变化监听
        terminalView.setResizeListener((cols, rows) -> {
            resizeCurrentSession(cols, rows);
        });
    }

    /**
     * 设置扩展键盘
     */
    private void setupExtendedKeyboard() {
        // 动态创建 ExtendedKeyboardView
        extendedKeyboard = new ExtendedKeyboardView(requireContext());
        binding.extendedKeyboardContainer.addView(extendedKeyboard,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        extendedKeyboard.setKeyListener(new ExtendedKeyboardView.KeyListener() {
            @Override
            public void onKeyPressed(int keyCode, boolean ctrl, boolean alt, boolean fn) {
                handleKeyPressed(keyCode, ctrl, alt, fn);
            }

            @Override
            public void onKeyReleased(int keyCode) {
                // 按键释放
            }

            @Override
            public void onModifierStateChanged(boolean ctrl, boolean alt, boolean fn) {
                // 修饰键状态变化，可以更新 UI 提示
            }

            @Override
            public void onCtrlC() {
                // 发送 Ctrl+C 中断信号
                if (currentSession != null) {
                    currentSession.sendCtrlC();
                }
            }
        });
    }

    /**
     * 设置输入框
     */
    private void setupInputField() {
        binding.etCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendCommand();
                return true;
            }
            return false;
        });

        // 支持物理键盘
        binding.etCommand.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    navigateHistory(true);
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    navigateHistory(false);
                    return true;
                }
            }
            return false;
        });

        // 监听软键盘输入，支持 CTRL 组合键
        binding.etCommand.addTextChangedListener(new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isProcessingTextChange) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不在这里处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isProcessingTextChange) return;

                // 检查是否有新增字符
                if (s.length() > beforeText.length()) {
                    // 获取新增的字符
                    String newText = s.toString().substring(beforeText.length());
                    
                    // 如果 CTRL 激活，发送组合键而不是输入字符
                    if (extendedKeyboard != null && extendedKeyboard.isCtrlActive() && newText.length() == 1) {
                        char c = newText.charAt(0);
                        // 只处理字母键
                        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                            // 发送 CTRL+字符
                            if (currentSession != null) {
                                byte ctrlChar = (byte) (Character.toLowerCase(c) - 'a' + 1);
                                currentSession.write(new byte[]{ctrlChar});
                            }
                            // 清除输入框（恢复到之前的状态）
                            isProcessingTextChange = true;
                            s.clear();
                            s.append(beforeText);
                            isProcessingTextChange = false;
                            // 如果 CTRL 未锁定，释放 CTRL；如果锁定，保持激活
                            extendedKeyboard.consumeCtrl();
                            return;
                        }
                    }
                }
            }
        });
    }

    /**
     * 设置功能按钮
     */
    private void setupActionButtons() {
        // 新建会话
        binding.btnNewSession.setOnClickListener(v -> createNewSession());

        // 设置
        binding.btnSettings.setOnClickListener(v -> showSettings());

        // 切换键盘布局
        binding.btnToggleKeyboard.setOnClickListener(v -> toggleKeyboardLayout());
    }

    /**
     * 连接 SSH
     */
    private void connectToSsh() {
        if (loginInfo == null) {
            Toast.makeText(requireContext(), R.string.please_login_first, Toast.LENGTH_SHORT).show();
            return;
        }

        // 终端使用独立的 SSH Session，不需要等待业务 Session 连接
        // 设置 LoginInfo 给 SessionManager，终端会话会自动创建独立的 SSH 连接
        sessionManager.setLoginInfo(loginInfo);

        // 创建第一个终端会话
        createNewSession();
        isInitialized = true;
    }

    /**
     * 创建新会话
     */
    private void createNewSession() {
        TerminalSession session = sessionManager.createSession(null);
        if (session == null) {
            Toast.makeText(requireContext(), "已达到最大会话数量", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置会话监听（仅用于触发 UI 刷新）
        session.addListener(new TerminalSession.SessionListener() {
            @Override
            public void onOutput(byte[] data, int offset, int length) {
                // 数据已写入会话的 emulator，这里只需触发 UI 刷新
                if (getActivity() != null && !getActivity().isFinishing()) {
                    // 只有当前会话才需要刷新 UI
                    if (currentSession == session && terminalView != null) {
                        getActivity().runOnUiThread(() -> {
                            terminalView.invalidate();
                        });
                    }
                }
            }

            @Override
            public void onStateChanged(TerminalSession.SessionState newState) {
                android.util.Log.d("ShellFragmentNew", "onStateChanged: " + newState);
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        updateSessionState(session);
                    });
                }
            }

            @Override
            public void onTitleChanged(String title) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        // 更新标题
                    });
                }
            }
        });

        // 设置新会话的 emulator 到 terminalView
        if (terminalView != null) {
            terminalView.setEmulator(session.getEmulator());
        }

        // 连接会话 - 使用最小尺寸确保连接成功，后续会在 onSizeChanged 中调整
        int cols = terminalView.getColumns();
        int rows = terminalView.getRows();
        // 确保最小尺寸
        if (cols <= 0) cols = 80;
        if (rows <= 0) rows = 24;
        android.util.Log.d("ShellFragmentNew", "Connecting session with cols=" + cols + ", rows=" + rows);
        session.connect(cols, rows);
        currentSession = session;
    }

    /**
     * 调整当前会话尺寸
     */
    private void resizeCurrentSession(int cols, int rows) {
        if (currentSession != null && cols > 0 && rows > 0) {
            currentSession.resize(cols, rows);
            android.util.Log.d("ShellFragmentNew", "Resized session to cols=" + cols + ", rows=" + rows);
        }
    }

    /**
     * 发送命令
     */
    private void sendCommand() {
        String command = binding.etCommand.getText().toString();
        if (command.isEmpty() || currentSession == null) {
            return;
        }

        // 添加到历史
        sessionManager.addCommandToHistory(command);

        // 发送命令
        currentSession.write(command + "\n");

        // 清空输入框
        binding.etCommand.setText("");
    }

    /**
     * 处理按键
     */
    private void handleKeyPressed(int keyCode, boolean ctrl, boolean alt, boolean fn) {
        if (currentSession == null) return;

        // 发送按键序列
        int modifiers = (ctrl ? 1 : 0) | (alt ? 2 : 0) | (fn ? 4 : 0);
        currentSession.sendKey(keyCode, modifiers);
    }

    /**
     * 导航历史命令
     */
    private void navigateHistory(boolean up) {
        List<String> history = sessionManager.getCommandHistory();
        if (history.isEmpty()) return;

        // 简单实现：显示历史列表
        // TODO: 实现更完善的导航
    }

    /**
     * 更新会话标签
     */
    private void updateSessionTabs() {
        if (binding.tabLayout == null) return;

        binding.tabLayout.removeAllTabs();
        List<String> names = sessionManager.getSessionNames();

        for (int i = 0; i < names.size(); i++) {
            TabLayout.Tab tab = binding.tabLayout.newTab();
            // 创建自定义标签视图
            tab.setCustomView(createTabView(names.get(i), i));
            binding.tabLayout.addTab(tab);
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 切换会话，会触发 onSessionSwitched 回调
                sessionManager.switchToSession(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * 创建标签视图（带关闭按钮）
     */
    private View createTabView(String name, int index) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // 标签名称
        TextView textView = new TextView(requireContext());
        textView.setText(name);
        textView.setTextColor(getResources().getColor(R.color.colorOnBackground, null));
        textView.setTextSize(12);
        textView.setPadding(0, 0, 8, 0);
        layout.addView(textView);

        // 关闭按钮（只在有多个会话时显示）
        if (sessionManager.getSessionCount() > 1) {
            ImageView closeBtn = new ImageView(requireContext());
            closeBtn.setImageResource(R.drawable.ic_close);
            closeBtn.setColorFilter(getResources().getColor(R.color.colorOnSurfaceVariant, null));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            params.setMarginStart(4);
            closeBtn.setLayoutParams(params);
            closeBtn.setOnClickListener(v -> {
                // 关闭会话
                int sessionIndex = index;
                sessionManager.closeSession(sessionIndex);
                // 如果关闭的是当前会话，切换到第一个
                if (sessionManager.getCurrentSession() != null) {
                    currentSession = sessionManager.getCurrentSession();
                }
            });
            layout.addView(closeBtn);
        }

        return layout;
    }

    /**
     * 更新会话状态
     */
    private void updateSessionState(TerminalSession session) {
        // 可以更新标签图标表示状态
    }

    /**
     * 切换终端模拟器（用于切换会话）
     */
    private void switchTerminalEmulator(int sessionIndex) {
        TerminalSession session = sessionManager.getSession(sessionIndex);
        if (session == null || terminalView == null) return;

        // 切换到新会话的终端模拟器
        terminalView.setEmulator(session.getEmulator());
        currentSession = session;

        // 应用当前主题
        if (themeManager != null && themeManager.getCurrentTheme() != null) {
            applyTheme(themeManager.getCurrentTheme());
        }

        android.util.Log.d("ShellFragmentNew", "Switched to session: " + session.getName());
    }

    /**
     * 应用主题
     */
    private void applyTheme(TerminalTheme theme) {
        if (terminalView == null || theme == null) return;

        terminalView.setDefaultForegroundColor(theme.getForegroundColor());
        terminalView.setDefaultBackgroundColor(theme.getBackgroundColor());
        terminalView.setCursorColor(theme.getCursorColor());
        terminalView.setSelectionColor(theme.getSelectionColor());
        terminalView.setFontSize(themeManager.getFontSize());
    }

    /**
     * 显示设置
     */
    private void showSettings() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

        // 创建对话框视图
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_terminal_settings, null);
        builder.setView(dialogView);

        // 字体大小设置
        android.widget.SeekBar fontSizeSeekBar = dialogView.findViewById(R.id.seekbar_font_size);
        TextView fontSizeValue = dialogView.findViewById(R.id.tv_font_size_value);
        fontSizeSeekBar.setProgress((int) (themeManager.getFontSize() - 6)); // 8-32 映射到 0-24
        fontSizeValue.setText(String.valueOf((int) themeManager.getFontSize()));
        fontSizeSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                float size = progress + 6;
                fontSizeValue.setText(String.valueOf((int) size));
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                float size = seekBar.getProgress() + 6;
                themeManager.setFontSize(size);
            }
        });

        // 光标闪烁设置
        androidx.appcompat.widget.SwitchCompat cursorBlinkSwitch = dialogView.findViewById(R.id.switch_cursor_blink);
        cursorBlinkSwitch.setChecked(true); // 默认开启
        cursorBlinkSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (terminalView != null) {
                terminalView.setCursorBlink(isChecked);
            }
        });

        // 主题选择
        android.widget.LinearLayout themeContainer = dialogView.findViewById(R.id.theme_container);
        List<TerminalTheme> themes = themeManager.getPresetThemes();
        for (TerminalTheme theme : themes) {
            View themeItem = createThemeItem(theme, themeContainer);
            themeContainer.addView(themeItem);
        }

        // 设置布局文件中的按钮事件
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save);

        // 创建对话框
        android.app.AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            // 应用当前选中的主题
            if (terminalView != null && themeManager.getCurrentTheme() != null) {
                applyTheme(themeManager.getCurrentTheme());
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 创建主题选择项
     */
    private View createThemeItem(TerminalTheme theme, android.widget.LinearLayout themeContainer) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layout.setPadding(0, 12, 0, 12);

        // 颜色预览
        View colorPreview = new View(requireContext());
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(60, 30);
        previewParams.setMarginEnd(12);
        colorPreview.setLayoutParams(previewParams);
        // 创建渐变背景
        android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{theme.getBackgroundColor(), theme.getForegroundColor()}
        );
        gradient.setCornerRadius(4);
        colorPreview.setBackground(gradient);
        layout.addView(colorPreview);

        // 主题名称
        TextView themeName = new TextView(requireContext());
        themeName.setText(theme.getName());
        themeName.setTextColor(getResources().getColor(R.color.colorOnBackground, null));
        themeName.setTextSize(14);
        layout.addView(themeName);

        // 当前选中状态
        if (themeManager.getCurrentTheme().getName().equals(theme.getName())) {
            themeName.setTextColor(getResources().getColor(R.color.neonGreen, null));
            themeName.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // 点击选择
        layout.setOnClickListener(v -> {
            themeManager.setCurrentTheme(theme);
            // 刷新主题容器中的选中状态，而不是创建新对话框
            refreshThemeSelection(themeContainer);
        });

        return layout;
    }

    /**
     * 刷新主题选择状态
     */
    private void refreshThemeSelection(android.widget.LinearLayout themeContainer) {
        for (int i = 0; i < themeContainer.getChildCount(); i++) {
            View child = themeContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) child;
                TextView themeName = (TextView) layout.getChildAt(1); // 第二个元素是主题名称
                if (themeName != null) {
                    String themeNameText = themeName.getText().toString();
                    if (themeManager.getCurrentTheme().getName().equals(themeNameText)) {
                        themeName.setTextColor(getResources().getColor(R.color.neonGreen, null));
                        themeName.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        themeName.setTextColor(getResources().getColor(R.color.colorOnBackground, null));
                        themeName.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
            }
        }
    }

    /**
     * 切换键盘布局
     */
    private void toggleKeyboardLayout() {
        if (extendedKeyboard != null) {
            int currentMode = extendedKeyboard.getLayoutMode() == ExtendedKeyboardView.LAYOUT_COMPACT ?
                    ExtendedKeyboardView.LAYOUT_FULL : ExtendedKeyboardView.LAYOUT_COMPACT;
            extendedKeyboard.setLayoutMode(currentMode);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 重连或恢复
    }

    @Override
    public void onPause() {
        super.onPause();
        // 保持连接
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 关闭所有会话
        if (sessionManager != null) {
            sessionManager.closeAllSessions();
        }
        binding = null;
    }
}
