## Why

当前 HomeManager 应用的 UI 存在三个核心问题：

1. **视觉层面**：配色是

## What Changes

### 技术基础设施
- **迁移到 ViewBinding**，替代 `findViewById` 以提升类型安全和开发效率
- **将 ListView 替换为 RecyclerView**，提升列表性能和灵活性
- **清理无效的 drawable 资源**（约 4 个未使用文件）
- **适配 Android 16 API**，确保兼容性和最佳实践

### Material Design 3 集成
- **替换自定义底部导航**为 Material 3 的 `BottomNavigationView` 组件
- **统一使用 MaterialButton**，替换自定义 drawable 按钮
- **使用 TextInputLayout 包装 EditText**，提供统一的输入体验
- **集成 Material You 动态色彩**，从用户壁纸自动生成配色方案

### 视觉设计升级
- **创建统一的样式系统**（styles.xml），消除重复的样式定义
- **实现深色主题支持**，完善 Material 3 DayNight 主题
- **移动硬编码字符串**到 strings.xml，支持国际化
- **优化布局性能**，使用 ConstraintLayout 的最佳实践

### 动画与微交互
- **Fragment 过渡动画**，使用 Material Motion 提升页面切换体验
- **卡片涟漪效果**，增强触摸反馈
- **列表项动画**，提升 RecyclerView 的视觉流畅度

### 终端体验优化
- **支持等宽字体选择**（Fira Code、JetBrains Mono 等）
- **支持配色方案切换**（Solarized、Dracula、Nord 等）
- **支持字体大小调节**，提升可访问性

## Capabilities

### New Capabilities

- `design-system`: 统一的 UI 设计系统，包含样式、颜色、字体、间距规范
- `view-binding`: ViewBinding 集成，提供类型安全的视图访问
- `dark-theme`: 完整的深色主题支持
- `modern-components`: 使用 Material 3 组件替换传统实现
- `animation-experience`: Material Motion 动画与微交互
- `terminal-experience`: 专业的终端界面体验（字体、配色方案）
- `dynamic-colors`: Material You 动态色彩集成

### Modified Capabilities

- `code-structure-refactor`: 配合 UI 重构调整代码结构

## Impact

### 影响的代码文件
- `ui/activity/MainActivity.java` - 导航逻辑重构 + 动画集成
- `ui/activity/LoginActivity.java` - ViewBinding 集成
- `ui/fragment/WolFragment.java` - ViewBinding 集成
- `ui/fragment/ShellFragment.java` - ViewBinding + 终端体验优化
- `ui/fragment/RemoteFragment.java` - RecyclerView 迁移 + ViewBinding
- `ui/adapter/*` - 适配器更新

### 影响的资源文件
- `res/layout/*.xml` - 所有布局文件更新
- `res/values/themes.xml` - 主题扩展 + 动态色彩
- `res/values/colors.xml` - 颜色系统优化
- `res/values/colors.xml (night)` - 深色主题颜色
- `res/values/styles.xml` - 新建样式定义
- `res/values/strings.xml` - 字符串提取
- `res/drawable/*.xml` - 清理无效资源
- `res/xml/font_certs.xml` - 字体证书配置（如需 Google Fonts）

### 依赖变更
- 无新增外部依赖（使用现有的 Material Design 库 1.13.0）
- 启用 ViewBinding 需要在 build.gradle 中配置
- 可能需要添加 Google Fonts 支持（可选）

### API 兼容性
- minSdk: 24（保持不变）
- targetSdk: 36（适配 Android 16）
- compileSdk: 36（保持不变）
