package com.wms.homemanager.terminal.session;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.service.SshService;
import com.wms.homemanager.terminal.ITerminalSession;
import com.wms.homemanager.terminal.TerminalEmulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 终端会话实现类
 * 封装 SSH Shell 通道，处理输入输出
 * 每个会话使用独立的 SSH Session，与端口转发业务隔离
 */
public class TerminalSession implements ITerminalSession {

    private static final String TAG = "TerminalSession";

    // 会话信息
    private String name;
    private SessionState state = SessionState.DISCONNECTED;

    // 独立的 SSH Session
    private String terminalSessionId; // 终端 Session ID
    private SshService sshService;
    private LoginInfo loginInfo;

    // SSH 相关
    private ChannelShell channel;
    private InputStream inputStream;
    private OutputStream outputStream;

    // 线程
    private ExecutorService executorService;
    private volatile boolean running = false;

    // 监听器
    private List<SessionListener> listeners = new ArrayList<>();

    // 终端尺寸
    private int columns = 80;
    private int rows = 24;

    // 终端模拟器（每个会话独立）
    private TerminalEmulator emulator;

    /**
     * 构造函数
     * @param context 上下文
     * @param loginInfo 登录信息
     * @param name 会话名称
     */
    public TerminalSession(Context context, LoginInfo loginInfo, String name) {
        this.sshService = SshService.getInstance(context);
        this.loginInfo = loginInfo;
        this.name = name;
        this.executorService = Executors.newSingleThreadExecutor();
        // 初始化终端模拟器（滚动缓冲区 10000 行）
        this.emulator = new TerminalEmulator(columns, rows, 10000);
    }

    /**
     * 获取终端 Session ID
     */
    public String getTerminalSessionId() {
        return terminalSessionId;
    }

    /**
     * 连接终端
     */
    public void connect(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;

        Log.d(TAG, "connect() called with columns=" + columns + ", rows=" + rows);
        state = SessionState.CONNECTING;
        notifyStateChanged();

        executorService.execute(() -> {
            try {
                // 创建独立的 SSH Session
                if (terminalSessionId == null) {
                    Log.d(TAG, "Creating new terminal session...");
                    terminalSessionId = sshService.createTerminalSession(loginInfo);
                    Log.d(TAG, "Terminal session created: " + terminalSessionId);
                }

                // 获取 Session
                Session sshSession = sshService.getTerminalSession(terminalSessionId);
                if (sshSession == null) {
                    Log.e(TAG, "Failed to get terminal session: " + terminalSessionId);
                    state = SessionState.ERROR;
                    notifyStateChanged();
                    return;
                }

                if (!sshSession.isConnected()) {
                    Log.e(TAG, "Terminal session is not connected!");
                    state = SessionState.ERROR;
                    notifyStateChanged();
                    return;
                }

                Log.d(TAG, "Opening shell channel...");
                // 创建 Shell 通道
                channel = (ChannelShell) sshSession.openChannel("shell");

                // 设置 PTY
                channel.setPty(true);
                channel.setPtyType("xterm", columns, rows, columns * 8, rows * 16);

                // 获取输入输出流
                inputStream = channel.getInputStream();
                outputStream = channel.getOutputStream();

                // 连接通道
                Log.d(TAG, "Connecting channel...");
                channel.connect(30000);
                Log.d(TAG, "Channel connected: " + channel.isConnected());

                running = true;
                state = SessionState.CONNECTED;
                notifyStateChanged();

                // 启动输出读取线程
                startOutputReader();

                Log.d(TAG, "Terminal session connected: " + name);

            } catch (Exception e) {
                Log.e(TAG, "Failed to connect terminal session: " + e.getMessage(), e);
                state = SessionState.ERROR;
                notifyStateChanged();
            }
        });
    }

    /**
     * 启动输出读取线程
     */
    private void startOutputReader() {
        new Thread(() -> {
            byte[] buffer = new byte[4096];
            Log.d(TAG, "Output reader started for session: " + name);
            try {
                while (running && channel != null && channel.isConnected()) {
                    int available = inputStream.available();
                    if (available > 0) {
                        int len = inputStream.read(buffer, 0, Math.min(available, buffer.length));
                        if (len > 0) {
                            Log.d(TAG, "Read " + len + " bytes from server");
                            notifyOutput(buffer, 0, len);
                        }
                    } else {
                        Thread.sleep(10);
                    }
                }
                Log.d(TAG, "Output reader exited for session: " + name + " running=" + running + " channelConnected=" + (channel != null && channel.isConnected()));
            } catch (Exception e) {
                if (running) {
                    Log.e(TAG, "Error reading output: " + e.getMessage(), e);
                    state = SessionState.ERROR;
                    notifyStateChanged();
                }
            }
        }, "TerminalOutputReader-" + name).start();
    }

    // ========== ITerminalSession 接口实现 ==========

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public SessionState getState() {
        return state;
    }

    @Override
    public void write(byte[] data) {
        // 在后台线程执行网络 I/O，避免 NetworkOnMainThreadException
        executorService.execute(() -> {
            if (outputStream != null && channel != null && channel.isConnected()) {
                try {
                    outputStream.write(data);
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "Error writing data: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void write(String text) {
        write(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void sendKey(int keyCode, int modifiers) {
        String sequence = generateKeySequence(keyCode, modifiers);
        if (sequence != null) {
            write(sequence);
        }
    }

    @Override
    public void close() {
        running = false;

        Log.d(TAG, "Closing terminal session: " + name);

        final String sessionIdToClose = terminalSessionId;
        terminalSessionId = null;

        // 在后台线程关闭所有网络资源
        new Thread(() -> {
            try {
                // 关闭 IO 流
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                // 关闭 Channel
                if (channel != null && channel.isConnected()) {
                    channel.disconnect();
                }
                // 关闭独立的 SSH Session
                if (sessionIdToClose != null) {
                    sshService.closeTerminalSession(sessionIdToClose);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing channel: " + e.getMessage());
            }
        }).start();

        state = SessionState.DISCONNECTED;
        notifyStateChanged();

        if (executorService != null) {
            executorService.shutdown();
        }

        Log.d(TAG, "Terminal session closed: " + name);
    }

    @Override
    public void addListener(SessionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    // ========== 按键序列生成 ==========

    /**
     * 生成按键序列
     */
    private String generateKeySequence(int keyCode, int modifiers) {
        StringBuilder sb = new StringBuilder();

        // 处理修饰键
        boolean ctrl = (modifiers & 1) != 0;
        boolean alt = (modifiers & 2) != 0;
        boolean shift = (modifiers & 4) != 0;

        switch (keyCode) {
            case 1: // KEY_CTRL - 不发送，仅状态
                break;
            case 2: // KEY_ALT - 不发送，仅状态
                break;
            case 3: // KEY_FN - 不发送，仅状态
                break;
            case 4: // KEY_ESC
                sb.append("\u001b");
                break;
            case 5: // KEY_TAB
                sb.append("\t");
                break;
            case 6: // KEY_DEL
                sb.append("\u007f");
                break;
            case 28: // KEY_ENTER
                sb.append("\n");
                break;
            case 7: // KEY_UP
                sb.append("\u001b[A");
                break;
            case 8: // KEY_DOWN
                sb.append("\u001b[B");
                break;
            case 9: // KEY_LEFT
                sb.append("\u001b[D");
                break;
            case 10: // KEY_RIGHT
                sb.append("\u001b[C");
                break;
            case 11: // KEY_HOME
                sb.append("\u001b[H");
                break;
            case 12: // KEY_END
                sb.append("\u001b[F");
                break;
            case 13: // KEY_PGUP
                sb.append("\u001b[5~");
                break;
            case 14: // KEY_PGDN
                sb.append("\u001b[6~");
                break;
            case 15: // KEY_INSERT
                sb.append("\u001b[2~");
                break;
            case 16: // KEY_F1
                sb.append("\u001bOP");
                break;
            case 17: // KEY_F2
                sb.append("\u001bOQ");
                break;
            case 18: // KEY_F3
                sb.append("\u001bOR");
                break;
            case 19: // KEY_F4
                sb.append("\u001bOS");
                break;
            case 20: // KEY_F5
                sb.append("\u001b[15~");
                break;
            case 21: // KEY_F6
                sb.append("\u001b[17~");
                break;
            case 22: // KEY_F7
                sb.append("\u001b[18~");
                break;
            case 23: // KEY_F8
                sb.append("\u001b[19~");
                break;
            case 24: // KEY_F9
                sb.append("\u001b[20~");
                break;
            case 25: // KEY_F10
                sb.append("\u001b[21~");
                break;
            case 26: // KEY_F11
                sb.append("\u001b[23~");
                break;
            case 27: // KEY_F12
                sb.append("\u001b[24~");
                break;
        }

        // 处理 Ctrl 组合键
        if (ctrl && sb.length() == 1) {
            char c = sb.charAt(0);
            if (c >= 'a' && c <= 'z') {
                sb.setCharAt(0, (char) (c - 'a' + 1));
            } else if (c >= 'A' && c <= 'Z') {
                sb.setCharAt(0, (char) (c - 'A' + 1));
            }
        }

        // 处理 Alt 前缀
        if (alt && sb.length() > 0) {
            sb.insert(0, "\u001b");
        }

        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 发送 Ctrl+C
     */
    public void sendCtrlC() {
        write(new byte[]{0x03});
    }

    /**
     * 发送 Ctrl+D
     */
    public void sendCtrlD() {
        write(new byte[]{0x04});
    }

    /**
     * 发送 Ctrl+Z
     */
    public void sendCtrlZ() {
        write(new byte[]{0x1A});
    }

    // ========== 终端尺寸调整 ==========

    /**
     * 调整终端尺寸
     */
    public void resize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;

        // 同步调整终端模拟器尺寸
        if (emulator != null) {
            emulator.resize(columns, rows);
        }

        // 在后台线程执行网络 I/O
        executorService.execute(() -> {
            if (channel != null && channel.isConnected()) {
                try {
                    channel.setPtySize(columns, rows, columns * 8, rows * 16);
                } catch (Exception e) {
                    Log.e(TAG, "Error resizing PTY: " + e.getMessage());
                }
            }
        });
    }

    // ========== 通知方法 ==========

    private void notifyOutput(byte[] data, int offset, int length) {
        // 先写入本会话的终端模拟器
        if (emulator != null) {
            emulator.processInput(data, offset, length);
        }
        // 通知监听器（用于触发 UI 刷新）
        for (SessionListener listener : listeners) {
            listener.onOutput(data, offset, length);
        }
    }

    private void notifyStateChanged() {
        for (SessionListener listener : listeners) {
            listener.onStateChanged(state);
        }
    }

    private void notifyTitleChanged(String title) {
        for (SessionListener listener : listeners) {
            listener.onTitleChanged(title);
        }
    }

    // ========== Getters ==========

    /**
     * 获取终端模拟器
     */
    public TerminalEmulator getEmulator() {
        return emulator;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }
}
