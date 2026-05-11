package com.wms.homemanager.service;

import android.app.Application;
import android.os.Process;
import com.wms.homemanager.service.SshService;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 当应用被终止时，关闭SshService，释放资源
        SshService.getInstance(this).shutdown();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        // 当系统内存严重不足时，关闭SshService，释放资源
        // 注意：只有在内存严重不足时才关闭，以保持SSH会话活跃
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        // 当应用处于后台时，保持SshService活跃，以维持SSH会话和端口转发
        // 只有在内存严重不足时才考虑关闭
        if (level >= TRIM_MEMORY_COMPLETE) {
            // 内存严重不足，关闭SshService以释放资源
            // SshService.getInstance(this).shutdown();
        }
    }
}