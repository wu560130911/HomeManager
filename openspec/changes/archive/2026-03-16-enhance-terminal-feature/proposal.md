## Why

当前 HomeManager 的终端功能较为基础，仅支持单次命令执行，不支持交互式命令（如 vim、top、tmux）、ANSI 颜色渲染、特殊字符输入等业界终端的标准功能。这使得用户在远程服务器上进行复杂操作时体验受限。为提升产品竞争力，需要对标业界标杆（Termux、JuiceSSH 等）对终端功能进行全面升级。

**技术决策**：基于此前集成第三方 TerminalEmulator 库失败的经验，本次采用完全自研方案，确保可控性和稳定性。

## What Changes

### 核心能力升级
- **交互式终端支持**：从单次命令执行升级为真正的 PTY 交互式终端，支持 vim、top、tmux 等交互式程序
- **ANSI 颜色渲染**：自研 ANSI 转义序列解析引擎，支持 256 色和真彩色终端，正确渲染命令输出中的颜色代码
- **特殊字符键盘**：自研虚拟扩展键盘组件，支持 CTRL、ALT、ESC、TAB、方向键、功能键等

### 会话管理
- **多会话支持**：支持同时开启多个终端会话，可在会话间快速切换
- **会话持久化**：支持会话命名、保存命令历史到本地

### 交互体验
- **文本选择与复制**：支持长按选择文本、复制粘贴操作
- **URL 识别**：自动识别终端输出中的 URL，点击可调用浏览器打开
- **手势操作**：支持滑动切换会话、双指缩放调整字体大小

### 个性化设置
- **主题配色**：提供多种预设主题，支持自定义终端颜色
- **字体设置**：支持调整字体大小、选择等宽字体

## Capabilities

### New Capabilities

- `interactive-terminal`: 交互式终端核心功能，包括 PTY 会话管理、输入输出流处理、信号处理（Ctrl+C 等）
- `ansi-parser`: ANSI 转义序列解析引擎，自研实现 CSI、OSC、SGR 等序列解析，支持 256 色和真彩色
- `terminal-buffer`: 终端缓冲区管理，包括屏幕缓冲区、滚动缓冲区、光标管理
- `extended-keyboard`: 虚拟扩展键盘组件，支持 CTRL、ALT、ESC、TAB、方向键、功能键等特殊字符输入
- `session-manager`: 终端会话管理，支持多会话创建、切换、命名、关闭
- `terminal-themes`: 终端主题系统，支持预设主题切换和自定义颜色配置

### Modified Capabilities

- 无（本次为新增功能，不影响现有规格）

## Impact

### 受影响的代码模块
- `ShellFragment.java`: 需要重构为基于自研终端模拟器的实现
- `SshService.java`: 需要扩展支持 shell 通道的交互式输入输出
- `fragment_shell.xml`: 需要重新设计布局以支持扩展键盘和会话管理

### 新增代码模块（全部自研）
- `terminal/` 包：
  - `TerminalView.java`: 自定义终端显示视图
  - `TerminalBuffer.java`: 终端缓冲区管理
  - `TerminalSession.java`: 终端会话封装
  - `AnsiParser.java`: ANSI 转义序列解析器
  - `TerminalEmulator.java`: 终端模拟器核心逻辑
  - `TerminalKeyListener.java`: 键盘事件监听处理
  - `ExtendedKeyboardView.java`: 扩展键盘视图

### 系统影响
- 需要处理更多的输入输出流，内存占用可能增加
- 需要考虑长时间运行的交互式会话的生命周期管理
- 自研方案开发周期较长，但可控性和稳定性更高
