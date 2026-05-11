## ADDED Requirements

### Requirement: Fragment 过渡动画

所有 Fragment 切换应使用 Material Motion 过渡动画。

#### Scenario: Fragment 进入动画
- **WHEN** Fragment 被加载
- **THEN** 使用 MaterialFade 或 MaterialSharedAxis 进入动画

#### Scenario: Fragment 退出动画
- **WHEN** Fragment 被替换或移除
- **THEN** 使用对应的退出动画

#### Scenario: 动画一致性
- **WHEN** 在不同 Fragment 之间切换
- **THEN** 使用统一的过渡动画风格

### Requirement: 卡片涟漪效果

所有可点击的卡片应具有涟漪触摸反馈效果。

#### Scenario: 有边界涟漪效果
- **WHEN** 卡片具有明确的边界
- **THEN** 使用 `?android:attr/selectableItemBackground` 作为前景

#### Scenario: 无边界涟漪效果
- **WHEN** 卡片需要扩展到边界的涟漪效果
- **THEN** 使用 `?attr/selectableItemBackgroundBorderless` 作为前景

#### Scenario: 卡片可点击性
- **WHEN** 卡片需要响应点击
- **THEN** 设置 `android:clickable="true"` 和 `android:focusable="true"`

### Requirement: 列表项动画

RecyclerView 列表项应具有流畅的添加、删除、移动动画。

#### Scenario: 列表项添加动画
- **WHEN** 新项添加到列表
- **THEN** 新项以动画方式进入视图

#### Scenario: 列表项删除动画
- **WHEN** 列表项被删除
- **THEN** 项以动画方式退出视图

#### Scenario: 列表项移动动画
- **WHEN** 列表项位置改变
- **THEN** 项以动画方式移动到新位置

### Requirement: 按钮点击反馈

所有按钮应具有明显的点击反馈效果。

#### Scenario: MaterialButton 默认涟漪
- **WHEN** 用户点击 MaterialButton
- **THEN** 按钮显示涟漪效果

#### Scenario: 按钮状态变化
- **WHEN** 按钮被按下
- **THEN** 按钮显示按下状态

### Requirement: 加载状态动画

长时间操作应显示加载动画，避免界面冻结。

#### Scenario: 命令执行中状态
- **WHEN** 用户执行 Shell 命令
- **THEN** 显示加载动画或进度指示器

#### Scenario: 网络请求状态
- **WHEN** 进行网络操作（SSH 连接）
- **THEN** 显示连接状态动画

#### Scenario: 加载完成
- **WHEN** 操作完成
- **THEN** 加载动画消失，显示结果

### Requirement: 底部导航切换动画

底部导航项切换应有平滑的过渡效果。

#### Scenario: 导航项选中动画
- **WHEN** 用户选择底部导航项
- **THEN** 该项显示选中动画（图标放大、标签变化）

#### Scenario: 导航内容切换
- **WHEN** 导航切换触发 Fragment 变化
- **THEN** 内容区域使用过渡动画切换
