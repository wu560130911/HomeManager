## 1. 创建包结构

- [x] 1.1 创建 ui.activity 包（存放 Activity 类）
- [x] 1.2 创建 ui.fragment 包（存放 Fragment 类）
- [x] 1.3 创建 ui.adapter 包（存放 Adapter 类）
- [x] 1.4 创建 service 包（存放服务类）
- [x] 1.5 创建 model 包（存放数据模型类）
- [x] 1.6 确认 util 包已存在

## 2. 移动 UI 层文件

- [x] 2.1 将 MainActivity.java 移动到 ui.activity 包
- [x] 2.2 将 LoginActivity.java 移动到 ui.activity 包
- [x] 2.3 将 WolFragment.java 移动到 ui.fragment 包
- [x] 2.4 将 ShellFragment.java 移动到 ui.fragment 包
- [x] 2.5 将 RemoteFragment.java 移动到 ui.fragment 包
- [x] 2.6 将 LoginListAdapter.java 移动到 ui.adapter 包
- [x] 2.7 将 PortForwardingAdapter.java 移动到 ui.adapter 包
- [x] 2.8 将 PortForwardingDialog.java 移动到 ui.adapter 包
- [x] 2.9 更新所有移动文件的 import 语句

## 3. 移动 Service 层文件

- [x] 3.1 将 SshService.java 移动到 service 包
- [x] 3.2 将 SshForegroundService.java 移动到 service 包
- [x] 3.3 将 App.java 移动到 service 包
- [x] 3.4 更新所有移动文件的 import 语句

## 4. 移动 Model 层文件

- [x] 4.1 将 LoginInfo.java 移动到 model 包
- [x] 4.2 将 PortForwardingConfig.java 移动到 model 包
- [x] 4.3 更新所有移动文件的 import 语句

## 5. 验证与测试

- [x] 5.1 执行 ./gradlew clean 清理构建
- [x] 5.2 执行 ./gradlew assembleDebug 编译项目
- [x] 5.3 修复编译错误（如果有）
- [ ] 5.4 在设备上测试登录功能
- [ ] 5.5 在设备上测试 Shell 命令功能
- [ ] 5.6 在设备上测试远程唤醒功能
- [ ] 5.7 在设备上测试端口转发功能

## 6. 清理

- [x] 6.1 检查并删除空包（如果存在）
- [x] 6.2 确认所有文件都在正确位置
