## Context

当前 HomeManager 是一款 Android 平台的 SSH 远程管理工具，采用 Material Design 科技风主题。现有终端功能基于简单的 EditText + TextView 实现，仅支持单次命令执行，无法运行交互式程序。

**技术约束**：
- 必须完全自研终端模拟器，不依赖第三方库
- 需要与现有科技风 UI 风格保持一致
- 基于现有 JSch SSH 库进行扩展
- 目标平台：Android 5.0+ (API 21+)

**利益相关者**：
- 终端用户：需要流畅的远程操作体验
- 开发者：需要可维护的代码架构

## Goals / Non-Goals

**Goals:**
- 实现完整的交互式终端模拟器，支持 vim、top、tmux 等交互式程序
- 自研 ANSI 转义序列解析引擎，支持 256 色和真彩色渲染
- 提供类似 Termux/JuiceSSH 的用户体验
- 支持多会话管理和主题定制
- 保持代码可维护性和可扩展性

**Non-Goals:**
- 不支持 X11 图形转发
- 不支持串口终端（Serial Terminal）
- 不支持 Telnet 协议（仅 SSH）
- 不支持插件系统

## Decisions

### 1. 终端架构设计

**决策**：采用分层架构，将终端模拟器分为四层

```
┌─────────────────────────────────────┐
│         TerminalView (UI层)          │  - 自定义 View，处理触摸事件和渲染
├─────────────────────────────────────┤
│      TerminalEmulator (模拟层)       │  - 状态机，管理终端状态
├─────────────────────────────────────┤
│       AnsiParser (解析层)            │  - 解析 ANSI 转义序列
├─────────────────────────────────────┤
│      TerminalBuffer (缓冲层)         │  - 管理屏幕缓冲区和滚动历史
└─────────────────────────────────────┘
```

**备选方案**：
- A) 单一视图方案：所有逻辑在 View 中处理 → 可维护性差
- B) MVP 架构：引入 Presenter 层 → 过度设计，终端是实时交互场景

**理由**：分层架构职责清晰，便于独立测试和维护，符合单一职责原则。

### 2. ANSI 解析器实现

**决策**：采用状态机模式实现 ANSI 解析器

**状态定义**：
- `GROUND`: 正常字符输出
- `ESCAPE`: 收到 ESC 字符
- `CSI`: 收到 CSI 序列起始 (ESC[)
- `OSC`: 收到 OSC 序列起始 (ESC])
- `STRING`: 字符串参数收集
- `NUMBER`: 数字参数收集

**理由**：
- 状态机模式能正确处理复杂的嵌套序列
- 易于扩展新的序列类型
- 性能优于正则表达式方案

### 3. 终端缓冲区设计

**决策**：采用双缓冲设计
- 屏幕缓冲区：当前可见区域（rows × cols）
- 滚动缓冲区：历史输出记录（可配置大小，默认 10000 行）

**数据结构**：
```java
class TerminalBuffer {
    char[][] screenBuffer;      // 当前屏幕
    char[][] scrollBuffer;      // 滚动历史
    int scrollBufferHead;       // 环形缓冲区头指针
    int scrollTop;              // 滚动偏移
    int cursorRow, cursorCol;   // 光标位置
}
```

**理由**：
- 环形缓冲区内存效率高
- 双缓冲设计便于实现平滑滚动

### 4. 扩展键盘设计

**决策**：自定义 View 实现，支持以下按键组

| 组别 | 按键 |
|------|------|
| 修饰键 | CTRL、ALT、FN |
| 控制键 | ESC、TAB、DEL |
| 方向键 | ↑ ↓ ← → |
| 功能键 | F1-F12 |
| 特殊键 | Home、End、PgUp、PgDn、Insert |

**交互设计**：
- 修饰键支持锁定模式（双击锁定）
- 按键布局可配置（简洁/完整模式）
- 长按显示按键说明

**理由**：参考 Termux 和 JuiceSSH 的成熟设计，符合用户习惯。

### 5. 会话管理设计

**决策**：采用标签页 + 滑动切换模式

**架构**：
```java
class SessionManager {
    List<TerminalSession> sessions;
    int currentSessionIndex;

    void createSession(String name);
    void switchSession(int index);
    void closeSession(int index);
    void renameSession(int index, String name);
}
```

**理由**：
- 标签页模式直观，符合现代终端习惯
- 滑动切换符合移动端交互习惯

### 6. 渲染方案

**决策**：使用 Canvas 直接绘制，避免 TextView 的性能问题

**实现要点**：
- 使用 `Paint.measureText()` 进行字符定位
- 支持等宽字体（Monospace / JetBrains Mono）
- 双缓冲绘制避免闪烁
- 局部重绘优化性能

**备选方案**：
- A) 基于 TextView + Spannable → 性能差，无法处理大量文本
- B) 基于 RecyclerView → 滚动体验不佳，不适合终端场景
- C) 基于 SurfaceView → 过度复杂，普通 Canvas 已足够

**理由**：Canvas 直接绘制是终端模拟器的标准做法，性能最优。

## Risks / Trade-offs

### 风险1：ANSI 解析复杂度
- **风险**：ANSI 标准复杂，私有序列众多，可能无法覆盖所有场景
- **缓解**：优先支持主流序列（xterm 标准），对未知序列采用容错策略

### 风险2：内存占用
- **风险**：大滚动缓冲区可能占用较多内存
- **缓解**：
  - 提供缓冲区大小配置（默认 10000 行）
  - 采用 char[][] 而非 String[][] 存储文本
  - 按需加载颜色属性

### 风险3：输入法兼容性
- **风险**：不同输入法对特殊字符处理不同
- **缓解**：
  - 扩展键盘覆盖常用特殊字符
  - 提供 InputConnection 自定义实现

### 风险4：开发周期
- **风险**：自研方案开发周期较长
- **缓解**：
  - 分阶段交付：核心功能优先，高级特性迭代
  - 模块化设计便于并行开发

## Migration Plan

### 阶段一：核心终端（预计工作量：基础模块）
1. 实现 TerminalBuffer 和 AnsiParser
2. 实现 TerminalView 基础渲染
3. 重构 SshService 支持 Shell 通道

### 阶段二：交互增强（预计工作量：扩展模块）
1. 实现 ExtendedKeyboardView
2. 实现文本选择和复制粘贴
3. 实现手势操作

### 阶段三：高级特性（预计工作量：完善模块）
1. 实现 SessionManager 多会话
2. 实现 TerminalThemes 主题系统
3. 实现 URL 识别和点击

### 回滚策略
- 保留旧版 ShellFragment 代码
- 通过功能开关控制新旧终端切换
- 新终端独立包名，不影响现有功能

## Open Questions

1. **字体选择**：是否需要内置等宽字体，还是依赖系统字体？
   - 建议：默认使用系统 Monospace，提供自定义字体选项

2. **触摸板模式**：是否需要支持触摸板模式（发送鼠标事件到远程）？
   - 建议：暂不支持，作为后续增强

3. **编码支持**：除 UTF-8 外，是否需要支持其他编码（GBK 等）？
   - 建议：优先 UTF-8，其他编码作为可选配置
