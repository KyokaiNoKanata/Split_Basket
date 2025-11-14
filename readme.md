#### 源代码 (app/src/main/java/com/example/split\_basket/)

**核心Activity类：**

1. **HomeActivity.java** - 主页面

   * 应用入口点（LAUNCHER Activity）
   * 包含四个功能卡片：快速添加、概览、新建清单、新建账单
   * 底部导航栏管理
   * 平滑的页面切换动画效果

2. **InventoryActivity.java** - 库存管理页面

   * 物品分类管理（全部、蔬菜、肉类、水果、其他）
   * 手动添加和从购物清单导入功能
   * 库存概览统计（剩余数量、即将过期、已消耗）
   * 物品列表展示（名称、购买日期、数量）

3. **ListActivity.java** - 购物清单页面

   * 购物清单管理
   * 添加物品和结算功能
   * 底部导航集成

4. **BillActivity.java** - 账单分摊页面

   * 账单创建和管理
   * 多种分摊方式：均等、按数量、按物品、自定义
   * 近期账单展示（未付/已付状态）
   * 成员管理功能

#### 资源文件 (app/src/main/res/)

**布局文件 (layout/):**

* **activity\_home.xml** - 主页布局，包含功能卡片网格
* **activity\_inventory.xml** - 库存页面布局，包含分类筛选和物品表格
* **activity\_list.xml** - 购物清单布局
* **activity\_bill.xml** - 账单页面布局，包含分摊选项表单

**值资源 (values/):**

* **strings.xml** - 所有文本字符串资源（91个字符串）
* **colors.xml** - 颜色定义，包含Material Design配色和自定义颜色
* **styles.xml** - 样式定义：页面标题、按钮、单选按钮、列表项、底部导航
* **themes.xml** - 应用主题配置，基于Material Components

**Drawable资源:**

* 底部导航图标：ic\_home.xml, ic\_inventory.xml, ic\_list.xml, ic\_bill.xml
* 按钮状态选择器：button\_background\_selector.xml
* 项目符号图标：ic\_item\_bullet.xml

**动画资源:**

* 按钮淡入淡出动画：button\_fade\_animation.xml
* 底部导航状态动画

#### 清单文件

* **AndroidManifest.xml** - 应用配置

  * 四个Activity声明，HomeActivity为主入口
  * 支持RTL、全屏显示优化
  * 数据备份规则配置
