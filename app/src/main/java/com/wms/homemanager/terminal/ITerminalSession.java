package com.wms.homemanager.terminal;

/**
 * 终端会话接口
 * 定义终端会话的基本操作
 */
public interface ITerminalSession {

    /**
     * 获取会话名称
     */
    String getName();

    /**
     * 设置会话名称
     */
    void setName(String name);

    /**
     * 获取会话状态
     */
    SessionState getState();

    /**
     * 发送数据到远程
     * @param data 要发送的数据
     */
    void write(byte[] data);

    /**
     * 发送字符串到远程
     * @param text 要发送的字符串
     */
    void write(String text);

    /**
     * 发送特殊按键序列
     * @param keyCode 按键码
     * @param modifiers 修饰键 (CTRL=1, ALT=2, SHIFT=4)
     */
    void sendKey(int keyCode, int modifiers);

    /**
     * 关闭会话
     */
    void close();

    /**
     * 添加会话监听器
     */
    void addListener(SessionListener listener);

    /**
     * 移除会话监听器
     */
    void removeListener(SessionListener listener);

    /**
     * 会话状态枚举
     */
    enum SessionState {
        CONNECTING,   // 连接中
        CONNECTED,    // 已连接
        DISCONNECTED, // 已断开
        ERROR         // 错误
    }

    /**
     * 会话监听器接口
     */
    interface SessionListener {
        /**
         * 收到输出数据
         */
        void onOutput(byte[] data, int offset, int length);

        /**
         * 会话状态变化
         */
        void onStateChanged(SessionState newState);

        /**
         * 会话标题变化
         */
        void onTitleChanged(String title);
    }
}
