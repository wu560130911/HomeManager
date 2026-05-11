## 1. 基础设施准备

- [x] 1.1 在 app/build.gradle 中启用 ViewBinding (buildFeatures.viewBinding = true)
- [x] 1.2 创建 res/values/styles.xml 文件
- [x] 1.3 创建 res/values/dimens.xml 文件定义标准间距
- [x] 1.4 创建 res/values-night/colors.xml 文件用于深色主题

## 2. 设计系统实现

### 2.1 颜色系统

- [x] 2.1.1 定义语义化颜色：primary, secondary, background, surface, error, on_primary, on_secondary 等
- [x] 2.1.2 移除硬编码的颜色值（blue_light, red_light 等），统一使用主题色
- [x] 2.1.3 功能卡片图标着色统一使用 ?attr/colorPrimary

### 2.2 样式定义

- [x] 2.2.1 定义按钮样式：Widget.HomeManager.Button.Primary, Secondary, Danger
- [x] 2.2.2 定义卡片样式：Widget.HomeManager.Card
- [x] 2.2.3 定义输入框样式：Widget.HomeManager.TextInput
- [x] 2.2.4 定义文字外观样式：TextAppearance.HomeManager.Headline, Body, Caption
- [x] 2.2.5 定义标准间距：margin_small, margin_medium, margin_large, padding_small, padding_medium

## 3. ViewBinding 集成

- [x] 3.1 重构 MainActivity 使用 ViewBinding (ActivityMainBinding)
- [x] 3.2 重构 LoginActivity 使用 ViewBinding (ActivityLoginBinding)
- [x] 3.3 重构 WolFragment 使用 ViewBinding
- [x] 3.4 重构 ShellFragment 使用 ViewBinding
- [x] 3.5 重构 RemoteFragment 使用 ViewBinding (FragmentRemoteBinding)
- [x] 3.6 重构 LoginListAdapter 使用 ViewBinding (ItemLoginInfoBinding)
- [x] 3.7 重构 PortForwardingAdapter 使用 ViewBinding (ItemPortForwardingBinding)
- [x] 3.8 在各 Fragment 中添加 binding 置空逻辑 (onDestroyView)

## 4. 组件现代化

### 4.1 底部导航重构

- [x] 4.1.1 创建 res/menu/bottom_navigation.xml 定义导航菜单
- [x] 4.1.2 修改 activity_main.xml：移除自定义底部导航，添加 BottomNavigationView
- [x] 4.1.3 重构 MainActivity 导航逻辑：使用 BottomNavigationView.OnItemSelectedListener
- [x] 4.1.4 删除自定义导航相关的 XML 代码（约 120 行）

### 4.2 按钮组件迁移

- [x] 4.2.1 LoginActivity 按钮迁移到 MaterialButton
- [x] 4.2.2 WolFragment 按钮迁移到 MaterialButton
- [x] 4.2.3 ShellFragment 按钮迁移到 MaterialButton
- [x] 4.2.4 RemoteFragment 按钮迁移到 MaterialButton
- [x] 4.2.5 对话框按钮迁移到 MaterialButton

### 4.3 输入框组件迁移

- [x] 4.3.1 LoginActivity 输入框包装到 TextInputLayout
- [x] 4.3.2 add_device_dialog.xml 输入框包装到 TextInputLayout
- [x] 4.3.3 dialog_port_forwarding.xml 输入框包装到 TextInputLayout

### 4.4 RecyclerView 迁移

- [x] 4.4.1 修改 fragment_remote.xml：ListView 替换为 RecyclerView
- [x] 4.4.2 更新 PortForwardingAdapter 继承 RecyclerView.Adapter
- [x] 4.4.3 创建 RecyclerView.ViewHolder 实现
- [x] 4.4.4 在 RemoteFragment 中配置 RecyclerView (LayoutManager, Adapter)

### 4.5 对话框现代化

- [x] 4.5.1 重构 add_device_dialog.xml 使用 Material 风格
- [x] 4.5.2 重构 dialog_port_forwarding.xml 使用 Material 风格

## 5. 深色主题实现

- [x] 5.1 定义深色主题背景色（colorBackgroundDark, colorSurfaceDark）
- [x] 5.2 定义深色主题文字颜色（colorOnBackgroundDark, colorOnSurfaceDark）
- [x] 5.3 定义深色主题强调色（colorPrimaryDark, colorSecondaryDark）
- [x] 5.4 定义深色主题状态栏颜色
- [x] 5.5 更新 themes.xml 支持深色主题属性
- [x] 5.6 测试所有界面在深色主题下的显示效果

## 6. 动画与微交互

### 6.1 Fragment 过渡动画

- [x] 6.1.1 在 MainActivity 中配置 Fragment 过渡动画
- [x] 6.1.2 为各 Fragment 设置 MaterialFade 进入/退出动画
- [x] 6.1.3 测试过渡动画流畅性

### 6.2 卡片涟漪效果

- [x] 6.2.1 为首页功能卡片添加 selectableItemBackground 前景
- [x] 6.2.2 确保卡片设置 clickable 和 focusable 属性
- [x] 6.2.3 测试涟漪效果响应性

### 6.3 列表项动画

- [x] 6.3.1 为 RecyclerView 配置 ItemAnimator
- [x] 6.3.2 测试列表项添加/删除动画

## 7. 终端体验优化

### 7.1 字体支持

- [ ] 7.1.1 创建终端设置界面（字体大小、配色方案）
- [ ] 7.1.2 实现字体大小调节功能（小/中/大/特大）
- [ ] 7.1.3 保存字体大小偏好到 SharedPreferences

### 7.2 配色方案

- [ ] 7.2.1 定义预设配色方案（Default, Solarized Dark, Dracula, Nord）
- [ ] 7.2.2 实现配色方案选择功能
- [ ] 7.2.3 保存配色方案偏好到 SharedPreferences

### 7.3 终端界面优化

- [ ] 7.3.1 优化终端卡片样式（圆角、阴影）
- [ ] 7.3.2 优化输入框样式（与终端配色一致）
- [ ] 7.3.3 测试终端在不同配色方案下的显示效果

## 8. 动态色彩（Material You）

### 8.1 动态色彩集成

- [ ] 8.1.1 在 App.java 中调用 DynamicColors.applyIfAvailable()
- [ ] 8.1.2 创建主题设置界面（动态色彩开关、预设主题色）
- [ ] 8.1.3 实现动态色彩开关功能

### 8.2 预设主题色

- [ ] 8.2.1 定义预设主题色选项（蓝、绿、紫、橙等）
- [ ] 8.2.2 实现主题色选择功能
- [ ] 8.2.3 保存主题色偏好到 SharedPreferences

## 9. 资源清理

- [x] 9.1 删除未使用的 drawable：btn_primary.xml
- [x] 9.2 删除未使用的 drawable：device_item_background.xml
- [x] 9.3 删除未使用的 drawable：command_input_background.xml
- [x] 9.4 删除未使用的 drawable：result_background.xml
- [x] 9.5 移除布局中的硬编码颜色值，替换为颜色资源引用
- [x] 9.6 提取硬编码字符串到 strings.xml

## 10. 测试与验证

- [x] 10.1 执行 ./gradlew assembleDebug 验证编译成功
- [x] 10.2 执行 ./gradlew lint 验证无严重警告
- [ ] 10.3 手动测试登录功能正常
- [ ] 10.4 手动测试底部导航切换正常
- [ ] 10.5 手动测试 WOL 唤醒功能正常
- [ ] 10.6 手动测试 Shell 命令执行功能正常
- [ ] 10.7 手动测试端口转发配置功能正常
- [ ] 10.8 手动测试深色主题切换正常
- [ ] 10.9 手动测试动态色彩功能正常
- [ ] 10.10 手动测试终端字体和配色切换正常
- [ ] 10.11 验证无内存泄漏（Fragment ViewBinding 置空）
- [ ] 10.12 测试所有动画流畅无卡顿
