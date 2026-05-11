package com.wms.homemanager.terminal.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.wms.homemanager.model.LoginInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端会话管理器
 * 管理多个终端会话
 * 每个会话使用独立的 SSH Session
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREFS_NAME = "terminal_sessions";
    private static final String KEY_COMMAND_HISTORY = "command_history";
    private static final int MAX_SESSIONS = 5;
    private static final int MAX_HISTORY_SIZE = 100;

    // 会话列表
    private List<TerminalSession> sessions = new ArrayList<>();
    private int currentSessionIndex = -1;

    // 命令历史
    private List<String> commandHistory = new ArrayList<>();

    // 登录信息（用于创建独立 SSH Session）
    private LoginInfo loginInfo;

    // 上下文
    private Context context;

    // 监听器
    private SessionManagerListener listener;

    /**
     * 会话管理器监听器
     */
    public interface SessionManagerListener {
        void onSessionCreated(TerminalSession session, int index);
        void onSessionClosed(int index);
        void onSessionSwitched(int fromIndex, int toIndex);
        void onSessionRenamed(int index, String newName);
    }

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
        loadCommandHistory();
    }

    /**
     * 设置登录信息
     * 用于创建独立的 SSH Session
     */
    public void setLoginInfo(LoginInfo loginInfo) {
        this.loginInfo = loginInfo;
        Log.d(TAG, "Login info set for terminal sessions");
    }

    /**
     * 检查是否可以创建会话
     */
    public boolean canCreateSession() {
        return loginInfo != null && sessions.size() < MAX_SESSIONS;
    }

    /**
     * 创建新会话
     * 每个会话使用独立的 SSH Session
     */
    public TerminalSession createSession(String name) {
        if (sessions.size() >= MAX_SESSIONS) {
            Log.w(TAG, "Max sessions reached: " + MAX_SESSIONS);
            return null;
        }

        if (loginInfo == null) {
            Log.e(TAG, "Login info not set, cannot create session");
            return null;
        }

        if (name == null || name.isEmpty()) {
            name = "Terminal #" + (sessions.size() + 1);
        }

        // 创建新会话（每个会话有独立的 SSH Session）
        TerminalSession session = new TerminalSession(context, loginInfo, name);
        sessions.add(session);

        int index = sessions.size() - 1;
        Log.d(TAG, "Session created: " + name + ", total sessions: " + sessions.size());

        if (listener != null) {
            listener.onSessionCreated(session, index);
        }

        // 自动切换到新会话
        switchToSession(index);

        return session;
    }

    /**
     * 获取当前会话
     */
    public TerminalSession getCurrentSession() {
        if (currentSessionIndex >= 0 && currentSessionIndex < sessions.size()) {
            return sessions.get(currentSessionIndex);
        }
        return null;
    }

    /**
     * 切换到指定会话
     */
    public void switchToSession(int index) {
        if (index < 0 || index >= sessions.size()) {
            return;
        }

        int oldIndex = currentSessionIndex;
        currentSessionIndex = index;

        if (listener != null && oldIndex != index) {
            listener.onSessionSwitched(oldIndex, index);
        }
    }

    /**
     * 切换到下一个会话
     */
    public void switchToNextSession() {
        if (sessions.isEmpty()) return;
        int nextIndex = (currentSessionIndex + 1) % sessions.size();
        switchToSession(nextIndex);
    }

    /**
     * 切换到上一个会话
     */
    public void switchToPrevSession() {
        if (sessions.isEmpty()) return;
        int prevIndex = (currentSessionIndex - 1 + sessions.size()) % sessions.size();
        switchToSession(prevIndex);
    }

    /**
     * 关闭指定会话
     */
    public void closeSession(int index) {
        if (index < 0 || index >= sessions.size()) {
            return;
        }

        TerminalSession session = sessions.get(index);
        session.close();
        sessions.remove(index);

        if (listener != null) {
            listener.onSessionClosed(index);
        }

        // 调整当前会话索引
        if (sessions.isEmpty()) {
            currentSessionIndex = -1;
        } else if (currentSessionIndex >= sessions.size()) {
            currentSessionIndex = sessions.size() - 1;
        } else if (currentSessionIndex > index) {
            currentSessionIndex--;
        }
    }

    /**
     * 重命名会话
     */
    public void renameSession(int index, String newName) {
        if (index < 0 || index >= sessions.size()) {
            return;
        }

        TerminalSession session = sessions.get(index);
        session.setName(newName);

        if (listener != null) {
            listener.onSessionRenamed(index, newName);
        }
    }

    /**
     * 获取会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取当前会话索引
     */
    public int getCurrentSessionIndex() {
        return currentSessionIndex;
    }

    /**
     * 获取所有会话名称
     */
    public List<String> getSessionNames() {
        List<String> names = new ArrayList<>();
        for (TerminalSession session : sessions) {
            names.add(session.getName());
        }
        return names;
    }

    /**
     * 获取会话
     */
    public TerminalSession getSession(int index) {
        if (index >= 0 && index < sessions.size()) {
            return sessions.get(index);
        }
        return null;
    }

    // ========== 命令历史管理 ==========

    /**
     * 添加命令到历史
     */
    public void addCommandToHistory(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        // 移除重复
        commandHistory.remove(command);

        // 添加到末尾
        commandHistory.add(command);

        // 限制大小
        while (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.remove(0);
        }

        // 保存
        saveCommandHistory();
    }

    /**
     * 获取命令历史
     */
    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }

    /**
     * 清空命令历史
     */
    public void clearCommandHistory() {
        commandHistory.clear();
        saveCommandHistory();
    }

    /**
     * 加载命令历史
     */
    private void loadCommandHistory() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String historyJson = prefs.getString(KEY_COMMAND_HISTORY, "[]");

        try {
            JSONArray array = new JSONArray(historyJson);
            commandHistory.clear();
            for (int i = 0; i < array.length(); i++) {
                commandHistory.add(array.getString(i));
            }
        } catch (JSONException e) {
            // 忽略解析错误
        }
    }

    /**
     * 保存命令历史
     */
    private void saveCommandHistory() {
        try {
            JSONArray array = new JSONArray(commandHistory);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_COMMAND_HISTORY, array.toString()).apply();
        } catch (Exception e) {
            // 忽略保存错误
        }
    }

    // ========== 监听器 ==========

    public void setListener(SessionManagerListener listener) {
        this.listener = listener;
    }

    // ========== 清理 ==========

    /**
     * 关闭所有会话
     */
    public void closeAllSessions() {
        Log.d(TAG, "Closing all sessions, count: " + sessions.size());
        for (TerminalSession session : sessions) {
            session.close();
        }
        sessions.clear();
        currentSessionIndex = -1;
    }
}
