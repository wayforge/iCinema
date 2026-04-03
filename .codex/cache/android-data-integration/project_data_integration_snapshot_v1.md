# Project Data Integration Snapshot

## Summary

- 当前项目已经具备统一的真实数据接入主链路：`ViewModel -> BizPort -> Repository-backed BizPort -> ICmsRepository -> CmsRepositoryImpl -> CmsApiService`。
- `home` 和 `detail` 两个 feature 目前都属于 `real`，不再是 UI 直连 repository。
- `detail` 的 fake 边界保留在 `FakeDetailBizPort`，但运行绑定已经切到 `RepositoryDetailBizPort`，因此当前线上链路是实数据。
- 当前仓库的关键不是底层用的是 Retrofit 还是 OkHttp，而是逻辑 endpoint `ac=detail` 被同一个接口以不同参数模式复用：
  - 列表：`pg` / `t`
  - 搜索：`wd` / `pg`
  - 详情：`ids`

## How To Add A New Real Endpoint Here

1. 先确认目标 feature 是否已有 `BizPort`。
2. 如果已有 `BizPort`，优先把真实请求收口到 repository-backed 实现，不把网络细节回塞进 ViewModel。
3. 新请求优先复用 `CmsApiService` 的逻辑 endpoint 模式；若必须新增接口，也保持 `Repository -> mapper -> domain -> BizPort` 这条路径不变。
4. 失败处理延续当前仓库规则：
   - `response.code == 1` 视为成功
   - `detail` 模式额外要求 `response.list` 非空
   - 其他情况转 `Result.failure(Exception(msg ?: 默认文案))`

## Verified Facts

- 分类读取路径：`HomeViewModel -> HomeBizPort -> RepositoryHomeBizPort -> ICmsRepository.getCategoryList() -> CmsRepositoryImpl -> CmsApiService.categoryList()`
- 详情读取路径：`DetailViewModel -> DetailBizPort -> RepositoryDetailBizPort -> ICmsRepository.getVideoDetail(videoId) -> CmsRepositoryImpl -> CmsApiService.vodDetail(ids=...)`
- 缓存只用于分类：`CategoryDao`
- DTO 到 domain 的主要映射是 `toData / toDomain`
- 依赖注入由 `ICinemaModule` 提供 repository，由 feature 级 bindings module 绑定 `BizPort`

## Detail Runtime Evidence

- 构建安装通过：`:app:compileDebugKotlin`, `:app:installDebug`
- 主页点入详情后，真实日志出现：
  - `GET https://caiji.dyttzyapi.com/api.php/provide/vod/?ac=detail&ids=12404&pg=1&limit=5`
- 详情页已渲染出：
  - 标题
  - 海报
  - 播放源切换
  - 分段选集
  - 剧集标签
  - 简介信息
