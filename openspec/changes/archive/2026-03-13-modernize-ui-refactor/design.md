## Context

HomeManager 是一个原生 Android 应用，目前使用 Material 3 主题作为基础，但 UI 实现存在以下问题：
- 自定义底部导航（约 120 行 XML），未使用标准 Material 组件
- 缺乏统一的样式系统（无 styles.xml）
- 混合使用 ListView 和 RecyclerView
- 大量硬编码字符串（约 12 处）
- 未使用 ViewBinding，依赖 `findViewById`
- 深色主题声明但未实际实现
- 4 个未使用的 drawable 资源

**约束条件：**
- minSdk 24，targetSdk 36
- 无新增外部依赖
- 保持现有功能不变
- 不影响 SSH 核心功能

## Goals / Non-Goals

**Goals:**
- 实现现代化的 Material 3 UI 设计系统
- 提升代码可维护性（ViewBinding、样式复用）
- 优化应用性能（RecyclerView、布局优化）
- 完整支持深色主题
- 适配 Android 16 API
- 减少无效资源以优化 APK 体积

**Non-Goals:**
- 不改变应用功能或业务逻辑
- 不重构 SSH 服务层代码
- 不添加新功能
- 不更改应用架构模式

## Decisions

### 1. 导航组件选择

**决定：使用 BottomNavigationView 替换自定义导航**

理由：
- Material 3 官方组件，自动适配主题
- 内置选中状态动画和涟漪效果
- 减少约 120 行自定义 XML
- 自动处理无障碍功能

备选方案：
- 保留自定义导航 + 手动优化 → 维护成本高，不符合 Material 规范

### 2. 视图绑定方案

**决定：启用 ViewBinding**

理由：
- 编译时类型安全
- 自动生成绑定类，无需手动编写
- 支持 null 安全
- 性能优于 findViewById（无运行时反射）

备选方案：
- DataBinding → 对于本项目过度设计
- 继续使用 findViewById → 类型不安全，易出错

### 3. 列表组件迁移

**决定：RemoteFragment 的 ListView 迁移到 RecyclerView**

理由：
- ViewHolder 模式自动回收视图
- 更灵活的布局管理器
- 内置动画支持
- 更好的性能表现

备选方案：
- 保留 ListView → 性能差，功能受限

### 4. 样式系统设计

**决定：创建统一的 styles.xml，定义以下样式类别**

```
样式层级：
├── Widget.HomeManager.Button          # 基础按钮样式
│   ├── Widget.HomeManager.Button.Primary
│   ├── Widget.HomeManager.Button.Secondary
│   └── Widget.HomeManager.Button.Danger
├── Widget.HomeManager.Card            # 卡片样式
├── Widget.HomeManager.TextInput       # 输入框样式
└── TextAppearance.HomeManager.*       # 文字样式
```

理由：
- 集中管理，便于全局修改
- 减少布局文件重复代码
- 确保视觉一致性

### 5. 深色主题实现

**决定：创建 values-night/colors.xml 定义深色主题颜色**

理由：
- 遵循 Material 3 标准
- 仅需覆盖颜色值，无需修改布局
- 系统自动根据系统主题切换

备选方案：
- 强制使用浅色主题 → 用户体验差
- 手动实现主题切换 → 增加复杂度

### 6. 资源清理策略

**决定：移除未使用的 drawable 资源**

已识别的无用资源：
- `btn_primary.xml`
- `device_item_background.xml`
- `command_input_background.xml`
- `result_background.xml`

理由：
- 减少 APK 体积
- 减少维护负担
- 降低混淆风险

## Risks / Trade-offs

### 风险 1：ViewBinding 迁移可能导致编译错误
- **影响**：所有使用 `findViewById` 的代码需要更新
- **缓解**：逐步迁移，先完成一个 Fragment 作为模板

### 风险 2：深色主题可能暴露 UI 问题
- **影响**：部分颜色在深色背景下可能不清晰
- **缓解**：测试所有界面在深色主题下的可读性

### 风险 3：BottomNavigationView 可能与现有逻辑冲突
- **影响**：导航切换逻辑需要重写
- **缓解**：保留原有导航状态管理逻辑，仅替换 UI 组件

### 风险 4：RecyclerView 迁移可能影响列表状态
- **影响**：滚动位置、选中状态可能丢失
- **缓解**：使用 `RecyclerView.ScrollListener` 保存状态

## 动画设计决策

### Fragment 过渡动画
- 使用 Material Motion 库提供的过渡效果
- 推荐使用 `MaterialFade` 或 `MaterialSharedAxis`

```xml
<!-- 使用 Material Motion 标准过渡 -->

### 风险 5：动画可能影响低端设备性能
- **影响**：低端设备可能出现卡顿
- **缓解**：使用轻量级动画，在低端设备上禁用复杂动画

## 动画设计决策

### Fragment 过渡动画
```xml
<!-- 使用 Material Motion 标准过渡 -->
<fragment
    android:transitionName="shared_element"
    ... />
```

在代码中使用：
```java
// 使用 MaterialContainerTransform 或 Fade
setEnterTransition(new MaterialFade());
setExitTransition(new MaterialFade());
```

### 卡片涟漪效果
- 使用 `?android:attr/selectableItemBackground` 或 `?attr/selectableItemBackgroundBorderless`
- 为卡片添加 `android:clickable="true"` 和 `android:focusable="true"`

### 列表项动画
```java
// RecyclerView ItemAnimator 默认使用 DefaultItemAnimator
// 可自定义：
recyclerView.setItemAnimator(new SlideInUpAnimator());
```

## 终端设计决策

### 字体方案
- 默认使用系统等宽字体 `monospace`
- 可选下载 Google Fonts：
  - Fira Code（支持连字）
  - JetBrains Mono（JetBrains 出品）
  - Source Code Pro（Adobe 出品）

### 配色方案
预设配色方案：
1. **Default**：绿色文字（#00FF00）+ 黑色背景
2. **Solarized Dark**：经典的 Solarized 配色
3. **Dracula**：流行的深色主题
4. **Nord**：北极风的冷色调

实现方式：
```java
// 使用 SharedPreferences 存储用户选择
// 在 ShellFragment 中应用配色
int colorScheme = prefs.getInt("terminal_color_scheme", 0);
applyColorScheme(colorScheme);
```

## Migration Plan

### 阶段 1：基础设施准备
1. 在 build.gradle 中启用 ViewBinding
2. 创建 styles.xml 样式系统
3. 创建 values-night/colors.xml 深色主题颜色

### 阶段 2：组件迁移
1. 替换底部导航为 BottomNavigationView
2. RemoteFragment ListView → RecyclerView
3. 所有 EditText 迁移到 TextInputLayout
4. 所有 Button 迁移到 MaterialButton

### 阶段 3：代码重构
1. Activity/Fragment 迁移到 ViewBinding
2. 提取硬编码字符串到 strings.xml
3. 应用统一样式到所有布局文件

### 阶段 4：清理与优化
1. 删除未使用的 drawable 资源
2. 删除过时的样式和主题定义
3. 运行 Lint 检查确保无警告

### 回滚策略
- 保留 Git 分支记录每个阶段的完成状态
- 如遇严重问题，可回滚到上一阶段的稳定版本

## Open Questions

1. **是否需要支持动态主题切换？**
   - 当前设计仅支持跟随系统
   - 如需应用内切换，需引入 ThemeOverlay 机制

2. **是否需要添加启动画面（Splash Screen）？**
   - Android 12+ 推荐使用 Splash Screen API
   - 可作为后续优化项
