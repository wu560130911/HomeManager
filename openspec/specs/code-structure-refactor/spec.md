## ADDED Requirements

### Requirement: 建立分层包结构

项目源代码应采用分层包结构，将代码按照功能类型分别存放在 ui、service、model、util 四个子包中。

#### Scenario: 包结构创建
- **WHEN** 代码重构完成后
- **THEN** 存在以下包结构：
  - com.wms.homemanager.ui.activity
  - com.wms.homemanager.ui.fragment
  - com.wms.homemanager.ui.adapter
  - com.wms.homemanager.service
  - com.wms.homemanager.model
  - com.wms.homemanager.util

### Requirement: UI 层文件正确归类

所有 Activity、Fragment 和 Adapter 文件应移动到对应的 ui 子包中。

#### Scenario: Activity 文件归类
- **WHEN** 重构完成后
- **THEN** MainActivity 在 ui.activity 包中，LoginActivity 在 ui.activity 包中

#### Scenario: Fragment 文件归类
- **WHEN** 重构完成后
- **THEN** WolFragment、ShellFragment、RemoteFragment 在 ui.fragment 包中

#### Scenario: Adapter 文件归类
- **WHEN** 重构完成后
- **THEN** LoginListAdapter、PortForwardingAdapter、PortForwardingDialog 在 ui.adapter 包中

### Requirement: Service 层文件正确归类

所有服务层文件应移动到 service 子包中。

#### Scenario: Service 文件归类
- **WHEN** 重构完成后
- **THEN** SshService、SshForegroundService 在 service 包中

### Requirement: Model 层文件正确归类

所有数据模型文件应移动到 model 子包中。

#### Scenario: Model 文件归类
- **WHEN** 重构完成后
- **THEN** LoginInfo、PortForwardingConfig 在 model 包中

### Requirement: Util 层文件正确归类

所有工具类文件应移动到 util 子包中。

#### Scenario: Util 文件归类
- **WHEN** 重构完成后
- **THEN** LoginInfoUtils、PortForwardingUtils、FileUtils 在 util 包中

### Requirement: 代码编译成功

重构后的代码应能够成功编译，不应引入新的编译错误。

#### Scenario: 编译验证
- **WHEN** 执行 ./gradlew assembleDebug 命令
- **THEN** 构建成功，生成 APK 文件

### Requirement: 功能保持不变

重构不应改变应用的现有功能。

#### Scenario: 登录功能验证
- **WHEN** 用户通过 LoginActivity 登录
- **THEN** 登录成功，保存登录信息

#### Scenario: Shell 命令功能验证
- **WHEN** 用户在 ShellFragment 执行命令
- **THEN** 命令正确执行，返回结果

#### Scenario: 远程唤醒功能验证
- **WHEN** 用户在 WolFragment 唤醒设备
- **THEN** 唤醒命令正确发送

#### Scenario: 端口转发功能验证
- **WHEN** 用户在 RemoteFragment 配置端口转发
- **THEN** 端口转发正确建立
