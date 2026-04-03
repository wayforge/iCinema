# Room Migration Notes

## 核心区别

- `Room` 依赖版本：决定使用哪版 Room API 和编译器。
- `AppDatabase.version`：决定本地 SQLite schema 版本。
- 升级 Room 依赖不能代替数据库 schema 迁移。

## 当前项目策略

- 当前数据库版本：`2`
- 已开启 `exportSchema = true`
- 已配置 schema 导出目录：`app/schemas`
- 当前使用：`fallbackToDestructiveMigration()`

## 为什么当前不保留 `migration1To2`

- 当前仍处于开发阶段。
- 新增的 `playback_history` 表没有必须保留的线上历史数据。
- 开发期优先降低 schema 迭代成本，版本变化时允许删库重建。

## 当前结论

- 开发阶段：
  使用 `fallbackToDestructiveMigration()`，避免每次 schema 变更都手写 migration。
- 稳定阶段或发布前：
  去掉 destructive migration，改为正式迁移方案。

## 后续推荐

- 简单 schema 变更：优先考虑 `AutoMigration`
- 复杂 schema 变更：手写 `Migration`
- 只修改 Room 库版本，不能解决旧数据库升级问题

## 适用边界

- 只要需要保留用户本地数据，就不能长期依赖 destructive migration。
- 一旦播放记录、缓存索引或业务数据需要保留，必须切回正式 migration 策略。
