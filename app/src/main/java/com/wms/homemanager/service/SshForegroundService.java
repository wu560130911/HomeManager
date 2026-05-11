package com.wms.homemanager.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.wms.homemanager.R;
import com.wms.homemanager.model.LoginInfo;
import com.wms.homemanager.model.PortForwardingConfig;
import com.wms.homemanager.ui.activity.MainActivity;
import com.wms.homemanager.utils.LoginInfoUtils;
import com.wms.homemanager.utils.PortForwardingUtils;

import java.util.List;

public class SshForegroundService extends Service {

    public static final String CHANNEL_ID = "SshForegroundServiceChannel";
    public static final int NOTIFICATION_ID = 1;

    private SshService sshService;
    private boolean isConfigApplied = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        sshService = SshService.getInstance(this);

        // 获取 WakeLock 防止设备休眠时 CPU 停止
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HomeManager:SshService");
            wakeLock.acquire(); // 永久持有，在 onDestroy 中释放
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        // 只有在服务首次启动时才自动应用已保存的端口转发配置
        if (!isConfigApplied) {
            autoApplyPortForwardingConfigs();
            isConfigApplied = true;
        }

        // 返回START_STICKY，确保服务被系统杀死后能够重启
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 释放 WakeLock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // 注意：不在这里调用 sshService.shutdown()
        // 让 SSH 连接保持，服务重启时可以恢复端口转发
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "SSH服务通道",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("保持SSH会话和端口转发活跃");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SSH服务")
                .setContentText("保持SSH会话和端口转发活跃")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void autoApplyPortForwardingConfigs() {
        // 获取当前登录信息
        LoginInfo loginInfo = LoginInfoUtils.getCurrentLoginInfo(this);
        if (loginInfo != null) {
            // 加载已保存的端口转发配置
            List<PortForwardingConfig> configs = PortForwardingUtils.getPortForwardingConfigs(this, loginInfo);
            if (!configs.isEmpty()) {
                // 应用端口转发配置
                sshService.applyPortForwardingConfigs(loginInfo, configs, new SshService.ExecuteCallback() {
                    @Override
                    public void onSuccess(String result) {
                        Log.d("SshForegroundService", "自动端口转发生效成功");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e("SshForegroundService", "自动端口转发生效失败: " + error);
                    }

                    @Override
                    public void onOutput(String output) {
                        Log.d("SshForegroundService", "SSH命令输出: " + output);
                    }
                });
            }
        }
    }

    // 应用端口转发配置
    public void applyPortForwardingConfigs(LoginInfo loginInfo, List<PortForwardingConfig> configs) {
        if (sshService != null && loginInfo != null && !configs.isEmpty()) {
            sshService.applyPortForwardingConfigs(loginInfo, configs, new SshService.ExecuteCallback() {
                @Override
                public void onSuccess(String result) {
                    Log.d("SshForegroundService", "端口转发生效成功");
                }

                @Override
                public void onFailure(String error) {
                    Log.e("SshForegroundService", "端口转发生效失败: " + error);
                }

                @Override
                public void onOutput(String output) {
                    Log.d("SshForegroundService", "SSH命令输出: " + output);
                }
            });
        }
    }

    // 获取SSH服务实例
    public SshService getSshService() {
        return sshService;
    }
}
