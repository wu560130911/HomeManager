package com.wms.homemanager.ui.fragment;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.wms.homemanager.R;
import com.wms.homemanager.databinding.FragmentShellBinding;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.utils.LoginInfoUtils;

import java.util.ArrayList;
import java.util.List;

public class ShellFragment extends Fragment {

    private FragmentShellBinding binding;
    private List<String> commandHistory;
    private List<String> commonCommands;
    private int historyIndex;
    private boolean isCommandRunning;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShellBinding.inflate(inflater, container, false);

        commandHistory = new ArrayList<>();
        commonCommands = new ArrayList<>();
        historyIndex = -1;
        isCommandRunning = false;

        // 初始化常见命令
        initCommonCommands();
        // 初始化终端显示
        initTerminal();

        binding.btnExecute.setOnClickListener(v -> executeCommand());
        binding.btnStop.setOnClickListener(v -> stopCommand());
        binding.btnHistory.setOnClickListener(v -> showHistoryDialog());
        binding.btnCommon.setOnClickListener(v -> showCommonCommandsDialog());
        binding.btnStop.setEnabled(false);

        // 监听键盘事件，支持上下键切换历史命令
        binding.etCommand.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_UP:
                        navigateHistory(true);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        navigateHistory(false);
                        return true;
                    case KeyEvent.KEYCODE_ENTER:
                        executeCommand();
                        return true;
                }
            }
            return false;
        });

        // 监听键盘弹出，自动滚动到输入框
        setupKeyboardListener();

        return binding.getRoot();
    }

    private void setupKeyboardListener() {
        final View rootView = binding.getRoot();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                // 键盘高度超过屏幕15%时认为键盘弹出
                if (keypadHeight > screenHeight * 0.15) {
                    // 键盘弹出，滚动到输入框
                    binding.llInput.post(() -> {
                        binding.llInput.requestFocus();
                        binding.etCommand.requestFocus();
                    });
                }
            }
        });
    }

    private void initTerminal() {
        binding.tvTerminal.setText("Welcome to HomeManager Terminal\n\n");
    }

    private void initCommonCommands() {
        commonCommands.add("ls");
        commonCommands.add("pwd");
        commonCommands.add("clear");
        commonCommands.add("top");
        commonCommands.add("netstat");
        commonCommands.add("ifconfig");
        commonCommands.add("whoami");
        commonCommands.add("uname -a");
        commonCommands.add("df -h");
        commonCommands.add("free -m");
    }

    private void executeCommand() {
        String command = binding.etCommand.getText().toString().trim();
        if (command.isEmpty()) {
            Toast.makeText(requireContext(), R.string.please_enter_command, Toast.LENGTH_SHORT).show();
            return;
        }
        if ("clear".equals(command)) {
            initTerminal();
            binding.etCommand.setText("");
            binding.etCommand.requestFocus();
            return;
        }

        // 添加到历史记录
        commandHistory.add(command);
        historyIndex = commandHistory.size();

        // 显示命令
        appendToTerminal("$ " + command + "\n");

        // 设置命令执行状态
        isCommandRunning = true;
        binding.btnExecute.setEnabled(false);
        binding.btnStop.setEnabled(true);

        // 从 SharedPreferences 中获取登录信息对象
        LoginInfo loginInfo = LoginInfoUtils.getCurrentLoginInfo(requireContext());

        if (loginInfo != null) {
            // 使用 SshService 执行命令
            SshService.getInstance(requireContext()).executeCommand(
                    loginInfo,
                    command,
                    new SshService.ExecuteCallback() {
                        @Override
                        public void onSuccess(String result) {
                            requireActivity().runOnUiThread(() -> {
                                // 命令执行完成，恢复按钮状态
                                isCommandRunning = false;
                                binding.btnExecute.setEnabled(true);
                                binding.btnStop.setEnabled(false);
                                binding.etCommand.setText("");
                                // 使用 postDelayed 确保在滚动完成后再设置焦点
                                binding.etCommand.postDelayed(() -> {
                                    binding.etCommand.requestFocus();
                                }, 100);
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            requireActivity().runOnUiThread(() -> {
                                if (error.endsWith("\n")) {
                                    appendToTerminal(error);
                                } else {
                                    appendToTerminal(error + "\n");
                                }
                                // 命令执行完成，恢复按钮状态
                                isCommandRunning = false;
                                binding.btnExecute.setEnabled(true);
                                binding.btnStop.setEnabled(false);
                                binding.etCommand.setText("");
                                // 使用 postDelayed 确保在滚动完成后再设置焦点
                                binding.etCommand.postDelayed(() -> {
                                    binding.etCommand.requestFocus();
                                }, 100);
                            });
                        }

                        @Override
                        public void onOutput(String output) {
                            requireActivity().runOnUiThread(() -> {
                                if (output.endsWith("\n")) {
                                    appendToTerminal(output);
                                } else {
                                    appendToTerminal(output + "\n");
                                }
                            });
                        }
                    }
            );
        } else {
            Toast.makeText(requireContext(), R.string.please_login_first, Toast.LENGTH_SHORT).show();
            appendToTerminal("错误: 请先登录\n");
            binding.etCommand.requestFocus();
            // 恢复按钮状态
            isCommandRunning = false;
            binding.btnExecute.setEnabled(true);
            binding.btnStop.setEnabled(false);
        }
    }

    private void appendToTerminal(String text) {
        binding.tvTerminal.append(text);
        // 滚动到底部
        binding.svTerminal.post(() -> binding.svTerminal.fullScroll(View.FOCUS_DOWN));
    }

    private void navigateHistory(boolean up) {
        if (commandHistory.isEmpty()) {
            return;
        }

        if (up) {
            // 向上导航，获取上一条命令
            if (historyIndex > 0) {
                historyIndex--;
                binding.etCommand.setText(commandHistory.get(historyIndex));
                binding.etCommand.setSelection(binding.etCommand.getText().length());
            }
        } else {
            // 向下导航，获取下一条命令
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                binding.etCommand.setText(commandHistory.get(historyIndex));
                binding.etCommand.setSelection(binding.etCommand.getText().length());
            } else {
                // 到达历史记录末尾，清空输入框
                historyIndex = commandHistory.size();
                binding.etCommand.setText("");
            }
        }
    }

    private void stopCommand() {
        SshService.getInstance(requireContext()).stopCommand();
        appendToTerminal("\n" + getString(R.string.command_stopped) + "\n");
        isCommandRunning = false;
        binding.btnExecute.setEnabled(true);
        binding.btnStop.setEnabled(false);
        binding.etCommand.requestFocus();
    }

    private void showHistoryDialog() {
        if (commandHistory.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_history, Toast.LENGTH_SHORT).show();
            return;
        }

        showCommandPopup(binding.btnHistory, commandHistory);
    }

    private void showCommonCommandsDialog() {
        showCommandPopup(binding.btnCommon, commonCommands);
    }

    private void showCommandPopup(View anchorView, List<String> commands) {
        // 创建自定义弹出菜单
        PopupWindow popupWindow = new PopupWindow(requireContext());
        popupWindow.setWidth(400);
        popupWindow.setHeight(500);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(null);

        // 创建布局
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor("#000000"));
        layout.setPadding(5, 5, 5, 5);
        layout.setElevation(8);

        // 创建ListView
        ListView listView = new ListView(requireContext());
        listView.setBackgroundColor(Color.parseColor("#000000"));
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(true);

        // 创建自定义适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                android.R.layout.simple_list_item_1, commands) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.parseColor("#00ff00"));
                textView.setBackgroundColor(Color.parseColor("#000000"));
                textView.setPadding(15, 10, 15, 10);
                textView.setTextSize(12);
                textView.setTypeface(android.graphics.Typeface.MONOSPACE);
                return view;
            }
        };
        listView.setAdapter(adapter);

        // 设置点击事件
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String command = commands.get(position);
            binding.etCommand.setText(command);
            binding.etCommand.setSelection(command.length());
            popupWindow.dismiss();
        });

        // 添加ListView到布局
        layout.addView(listView);
        popupWindow.setContentView(layout);

        // 计算精确位置
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        int x = location[0];
        int y = location[1] - 510;

        // 确保Y坐标为正
        y = Math.max(y, 0);

        // 显示弹出菜单
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
