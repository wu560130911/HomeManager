## ADDED Requirements

### Requirement: 提供修饰键

系统 SHALL 提供 CTRL、ALT、FN 修饰键，用于发送组合键。

#### Scenario: 使用 CTRL 修饰键
- **WHEN** 用户点击 CTRL 键后点击 C 键
- **THEN** 发送 Ctrl+C 组合键到远程

#### Scenario: 使用 ALT 修饰键
- **WHEN** 用户点击 ALT 键后点击 D 键
- **THEN** 发送 Alt+D 组合键到远程

#### Scenario: 修饰键锁定模式
- **WHEN** 用户双击 CTRL 键
- **THEN** CTRL 键进入锁定状态
- **AND** 后续按键自动添加 CTRL 修饰
- **AND** 再次点击取消锁定

### Requirement: 提供方向键

系统 SHALL 提供上下左右方向键。

#### Scenario: 使用方向键导航
- **WHEN** 用户点击方向键
- **THEN** 发送对应的方向键序列到远程

#### Scenario: 在 vim 中使用方向键
- **WHEN** 用户在 vim 中点击方向键
- **THEN** vim 正确响应光标移动

### Requirement: 提供控制键

系统 SHALL 提供 ESC、TAB、DEL 等常用控制键。

#### Scenario: 使用 ESC 键
- **WHEN** 用户点击 ESC 键
- **THEN** 发送 ESC 字符到远程

#### Scenario: 使用 TAB 键
- **WHEN** 用户点击 TAB 键
- **THEN** 发送 Tab 字符到远程
- **AND** 远程 shell 正确响应自动补全

#### Scenario: 使用 DEL 键
- **WHEN** 用户点击 DEL 键
- **THEN** 发送删除字符到远程

### Requirement: 提供功能键

系统 SHALL 提供 F1-F12 功能键。

#### Scenario: 使用功能键
- **WHEN** 用户点击 F1 键
- **THEN** 发送 F1 功能键序列到远程

#### Scenario: 功能键在 tmux 中的使用
- **WHEN** 用户按下 Ctrl+B 后点击功能键
- **THEN** tmux 正确响应功能键命令

### Requirement: 提供特殊导航键

系统 SHALL 提供 Home、End、PgUp、PgDn、Insert 等特殊导航键。

#### Scenario: 使用 Home 键
- **WHEN** 用户点击 Home 键
- **THEN** 发送 Home 序列到远程
- **AND** 光标移动到行首

#### Scenario: 使用 End 键
- **WHEN** 用户点击 End 键
- **THEN** 发送 End 序列到远程
- **AND** 光标移动到行尾

#### Scenario: 使用翻页键
- **WHEN** 用户点击 PgUp 或 PgDn
- **THEN** 发送对应翻页序列到远程

### Requirement: 支持按键布局切换

系统 SHALL 支持简洁和完整两种按键布局模式。

#### Scenario: 切换到简洁布局
- **WHEN** 用户选择简洁布局
- **THEN** 扩展键盘只显示常用按键
- **AND** 节省屏幕空间

#### Scenario: 切换到完整布局
- **WHEN** 用户选择完整布局
- **THEN** 扩展键盘显示全部功能键
