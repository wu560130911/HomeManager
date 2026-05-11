## Why

当前项目的代码结构较为混乱，所有 Java 文件都堆放在同一个包 `com.wms.homemanager` 中，没有按照功能模块或业务逻辑进行合理分层。这导致代码难以维护、扩展和理解。需要对项目进行重构，建立清晰的代码组织结构。

## What Changes

- 将现有代码按照功能模块进行拆分，形成独立的包结构
- 分离 UI 层（Activity/Fragment）与业务逻辑层（Service/Utils）
- 创建统一的模型层，管理数据模型
- 将工具类按照功能类型进行分类管理
- 建立清晰的依赖关系：UI → 业务 → 数据

## Capabilities

### New Capabilities
- `code-structure-refactor`: 重构代码组织结构，建立基于功能模块的包结构

### Modified Capabilities
- （无）此为纯重构变更，不涉及功能需求变更

## Impact

- 源代码目录结构：`app/src/main/java/com/wms/homemanager/`
- 依赖关系：无新增外部依赖
- 受影响的文件：所有 Java 源文件需要移动到新的包结构中
