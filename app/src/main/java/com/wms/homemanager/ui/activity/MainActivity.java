package com.wms.homemanager.ui.activity;

import com.wms.homemanager.R;
import com.wms.homemanager.databinding.ActivityMainBinding;
import com.wms.homemanager.service.SshForegroundService;
import com.wms.homemanager.ui.fragment.ShellFragmentNew;
import com.wms.homemanager.ui.fragment.WolFragment;
import com.wms.homemanager.ui.fragment.RemoteFragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_PERMISSIONS = 1;

    // 导航项
    private LinearLayout[] navItems;
    private int currentNavIndex = 0;

    // Fragment 实例（保持引用，不销毁）
    private WolFragment wolFragment;
    private ShellFragmentNew shellFragment;
    private RemoteFragment remoteFragment;

    // 当前显示的 Fragment
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 请求运行时权限
        requestPermissions();

        // 启动SSH前台服务
        startSshForegroundService();

        // 设置底部导航
        setupBottomNavigation();

        // 设置功能卡片点击事件（首页内容）
        setupFunctionCards();
    }

    private void setupBottomNavigation() {
        navItems = new LinearLayout[]{
                binding.navHome,
                binding.navWol,
                binding.navShell,
                binding.navRemote
        };

        // 初始化 Fragment 实例
        wolFragment = new WolFragment();
        shellFragment = new ShellFragmentNew();
        remoteFragment = new RemoteFragment();

        // 设置每个导航项的点击事件
        binding.navHome.setOnClickListener(v -> {
            selectNav(0);
            showHomeContent();
        });

        binding.navWol.setOnClickListener(v -> {
            selectNav(1);
            showFragment(wolFragment, "WolFragment");
        });

        binding.navShell.setOnClickListener(v -> {
            selectNav(2);
            showFragment(shellFragment, "ShellFragment");
        });

        binding.navRemote.setOnClickListener(v -> {
            selectNav(3);
            showFragment(remoteFragment, "RemoteFragment");
        });
    }

    private void selectNav(int index) {
        currentNavIndex = index;
        // 更新所有导航项的颜色
        int activeColor = ContextCompat.getColor(this, R.color.neonBlue);
        int inactiveColor = ContextCompat.getColor(this, R.color.colorOnSurfaceVariant);

        for (int i = 0; i < navItems.length; i++) {
            LinearLayout navItem = navItems[i];
            ImageView icon = (ImageView) navItem.getChildAt(0);
            TextView text = (TextView) navItem.getChildAt(1);

            if (i == index) {
                icon.setColorFilter(activeColor);
                text.setTextColor(activeColor);
            } else {
                icon.setColorFilter(inactiveColor);
                text.setTextColor(inactiveColor);
            }
        }
    }

    private void setupFunctionCards() {
        binding.cvWol.setOnClickListener(v -> {
            selectNav(1);
            showFragment(wolFragment, "WolFragment");
        });

        binding.cvShell.setOnClickListener(v -> {
            selectNav(2);
            showFragment(shellFragment, "ShellFragment");
        });

        binding.cvRemote.setOnClickListener(v -> {
            selectNav(3);
            showFragment(remoteFragment, "RemoteFragment");
        });

        binding.cvSettings.setOnClickListener(v -> {
            // 设置功能（暂未实现）
        });
    }

    private void requestPermissions() {
        // INTERNET 和 ACCESS_NETWORK_STATE 是普通权限，不需要运行时申请
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 显示指定的 Fragment（使用 show/hide 保持状态）
    private void showFragment(Fragment fragment, String tag) {
        // 隐藏首页内容
        binding.homeContent.setVisibility(View.GONE);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // 隐藏当前显示的 Fragment
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 如果 Fragment 未添加，则添加
        if (!fragment.isAdded()) {
            transaction.add(R.id.fragment_container, fragment, tag);
        } else {
            // Fragment 已存在，直接显示
            transaction.show(fragment);
        }

        transaction.commit();
        currentFragment = fragment;

        // 更新标题栏
        updateTitle(tag);
    }

    // 显示首页内容
    private void showHomeContent() {
        // 隐藏当前 Fragment
        if (currentFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .hide(currentFragment)
                    .commit();
        }

        // 显示首页内容
        binding.homeContent.setVisibility(View.VISIBLE);

        // 恢复原标题
        binding.tvTitle.setText(R.string.app_name);
    }

    // 更新标题栏
    private void updateTitle(String fragmentTag) {
        if (fragmentTag.equals("WolFragment")) {
            binding.tvTitle.setText(R.string.menu_wol);
        } else if (fragmentTag.equals("ShellFragment")) {
            binding.tvTitle.setText(R.string.menu_shell);
        } else if (fragmentTag.equals("RemoteFragment")) {
            binding.tvTitle.setText(R.string.menu_remote);
        }
    }

    @Override
    public void onBackPressed() {
        // 如果当前显示的是 Fragment，返回首页
        if (currentFragment != null && currentFragment.isVisible()) {
            showHomeContent();
            selectNav(0);
        } else {
            super.onBackPressed();
        }
    }

    // 启动SSH前台服务
    private void startSshForegroundService() {
        Intent serviceIntent = new Intent(this, SshForegroundService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 检查并请求电池优化白名单
        checkBatteryOptimization();
    }

    // 检查并请求电池优化白名单
    private void checkBatteryOptimization() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();

            // 检查是否已在白名单中
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // 显示对话框，提示用户加入白名单
                new AlertDialog.Builder(this)
                        .setTitle("电池优化设置")
                        .setMessage("为了确保SSH端口转发在后台正常运行，需要将应用加入电池优化白名单。是否立即设置？")
                        .setPositiveButton("去设置", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + packageName));
                                startActivity(intent);
                            } catch (Exception e) {
                                // 如果无法打开设置页面，打开电池优化设置
                                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("以后再说", null)
                        .setCancelable(true)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
