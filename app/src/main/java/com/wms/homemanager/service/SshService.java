package com.wms.homemanager.service;

import android.content.Context;
import android.util.Log;

import com.jcraft.jsch.*;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.model.PortForwardingConfig;
import com.wms.homemanager.utils.PortForwardingUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SshService {
    private static final String TAG = "SshService";
    private static SshService instance;
    private final ExecutorService executorService;
    private final Context context;

    // 业务 Session 池（端口转发、命令执行等）
    private final Map<String, Session> sessionPool;
    // 终端专用 Session 池（独立管理）
    private final Map<String, Session> terminalSessionPool;
    // 终端 Session ID 计数器
    private volatile int terminalSessionCounter = 0;

    private volatile Channel currentChannel;

    private volatile List<PortForwardingConfig> forwardingConfigs;

    private SshService(Context context) {
        this.context = context.getApplicationContext();
        // 创建一个核心线程数为1的线程池，确保即使在后台也能保持活跃
        this.executorService = new java.util.concurrent.ThreadPoolExecutor(
                1, // 核心线程数
                2, // 最大线程数
                60L, // 非核心线程闲置超时时间
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(),
                new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy()
        );
        this.sessionPool = new ConcurrentHashMap<>();
        this.terminalSessionPool = new ConcurrentHashMap<>();
    }

    public static synchronized SshService getInstance(Context context) {
        if (instance == null) {
            instance = new SshService(context);
        }
        return instance;
    }

    // 登录回调接口
    public interface LoginCallback {
        void onSuccess();

        void onFailure(String error);
    }

    // 执行命令回调接口
    public interface ExecuteCallback {
        void onSuccess(String result);

        void onFailure(String error);

        default void onOutput(String output) {
        }
    }

    // 生成会话ID
    private String generateSessionId(String host, int port, String username) {
        return host + ":" + port + ":" + username;
    }

    // 获取或创建会话
    private Session getOrCreateSession(LoginInfo loginInfo) throws JSchException {
        String sessionId = generateSessionId(loginInfo.getHost(), loginInfo.getPort(), loginInfo.getUsername());
        Session session = sessionPool.get(sessionId);

        boolean isNewSession = false;

        if (session == null || !session.isConnected()) {
            // 创建新会话
            JSch jsch = new JSch();
            String actualCertPath = handleCertPath(loginInfo.getCertPath());

            // 添加身份
            if (loginInfo.getCertPass() == null || loginInfo.getCertPass().isEmpty()) {
                jsch.addIdentity(actualCertPath);
            } else {
                jsch.addIdentity(actualCertPath, loginInfo.getCertPass().getBytes());
            }

            // 创建会话
            session = jsch.getSession(loginInfo.getUsername(), loginInfo.getHost(), loginInfo.getPort());
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey");
            // 设置允许证书转发，等常用配置
            session.setConfig("ForwardAgent", "yes");
            session.setConfig("ServerAliveInterval", "60");
            session.setConfig("ServerAliveCountMax", "30");
            //AddKeysToAgent yes
            session.setConfig("AddKeysToAgent", "yes");

            session.connect(30000); // 30秒超时

            // 添加到连接池
            sessionPool.put(sessionId, session);
            isNewSession = true;
        }

        // 如果是新创建的会话，自动应用已保存的端口转发配置
        if (isNewSession) {
            autoApplyPortForwardingConfigs(loginInfo);
        }

        return session;
    }

    // 自动应用已保存的端口转发配置
    private void autoApplyPortForwardingConfigs(LoginInfo loginInfo) {
        try {
            // 从持久化存储中加载已保存的端口转发配置
            List<PortForwardingConfig> configs = PortForwardingUtils.getPortForwardingConfigs(context, loginInfo);

            if (!configs.isEmpty()) {
                applyPortForwardingConfigs(loginInfo, configs, new ExecuteCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d(TAG, "自动端口转发生效成功");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "自动端口转发生效失败: " + error);
                    }
                });

            }
        } catch (Exception e) {
            Log.e(TAG, "自动应用端口转发配置失败: " + e.getMessage(), e);
        }
    }

    // 登录方法
    public void login(LoginInfo loginInfo, LoginCallback callback) {
        executorService.execute(() -> {
            try {
                // 获取或创建会话
                Session session = getOrCreateSession(loginInfo);

                if (session.isConnected()) {
                    callback.onSuccess();
                } else {
                    callback.onFailure("连接失败: 会话未连接");
                }
            } catch (Exception e) {
                callback.onFailure("登录失败: " + e.getMessage());
            }
        });
    }

    // 执行命令方法
    public void executeCommand(LoginInfo loginInfo, String command, ExecuteCallback callback) {
        executorService.execute(() -> {
            Channel channel = null;
            InputStream in = null, extIn = null;
            try {
                // 获取或创建会话
                Session session = getOrCreateSession(loginInfo);

                if (session.isConnected()) {
                    // 打开通道
                    channel = session.openChannel("exec");
                    currentChannel = channel;
                    ((ChannelExec) channel).setCommand(command);
                    channel.setInputStream(null);

                    // 获取输出流
                    in = channel.getInputStream();
                    extIn = channel.getExtInputStream();
                    channel.connect();

                    // 读取输出
                    StringBuilder result = new StringBuilder();
                    byte[] tmp = new byte[1024];
                    byte[] errTmp = new byte[1024];
                    while (true) {
                        // 读取标准输出
                        while (in.available() > 0) {
                            int i = in.read(tmp, 0, 1024);
                            if (i < 0) break;
                            String output = new String(tmp, 0, i);
                            result.append(output);
                            callback.onOutput(output);
                        }
                        // 读取错误输出
                        while (extIn.available() > 0) {
                            int i = extIn.read(errTmp, 0, 1024);
                            if (i < 0) break;
                            String errorOutput = new String(errTmp, 0, i);
                            result.append(errorOutput);
                            callback.onOutput(errorOutput);
                        }
                        if (channel.isClosed()) {
                            if (in.available() > 0 || extIn.available() > 0) continue;
                            // 检查退出状态是否为0，0表示成功
                            if (channel.getExitStatus() != 0) {
                                callback.onFailure("命令执行失败" + result + " 退出状态: " + channel.getExitStatus());
                                return;
                            }
                            break;
                        }
                        try {
                            Thread.sleep(50); // 减少睡眠时间，提高响应速度
                        } catch (Exception e) {
                            //ignore
                        }
                    }

                    callback.onSuccess("命令执行成功");
                } else {
                    callback.onFailure("连接失败: 会话未连接");
                }
            } catch (Exception e) {
                //打印异常堆栈到失败信息里面
                callback.onOutput("执行命令失败: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
                callback.onFailure("执行命令失败");
            } finally {
                // 关闭通道和输入流，但保留会话
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        //
                    }
                }
                if (extIn != null) {
                    try {
                        extIn.close();
                    } catch (IOException e) {
                        //
                    }
                }
                if (channel != null && channel.isConnected()) {
                    channel.disconnect();
                }
                currentChannel = null;
            }
        });
    }

    // 创建交互式终端通道
    public ChannelShell createInteractiveShell(LoginInfo loginInfo) throws Exception {
        // 获取或创建会话
        Session session = getOrCreateSession(loginInfo);

        if (session.isConnected()) {
            // 打开shell通道
            ChannelShell channel = (ChannelShell) session.openChannel("shell");

            // 设置PTY选项
            channel.setPty(true);
            channel.setPtyType("xterm");

            return channel;
        } else {
            throw new Exception("连接失败: 会话未连接");
        }
    }

    /**
     * 创建独立的终端 SSH Session
     * 每个终端会话使用独立的 Session，与端口转发业务隔离
     *
     * @param loginInfo 登录信息
     * @return Session ID（用于后续管理）
     */
    public String createTerminalSession(LoginInfo loginInfo) throws Exception {
        JSch jsch = new JSch();
        String actualCertPath = handleCertPath(loginInfo.getCertPath());

        // 添加身份
        if (loginInfo.getCertPass() == null || loginInfo.getCertPass().isEmpty()) {
            jsch.addIdentity(actualCertPath);
        } else {
            jsch.addIdentity(actualCertPath, loginInfo.getCertPass().getBytes());
        }

        // 创建独立的 Session
        Session session = jsch.getSession(loginInfo.getUsername(), loginInfo.getHost(), loginInfo.getPort());
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey");
        session.setConfig("ForwardAgent", "yes");
        session.setConfig("ServerAliveInterval", "60");
        session.setConfig("ServerAliveCountMax", "30");

        session.connect(30000);

        // 生成唯一的终端 Session ID
        String terminalSessionId = "terminal_" + (++terminalSessionCounter);
        terminalSessionPool.put(terminalSessionId, session);

        Log.d(TAG, "Created terminal session: " + terminalSessionId + ", total: " + terminalSessionPool.size());

        return terminalSessionId;
    }

    /**
     * 获取终端 Session
     *
     * @param terminalSessionId 终端 Session ID
     * @return Session 对象
     */
    public Session getTerminalSession(String terminalSessionId) {
        return terminalSessionPool.get(terminalSessionId);
    }

    /**
     * 关闭终端 Session
     *
     * @param terminalSessionId 终端 Session ID
     */
    public void closeTerminalSession(String terminalSessionId) {
        Session session = terminalSessionPool.remove(terminalSessionId);
        if (session != null) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing terminal session: " + e.getMessage());
            }
            Log.d(TAG, "Closed terminal session: " + terminalSessionId + ", remaining: " + terminalSessionPool.size());
        }
    }

    /**
     * 关闭所有终端 Session
     */
    public void closeAllTerminalSessions() {
        for (String sessionId : new ArrayList<>(terminalSessionPool.keySet())) {
            closeTerminalSession(sessionId);
        }
        terminalSessionPool.clear();
        Log.d(TAG, "Closed all terminal sessions");
    }

    /**
     * 获取终端 Session 数量
     */
    public int getTerminalSessionCount() {
        return terminalSessionPool.size();
    }

    /**
     * @deprecated 使用 createTerminalSession 替代，避免与端口转发冲突
     */
    @Deprecated
    public Session getSession(LoginInfo loginInfo) throws Exception {
        return getOrCreateSession(loginInfo);
    }

    // 处理证书路径
    private String handleCertPath(String certPath) {
        // 直接返回路径，因为我们已经在LoginActivity中处理了文件复制到内部存储的操作
        return certPath;
    }

    // 关闭指定会话
    public void closeSession(LoginInfo loginInfo) {
        String sessionId = generateSessionId(loginInfo.getHost(), loginInfo.getPort(), loginInfo.getUsername());
        Session session = sessionPool.remove(sessionId);
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    // 关闭所有会话
    public void closeAllSessions() {
        // 关闭业务 Session
        for (Session session : sessionPool.values()) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
        sessionPool.clear();

        // 关闭终端 Session
        closeAllTerminalSessions();
    }

    // 停止当前正在执行的命令
    public void stopCommand() {
        if (currentChannel != null && currentChannel.isConnected()) {
            currentChannel.disconnect();
            currentChannel = null;
        }
    }

    // 应用端口转发配置
    public synchronized void applyPortForwardingConfigs(LoginInfo loginInfo, List<PortForwardingConfig> configs, ExecuteCallback callback) {
        executorService.execute(() -> {
            try {
                // 获取或创建会话
                Session session = getOrCreateSession(loginInfo);

                if (session.isConnected()) {
                    if (forwardingConfigs != null) {

                        //compare old configs and new configs if they are equal, then skip
                        if (forwardingConfigs.equals(configs)) {
                            callback.onSuccess("端口转发配置未改变");
                            return;
                        }

                        for (PortForwardingConfig config : forwardingConfigs) {
                            try {
                                if (config.getForwardType() == PortForwardingConfig.ForwardType.TARGET_TO_LOCAL) {
                                    session.delPortForwardingL(
                                            config.getSourceIp(),
                                            config.getSourcePort()
                                    );
                                } else if (config.getForwardType() == PortForwardingConfig.ForwardType.LOCAL_TO_TARGET) {
                                    session.delPortForwardingR(
                                            config.getTargetIp(),
                                            config.getTargetPort()
                                    );
                                }
                            }
                            catch (Throwable e) {
                                //ignore
                            }
                        }
                    }


                    // 应用新的端口转发配置
                    for (PortForwardingConfig config : configs) {
                        if (config.getForwardType() == PortForwardingConfig.ForwardType.TARGET_TO_LOCAL) {
                            // 本地转发到目标（本地端口转发）
                            // setPortForwardingL(lport, rhost, rport)
                            session.setPortForwardingL(
                                    config.getSourceIp(),
                                    config.getSourcePort(),
                                    config.getTargetIp(),
                                    config.getTargetPort()
                            );
                            Log.d(TAG, "applyPortForwardingConfigs: 本地转发到目标（本地端口转发）" +
                                    " lhost: " + config.getSourceIp() +
                                    " lport: " + config.getSourcePort() +
                                    " rhost: " + config.getTargetIp() +
                                    " rport: " + config.getTargetPort());
                        } else if (config.getForwardType() == PortForwardingConfig.ForwardType.LOCAL_TO_TARGET) {
                            // 目标转发到本地（远程端口转发）
                            // setPortForwardingR(rport, lhost, lport)
                            session.setPortForwardingR(
                                    config.getTargetIp(),
                                    config.getTargetPort(),
                                    config.getSourceIp(),
                                    config.getSourcePort()
                            );
                            Log.d(TAG, "applyPortForwardingConfigs: 目标转发到本地（远程端口转发）" +
                                    " rhost: " + config.getTargetIp() +
                                    " rport: " + config.getTargetPort() +
                                    " lhost: " + config.getSourceIp() +
                                    " lport: " + config.getSourcePort());
                        }
                    }

                    this.forwardingConfigs = new ArrayList<>(configs);

                    callback.onSuccess("所有端口转发配置已生效");
                } else {
                    callback.onFailure("连接失败: 会话未连接");
                }
            } catch (Exception e) {
                callback.onOutput("执行端口转发失败: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
                callback.onFailure("执行端口转发失败: " + e.getMessage() + "\n" + Log.getStackTraceString(e));
            }
        });
    }

    // 关闭服务
    public void shutdown() {
        stopCommand();
        closeAllSessions();
        executorService.shutdown();
        Log.d(TAG, "SshService shutdown complete");
    }
}
