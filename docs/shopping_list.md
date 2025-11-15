# 购物清单（Shopping List）模块说明（中文）

本文档面向开发者，介绍购物清单模块的操作流程、功能点、实现机制，以及最重要的数据库（Room）规范，方便后续模块（如库存、账单等）共享数据约定与复用能力。

## 功能概览

- 添加物品：输入“名称/数量/添加人”，保存为待购买项。
- 推荐快捷添加：根据当前清单中出现频率较高的物品，显示一行推荐 Chip；点击可直接带入名称至添加对话框。
- 勾选购买：勾选后标记为“已购”，列表按“未购在前、已购在后”排序展示。
- 批量结算：弹窗预览未购条目，确认后一次性标记为已购（批量更新）。
- 编辑/删除：长按条目可编辑；左右滑动可删除，支持 Snackbar 撤销。
- 空态与汇总：空列表提示；顶部展示“Total / Purchased”汇总统计。

## 操作路径（用户视角）

1. 点击“Add Items”打开添加对话框，填入：
    - 名称（必填）
    - 数量（默认 1）
    - 添加人（默认 “You”）
2. 点击推荐 Chip（如 Bread/Tissue/Eggs 或动态热度）可直接打开添加对话框，并已填好名称。
3. 列表行：
    - 复选框：标记已购/未购
    - 长按：编辑名称/数量/添加人
    - 左右滑动：删除（可撤销）
4. 点击“Settlement”：
    - 若还有未购项，弹窗列出剩余物品，确认后批量标记为已购
    - 若无未购项，弹窗提示“全部已购”

## 技术实现（架构与数据流）

- UI 层：`ListActivity` + `RecyclerView`（`ShoppingListAdapter`）
    - 仅列表区域可滚动；顶部标题/推荐和底部操作按钮固定
    - 推荐 Chip 一行显示；根据当前清单频率动态生成，空间不足即停止添加
- 状态/逻辑：`ShoppingListViewModel`
    - 向仓库发起新增、更新、删除、批量更新等操作
    - 暴露 `LiveData<List<ShoppingItem>>` 给界面层观察
- 数据层：`ShoppingListRepository` + `ShoppingListDao` + `SplitBasketDatabase`
    - Room 持久化；所有写操作在后台线程执行
    - 通过 `LiveData` 推送数据变更至界面
- Diff 与稳定 ID：`ShoppingListAdapter` 使用 `ListAdapter + DiffUtil` 与稳定 `getItemId()`，避免全量刷新

## 数据库规范（Room）

实体：`ShoppingItem`（表名：`shopping_items`）

字段定义（与数据库类型对照）：

- `id`：`@PrimaryKey(autoGenerate = true)` → INTEGER PRIMARY KEY AUTOINCREMENT
- `name`：`@ColumnInfo(name = "name") @NonNull` → TEXT NOT NULL
- `addedBy`：`@ColumnInfo(name = "added_by") @NonNull` → TEXT NOT NULL
- `quantity`：`@ColumnInfo(name = "quantity")` → INTEGER（业务保证 ≥ 1）
- `purchased`：`@ColumnInfo(name = "purchased")` → INTEGER(0/1) 作为布尔
- `createdAt`：`@ColumnInfo(name = "created_at")` → INTEGER（epoch millis）
- `inventoryItemId`：`@ColumnInfo(name = "inventory_item_id") @Nullable` → TEXT（可与库存模块关联，尚未强约束外键）

当前查询/写入规范：

- 列表订阅：
  `@Query("SELECT * FROM shopping_items ORDER BY purchased ASC, created_at ASC") LiveData<List<ShoppingItem>>`
    - 未购（purchased = 0）在前，已购在后；同组内按 `created_at` 升序
- 新增：`@Insert(onConflict = REPLACE)`（仓库层做“重复校验”，见下）
- 单项更新：`@Update`
- 批量已购：`@Query("UPDATE shopping_items SET purchased = 1 WHERE id IN (:ids)")`
- 删除全部：`@Query("DELETE FROM shopping_items")`

业务级唯一性策略：

- 允许“不同的人添加同名物品”；阻止“同一人重复添加同名物品”。
- 仓库层校验：`countItemsByNameAndAdder(name, addedBy)`（大小写不敏感）；若 > 0 则提示
  `%1$s by %2$s is already in the list.`
- 如后续需要在数据库层强约束，可新建唯一索引：

```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_items_unique_per_user
ON shopping_items(LOWER(name), LOWER(added_by));
```

推荐索引（查询/排序优化，可按需加入迁移）：

- 频率/查重：`CREATE INDEX IF NOT EXISTS idx_items_name ON shopping_items(LOWER(name));`
- 人员过滤：`CREATE INDEX IF NOT EXISTS idx_items_added_by ON shopping_items(LOWER(added_by));`
- 购买状态与排序：
  `CREATE INDEX IF NOT EXISTS idx_items_purchased_created ON shopping_items(purchased, created_at);`

版本与迁移建议：

- 初始版本（v1）：表结构如上
- 示例迁移（添加唯一索引）：

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_items_unique_per_user ON shopping_items(LOWER(name), LOWER(added_by))")
  }
}
```

> 注意：是否启用唯一索引取决于业务。当前实现为“应用层校验”，以兼容历史数据与更灵活的合并策略。

## 仓库与 DAO 能力清单

- `observeItems(): LiveData<List<ShoppingItem>>`
- `addItem(ShoppingItem, callback)`：后台线程插入；同人同名重复则回调失败
- `updateItem(ShoppingItem)`：更新单行
- `setPurchased(item, boolean)`：通过更新触发；在 VM 内封装
- `markItemsPurchasedByIds(List<Long>)`：批量置已购
- `deleteItem(ShoppingItem)` / `clearAll()`：删除

## 推荐（Recommendation）机制

- 数据来源：当前清单中的“名称”频次（不区分大小写），展示出现次数最多的若干名称。
- 展示规则：只渲染一行 Chip；当下一枚 Chip 宽度将超出可用宽度时停止添加。
- 交互：点击 Chip 打开添加对话框并预填名称，数量和“添加人”可编辑。
- 兜底：若当前没有可统计的候选，则使用 Bread/Tissue/Eggs 三个默认项（同样遵守“一行上限”）。

## 校验与用户体验

- 名称非空校验；数量最小为 1；“添加人”为空则默认 “You”。
- 反馈：新增/结算/删除/撤销等均通过 Toast/Snackbar 提示。
- 稳定刷新：DiffUtil + 稳定 ID，避免闪烁与错位。

## 兼容性与依赖

- 最低支持：API 22（Android 5.1）
- Java 17 编译；Room 2.6.1；Lifecycle 2.8.x；Material 1.13.x

## 与其他模块的对接建议

- 以 `inventory_item_id` 作为软关联字段与“库存模块”对接（字符串 ID）；后续可演进为强外键（需统一主键类型）。
- “账单模块”可复用 `shopping_items` 作为购物来源数据：
    - 依据 `purchased` 和 `created_at` 过滤已购清单生成账目草稿
    - 也可在账单侧保存快照，避免历史数据变动影响已结算账单

## 常见问题（FAQ）

1. 为什么允许不同人添加同名物品？
    - 因多人共同分工，同名物品可能由不同人分别购买或记录，需保留区分信息。
2. 为什么不直接用数据库唯一约束？
    - 当前以应用层校验为主，兼容历史数据与灵活业务；如需严格约束，可按上文添加唯一索引并补齐迁移。
3. 推荐是否会持久化历史？
    - 现阶段只基于“当前清单”频次统计。若需要长期推荐，可新增“添加历史表”供全局统计与搜索。

---

有新的字段或跨模块需求时，请首先更新本规范，并为 Room 提供向后兼容的 Migration，确保已有用户数据安全升级。
