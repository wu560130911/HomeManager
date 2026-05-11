## ADDED Requirements

### Requirement: 统一样式系统

应用应具有统一的样式定义系统，所有可复用的样式应定义在 styles.xml 中。

#### Scenario: 样式文件存在
- **WHEN** 项目构建时
- **THEN** res/values/styles.xml 文件存在

#### Scenario: 按钮样式定义
- **WHEN** 开发者需要应用按钮样式
- **THEN** 可使用 Widget.HomeManager.Button.Primary、Secondary、Danger 等样式

#### Scenario: 卡片样式定义
- **WHEN** 开发者需要应用卡片样式
- **THEN** 可使用 Widget.HomeManager.Card 样式

#### Scenario: 输入框样式定义
- **WHEN** 开发者需要应用输入框样式
- **THEN** 可使用 Widget.HomeManager.TextInput 样式

### Requirement: 颜色系统规范

应用应定义统一的颜色资源，避免在布局中使用硬编码颜色值。

#### Scenario: 主题颜色定义
- **WHEN** 项目构建时
- **THEN** res/values/colors.xml 定义 primary、secondary、background、surface 等语义化颜色

#### Scenario: 颜色资源引用
- **WHEN** 布局文件需要使用颜色
- **THEN** 通过 @color/ 资源引用，不使用硬编码十六进制值

### Requirement: 字体样式规范

应用应定义统一的文字外观样式。

#### Scenario: 标题文字样式
- **WHEN** 需要显示标题文字
- **THEN** 使用 TextAppearance.HomeManager.Headline 样式

#### Scenario: 正文文字样式
- **WHEN** 需要显示正文文字
- **THEN** 使用 TextAppearance.HomeManager.Body 样式

#### Scenario: 说明文字样式
- **WHEN** 需要显示说明文字
- **THEN** 使用 TextAppearance.HomeManager.Caption 样式

### Requirement: 间距规范

应用应定义统一的间距资源。

#### Scenario: 间距资源定义
- **WHEN** 项目构建时
- **THEN** res/values/dimens.xml 定义标准间距值（如 margin_small、margin_medium、margin_large）

#### Scenario: 间距资源引用
- **WHEN** 布局文件需要设置间距
- **THEN** 通过 @dimen/ 资源引用标准间距值
