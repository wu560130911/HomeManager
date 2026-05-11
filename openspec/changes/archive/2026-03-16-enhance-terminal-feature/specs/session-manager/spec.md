## ADDED Requirements

### Requirement: 创建多个终端会话

系统 SHALL 支持同时创建和管理多个独立的终端会话。

#### Scenario: 创建新会话
- **WHEN** 用户点击新建会话按钮
- **THEN** 创建一个新的终端会话
- **AND** 自动切换到新会话

#### Scenario: 多会话独立运行
- **WHEN** 存在多个会话
- **THEN** 每个会话独立运行，互不干扰

#### Scenario: 会话数量限制
- **WHEN** 会话数量达到上限（默认5个）
- **THEN** 提示用户关闭部分会话后再创建

### Requirement: 切换终端会话

系统 SHALL 支持在不同会话之间快速切换。

#### Scenario: 点击切换会话
- **WHEN** 用户点击会话标签
- **THEN** 立即切换到对应会话
- **AND** 显示该会话的终端内容

#### Scenario: 滑动切换会话
- **WHEN** 用户在终端区域左右滑动
- **THEN** 切换到相邻的会话

### Requirement: 关闭终端会话

系统 SHALL 支持关闭指定的终端会话。

#### Scenario: 关闭会话
- **WHEN** 用户点击关闭按钮
- **THEN** 提示确认是否关闭
- **AND** 确认后关闭会话并释放资源

#### Scenario: 关闭最后一个会话
- **WHEN** 用户尝试关闭最后一个会话
- **THEN** 提示确认是否退出终端功能

### Requirement: 重命名会话

系统 SHALL 支持为会话设置自定义名称。

#### Scenario: 设置会话名称
- **WHEN** 用户长按会话标签并输入新名称
- **THEN** 会话名称更新为自定义名称

#### Scenario: 默认会话名称
- **WHEN** 创建新会话
- **THEN** 默认名称为 "Terminal #N"（N 为序号）

### Requirement: 会话状态显示

系统 SHALL 显示每个会话的当前状态。

#### Scenario: 显示连接状态
- **WHEN** 会话正在连接
- **THEN** 标签显示连接中状态指示器

#### Scenario: 显示断开状态
- **WHEN** 会话连接断开
- **THEN** 标签显示断开状态指示器

### Requirement: 命令历史持久化

系统 SHALL 将命令历史保存到本地，支持跨会话使用。

#### Scenario: 保存命令历史
- **WHEN** 用户执行命令
- **THEN** 命令保存到历史记录

#### Scenario: 查看历史命令
- **WHEN** 用户按上方向键或打开历史面板
- **THEN** 显示历史命令列表

#### Scenario: 跨会话使用历史
- **WHEN** 用户新建会话
- **THEN** 可以访问之前会话的命令历史
