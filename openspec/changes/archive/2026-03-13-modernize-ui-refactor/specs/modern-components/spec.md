## ADDED Requirements

### Requirement: Material 3 BottomNavigationView

应用应使用 Material 3 BottomNavigationView 替换自定义底部导航。

#### Scenario: 底部导航组件存在
- **WHEN** MainActivity 布局加载
- **THEN** 使用 com.google.android.material.bottomnavigation.BottomNavigationView 组件

#### Scenario: 导航项配置
- **WHEN** 查看 BottomNavigationView 配置
- **THEN** 包含 Home、WOL、Shell、Remote 四个导航项

#### Scenario: 导航项图标
- **WHEN** 导航项显示
- **THEN** 每个导航项使用 Material Icons 或自定义 vector drawable

#### Scenario: 导航项选中状态
- **WHEN** 用户点击导航项
- **THEN** 该项显示选中状态，其他项显示未选中状态

#### Scenario: 导航切换功能
- **WHEN** 用户选择不同导航项
- **THEN** 对应的 Fragment 正确切换显示

### Requirement: MaterialButton 替换

应用应使用 MaterialButton 替换自定义 drawable 背景的 Button。

#### Scenario: 主要操作按钮
- **WHEN** 界面需要主要操作按钮
- **THEN** 使用 MaterialButton 并应用 Primary 样式

#### Scenario: 次要操作按钮
- **WHEN** 界面需要次要操作按钮
- **THEN** 使用 MaterialButton 并应用 Secondary 样式

#### Scenario: 危险操作按钮
- **WHEN** 界面需要危险操作按钮（如删除）
- **THEN** 使用 MaterialButton 并应用 Danger 样式

### Requirement: TextInputLayout 使用

所有输入框应使用 TextInputLayout 包装，提供统一的输入体验。

#### Scenario: 登录输入框
- **WHEN** LoginActivity 显示登录表单
- **THEN** 输入框使用 TextInputLayout 包装，带有 hint 提示

#### Scenario: 设备添加对话框输入框
- **WHEN** 添加 WOL 设备对话框显示
- **THEN** 所有输入框使用 TextInputLayout 包装

#### Scenario: 端口转发配置对话框输入框
- **WHEN** 端口转发配置对话框显示
- **THEN** 所有输入框使用 TextInputLayout 包装

### Requirement: RecyclerView 替换 ListView

RemoteFragment 应使用 RecyclerView 替换 ListView。

#### Scenario: RecyclerView 组件存在
- **WHEN** RemoteFragment 布局加载
- **THEN** 使用 androidx.recyclerview.widget.RecyclerView 组件

#### Scenario: ViewHolder 实现
- **WHEN** RecyclerView Adapter 实现
- **THEN** 使用 ViewBinding 创建 ViewHolder

#### Scenario: 列表性能
- **WHEN** 端口转发列表包含大量项
- **THEN** 滚动流畅，无明显卡顿

### Requirement: CardView 一致样式

所有 CardView 应使用统一的样式定义。

#### Scenario: 卡片圆角一致
- **WHEN** 应用显示卡片
- **THEN** 所有卡片使用相同的圆角半径

#### Scenario: 卡片阴影一致
- **WHEN** 应用显示卡片
- **THEN** 所有卡片使用相同的阴影效果

### Requirement: 对话框 Material Design 风格

所有对话框应使用 Material Design 风格。

#### Scenario: 添加设备对话框
- **WHEN** 用户打开添加设备对话框
- **THEN** 对话框使用 Material 风格，圆角卡片样式

#### Scenario: 端口转发配置对话框
- **WHEN** 用户打开端口转发配置对话框
- **THEN** 对话框使用 Material 风格，圆角卡片样式
