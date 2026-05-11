## ADDED Requirements

### Requirement: 深色主题颜色定义

应用应为深色主题定义专用的颜色资源。

#### Scenario: 深色主题颜色文件存在
- **WHEN** 项目构建时
- **THEN** res/values-night/colors.xml 文件存在

#### Scenario: 深色主题背景色
- **WHEN** 应用运行在深色主题下
- **THEN** 背景色使用适合深色主题的颜色值

#### Scenario: 深色主题文字颜色
- **WHEN** 应用运行在深色主题下
- **THEN** 文字颜色与深色背景形成足够对比度（WCAG AA 标准）

#### Scenario: 深色主题强调色
- **WHEN** 应用运行在深色主题下
- **THEN** 强调色（primary、secondary）适配深色背景

### Requirement: 主题自动切换

应用应支持跟随系统主题自动切换深色/浅色模式。

#### Scenario: 系统浅色主题
- **WHEN** 系统设置为浅色主题
- **THEN** 应用使用浅色主题颜色

#### Scenario: 系统深色主题
- **WHEN** 系统设置为深色主题
- **THEN** 应用使用深色主题颜色

### Requirement: 所有界面深色主题适配

应用的每个界面都应在深色主题下正确显示。

#### Scenario: 登录界面深色适配
- **WHEN** LoginActivity 在深色主题下显示
- **THEN** 所有文字、背景、按钮颜色正确适配

#### Scenario: 主界面深色适配
- **WHEN** MainActivity 在深色主题下显示
- **THEN** 导航栏、内容区域颜色正确适配

#### Scenario: Shell 界面深色适配
- **WHEN** ShellFragment 在深色主题下显示
- **THEN** 终端背景、文字、输入框颜色正确适配

#### Scenario: 远程唤醒界面深色适配
- **WHEN** WolFragment 在深色主题下显示
- **THEN** 列表项、按钮颜色正确适配

#### Scenario: 端口转发界面深色适配
- **WHEN** RemoteFragment 在深色主题下显示
- **THEN** 列表项、对话框颜色正确适配

### Requirement: 状态栏和导航栏适配

系统状态栏和导航栏应与主题颜色协调。

#### Scenario: 浅色主题状态栏
- **WHEN** 应用使用浅色主题
- **THEN** 状态栏使用浅色背景，深色图标

#### Scenario: 深色主题状态栏
- **WHEN** 应用使用深色主题
- **THEN** 状态栏使用深色背景，浅色图标
