## ADDED Requirements

### Requirement: 提供预设主题

系统 SHALL 提供多种预设终端主题供用户选择。

#### Scenario: 选择预设主题
- **WHEN** 用户在设置中选择预设主题
- **THEN** 终端应用选中的主题配色

#### Scenario: 预设主题列表
- **WHEN** 系统初始化主题功能
- **THEN** 提供至少以下预设主题：
  - 默认绿（Default Green）
  - 暗夜黑（Dark Night）
  - Solarized Dark
  - Solarized Light
  - Monokai
  - Dracula

### Requirement: 自定义主题配色

系统 SHALL 支持用户自定义终端的前景色、背景色和颜色调色板。

#### Scenario: 自定义前景色和背景色
- **WHEN** 用户在主题设置中选择自定义颜色
- **THEN** 终端使用指定的前景色和背景色

#### Scenario: 自定义 ANSI 调色板
- **WHEN** 用户自定义 16 色调色板
- **THEN** ANSI 标准颜色使用自定义值显示

### Requirement: 字体设置

系统 SHALL 支持调整终端字体大小和选择等宽字体。

#### Scenario: 调整字体大小
- **WHEN** 用户调整字体大小设置
- **THEN** 终端使用新的字体大小显示

#### Scenario: 手势缩放字体
- **WHEN** 用户在终端双指缩放
- **THEN** 字体大小实时调整

#### Scenario: 选择等宽字体
- **WHEN** 用户选择字体类型
- **THEN** 终端使用选中的等宽字体

### Requirement: 主题持久化

系统 SHALL 保存用户的主题设置，下次打开时自动应用。

#### Scenario: 保存主题设置
- **WHEN** 用户更改主题设置
- **THEN** 设置立即保存到本地

#### Scenario: 加载主题设置
- **WHEN** 用户打开终端
- **THEN** 自动应用上次保存的主题设置

### Requirement: 主题与 APP 风格一致

系统 SHALL 确保终端主题与 HomeManager APP 的科技风 UI 风格保持一致。

#### Scenario: 默认主题风格
- **WHEN** 用户未自定义主题
- **THEN** 默认主题使用科技风配色（深色背景 + 霓虹绿前景）

#### Scenario: 主题切换动画
- **WHEN** 用户切换主题
- **THEN** 切换过程平滑过渡，无闪烁
