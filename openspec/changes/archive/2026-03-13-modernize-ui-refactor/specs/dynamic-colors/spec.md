## ADDED Requirements

### Requirement: Material You 动态色彩集成

应用应支持 Material You 动态色彩，从用户壁纸自动生成配色方案（Android 12+）。

#### Scenario: 动态色彩启用
- **WHEN** 应用在 Android 12+ 设备上运行且用户启用动态色彩
- **THEN** 应用从用户壁纸提取配色方案

#### Scenario: 动态色彩回退
- **WHEN** 应用在 Android 12 以下设备运行
- **THEN** 使用应用预设的主题色

#### Scenario: 动态色彩开关
- **WHEN** 用户在设置中关闭动态色彩
- **THEN** 应用使用预设主题色

### Requirement: 统一配色方案

应用应使用统一的配色方案，移除五颜六色的卡片背景。

#### Scenario: 功能卡片配色
- **WHEN** 首页功能卡片显示
- **THEN** 所有卡片使用统一的表面色（?attr/colorSurface），通过图标颜色区分功能

#### Scenario: 图标着色
- **WHEN** 功能图标显示
- **THEN** 使用主题强调色（?attr/colorPrimary 或 ?attr/colorSecondary）

#### Scenario: 移除彩虹配色
- **WHEN** 布局文件更新
- **THEN** 不再使用 blue_light、red_light、green_light、purple_light 等硬编码颜色

### Requirement: 预设主题色

应用应提供预设主题色选项，供用户选择。

#### Scenario: 预设色选项
- **WHEN** 用户进入主题设置
- **THEN** 显示预设主题色选项

#### Scenario: 预设色应用
- **WHEN** 用户选择预设主题色
- **THEN** 应用立即应用新主题色

#### Scenario: 预设色持久化
- **WHEN** 用户重新打开应用
- **THEN** 使用之前选择的主题色

### Requirement: 语义化颜色定义

应用应使用语义化颜色名称，便于主题切换和动态色彩集成。

#### Scenario: 语义化颜色文件
- **WHEN** 项目构建时
- **THEN** res/values/colors.xml 定义语义化颜色（primary、secondary、surface、on_surface 等）

#### Scenario: 布局使用主题属性
- **WHEN** 布局文件需要使用颜色
- **THEN** 使用主题属性引用（?attr/colorPrimary、?attr/colorSurface 等）

#### Scenario: 深色主题颜色
- **WHEN** 应用使用深色主题
- **THEN** 自动使用 res/values-night/colors.xml 中定义的颜色
