## ADDED Requirements

### Requirement: ViewBinding 功能启用

项目应启用 ViewBinding 功能以提供类型安全的视图访问。

#### Scenario: Gradle 配置
- **WHEN** 查看 app/build.gradle 文件
- **THEN** buildFeatures.viewBinding 设置为 true

#### Scenario: 绑定类自动生成
- **WHEN** 项目编译成功
- **THEN** 为每个布局文件生成对应的绑定类（如 ActivityMainBinding、FragmentWolBinding）

### Requirement: Activity 使用 ViewBinding

所有 Activity 应使用 ViewBinding 访问视图，不使用 findViewById。

#### Scenario: MainActivity ViewBinding 集成
- **WHEN** MainActivity 初始化视图
- **THEN** 使用 ActivityMainBinding 绑定类访问视图

#### Scenario: LoginActivity ViewBinding 集成
- **WHEN** LoginActivity 初始化视图
- **THEN** 使用 ActivityLoginBinding 绑定类访问视图

### Requirement: Fragment 使用 ViewBinding

所有 Fragment 应使用 ViewBinding 访问视图，不使用 findViewById。

#### Scenario: WolFragment ViewBinding 集成
- **WHEN** WolFragment 初始化视图
- **THEN** 使用 FragmentWolBinding 或 ActivityWolBinding 绑定类访问视图

#### Scenario: ShellFragment ViewBinding 集成
- **WHEN** ShellFragment 初始化视图
- **THEN** 使用 FragmentShellBinding 或 ActivityShellBinding 绑定类访问视图

#### Scenario: RemoteFragment ViewBinding 集成
- **WHEN** RemoteFragment 初始化视图
- **THEN** 使用 FragmentRemoteBinding 绑定类访问视图

### Requirement: Adapter 使用 ViewBinding

所有 RecyclerView Adapter 应使用 ViewBinding 创建和绑定视图。

#### Scenario: Adapter ViewHolder 绑定
- **WHEN** Adapter 创建 ViewHolder
- **THEN** 使用对应的 ItemBinding 类（如 ItemLoginInfoBinding、ItemPortForwardingBinding）

### Requirement: ViewBinding 内存管理

Fragment 中应正确管理 ViewBinding 的生命周期，避免内存泄漏。

#### Scenario: Fragment 视图销毁时释放绑定
- **WHEN** Fragment 的 onDestroyView 被调用
- **THEN** 绑定实例应设置为 null
