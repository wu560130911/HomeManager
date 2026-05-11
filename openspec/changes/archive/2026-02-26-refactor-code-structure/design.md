## Context

当前项目的所有 Java 文件都存放在 `com.wms.homemanager` 包中，没有进行任何分包管理。随着功能增加，代码变得难以维护和扩展。

现有文件分类：
- **Activity/Fragment**: MainActivity, LoginActivity, WolFragment, ShellFragment, RemoteFragment
- **Adapter**: LoginListAdapter, PortForwardingAdapter, PortForwardingDialog
- **Service**: SshService, SshForegroundService, App
- **Model**: LoginInfo, PortForwardingConfig
- **Utils**: LoginInfoUtils, PortForwardingUtils, FileUtils

## Goals / Non-Goals

**目标：**
- 建立清晰的包结构，按功能模块或层次分离代码
- 分离 UI 层与业务逻辑层
- 提高代码的可读性、可维护性和可扩展性
- 保持现有功能不变，不引入新的业务逻辑

**非目标：**
- 不添加新的业务功能
- 不改变应用的运行逻辑
- 不修改 AndroidManifest.xml 中的组件声明（因为包名不变，只需更新 import）

## Decisions

### 决策 1：采用分层+模块的包结构

**选择方案：**
```
com.wms.homemanager/
├── ui/                    # UI 层
│   ├── activity/         # Activity
│   ├── fragment/         # Fragment
│   └── adapter/          # Adapter
├── service/              # 服务层
├── model/                # 数据模型层
└── util/                 # 工具类层
```

**理由：**
- 分层结构清晰，易于理解
- 符合 Android 开发的最佳实践
- 便于后续扩展和维护

**备选方案：**
- 按功能模块分包（如 login/, shell/, wol/, remote/）- 优点是业务边界清晰，缺点是跨模块代码难以归类

### 决策 2：保持包名不变

**选择方案：**
保持 `com.wms.homemanager` 作为基础包名，只在其下创建子包。

**理由：**
- 无需修改 AndroidManifest.xml
- 无需修改 build.gradle 中的包名配置
- 对用户无感知

**备选方案：**
- 修改包名 - 需要同时修改多处配置，可能导致构建问题

### 决策 3：创建 common 子包存放核心组件

**选择方案：**
将 App.java 和 MainActivity.java 放入 common/ 子包。

**理由：**
- App 是应用入口，MainActivity 是主界面
- 它们是基础设施，不属于特定业务模块

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 文件移动后 import 引用失效 | 编译失败 | 使用 Android Studio 的 Refactor → Move 功能自动更新引用 |
| 可能遗漏某些文件的引用 | 运行时崩溃 | 移动后进行全面编译测试 |
| 多人协作时容易产生冲突 | 开发效率降低 | 建议单独完成此重构任务 |

## Migration Plan

1. **准备阶段**
   - 备份当前代码
   - 在 IDE 中分析包依赖关系

2. **实施阶段**
   - 创建目标包结构
   - 移动文件到对应包
   - 自动更新 import 语句
   - 手动检查并修复遗漏的引用

3. **验证阶段**
   - 运行 `./gradlew assembleDebug` 验证编译
   - 在模拟器或真机上测试所有功能

4. **清理阶段**
   - 删除空包（如果存在）
   - 提交代码变更

## Open Questions

- 是否需要为每个模块创建独立的包？（如 login/, shell/, wol/, remote/）
  - 当前建议：暂时不分到模块级，先用分层结构
  - 后续可根据复杂度决定是否进一步拆分
