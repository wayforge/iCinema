# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

- 构建 Debug APK：`./gradlew :app:assembleDebug`
- 安装到已连接设备：`./gradlew :app:installDebug`
- 运行 Lint：`./gradlew :app:lint`
- 运行全部单元测试：`./gradlew :app:testDebugUnitTest`
- 运行单个单元测试类：`./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest"`
- 运行单个单元测试方法：`./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest.addition_isCorrect"`
- 运行全部仪器测试（需设备/模拟器）：`./gradlew :app:connectedDebugAndroidTest`
- 运行单个仪器测试类：`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.icinema.ExampleInstrumentedTest`
- 运行单个仪器测试方法：`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.icinema.ExampleInstrumentedTest#useAppContext`

## 关键配置

- 这是单模块工程，仅包含 `:app`。
- CMS API 基地址在 `app/src/main/java/com/icinema/di/ICinemaModule.kt` 的 `BASE_URL` 常量。
- `app/src/main/AndroidManifest.xml` 当前开启了 `usesCleartextTraffic=true`；涉及网络策略或接口域名调整时要一起检查。
- Room schema 导出目录是 `app/schemas/com.icinema.data.local.AppDatabase/`。
- `DatabaseModule` 当前使用 `fallbackToDestructiveMigration()`；修改 Room 表结构时默认允许开发期删库重建，不应假设本地数据会被保留。

## 高层架构

这是一个单模块 Android 项目，当前实际技术栈是 Compose + Hilt + Retrofit + Room + Media3。应用采用多 Activity 组织页面，页面跳转通过显式 `Activity` 跳转完成，不存在集中式 Navigation Compose 路由图。

### 代码组织

- `data/`
  - `api/`：苹果 CMS 接口定义。
  - `local/`：Room 数据库、DAO、Entity。
  - `mappers/`：API/Data/Domain 转换。
  - `model/`：接口与数据层模型。
  - `repository/`：`ICmsRepository` 与 `CmsRepositoryImpl`，统一收口远端与本地数据访问。
- `domain/`
  - `model/`：UI 消费的核心领域模型，如 `Video`、`PlaySource`、`PlayableEpisode`。
- `pages/`
  - 按功能页面分包，主要包括 `home`、`detail`、`player`、`category`、`history`、`favorite`。
- `di/`：Hilt 应用级依赖注入。
- `ui/theme/`、`util/`：主题与通用工具。

### 页面协作约定

仓库内页面普遍使用同一套页面级状态模式：

- `*Contract`：定义 `UiState / UiIntent / UiEffect / Mutation`
- `*ViewModel`：接收 intent、调用 `BizPort`、提交 mutation
- `*Reducer`：纯函数归并 state
- `*BizPort` + `*BindingsModule`：页面业务端口与 Hilt 绑定

依赖方向保持为：`Activity/Composable -> ViewModel -> BizPort -> ICmsRepository -> api/local`。
UI 层不要直接访问 Retrofit、Room DAO 或 API model。

### 数据链路

- `CmsRepositoryImpl` 是主要数据收口层，除分类、列表、详情、搜索外，也承接观看历史、收藏、搜索历史、推荐、会话信息和下载任务等本地能力。
- 首页分类加载优先读取本地 Room 分类缓存，再回源 CMS。
- 首页“可见分类”不是单纯以后端返回为准，而是由 `pages/category/CategorySelectionStore` 用 SharedPreferences 持久化；分类编辑保持为独立 `pages/category` 模块，Home 仅负责跳转并在返回后刷新。
- Domain 模型是页面层唯一稳定消费模型，避免在 UI 层直接传播 API 返回结构。

### 本地存储

- Room 当前包含 5 类实体：分类缓存、播放进度、收藏、搜索历史、下载任务。
- 播放进度按 `videoId + sourceKey + episodeIndex` 维度持久化，用于继续观看与播放器恢复。
- 播放器设置（倍速、自动连播、手势快进）存储在 SharedPreferences，与播放进度分开管理。

### 页面职责

- `home`：首页列表、分类筛选、搜索、继续观看、搜索建议、推荐内容与分页加载。
- `detail`：视频详情、播放源/选集切换、收藏状态。
- `player`：Media3 播放、进度恢复、自动连播、下一集预加载、播放器设置。
- `category`：首页可见分类编辑。
- `history` / `favorite`：观看历史与收藏列表。

### 播放链路

- 入口是 `PlayerActivity` + `PlayerViewModel`。
- `PlayerViewModel` 持有 `ExoPlayer`，通过 `PlaybackMediaSourceFactory` 与 `PlayerPreloadCoordinator` 组织播放与预加载。
- 播放加载时会从 `Video` 解析出播放源与剧集，优先选择可用 HLS 源；当前版本实际只支持 HLS 播放。
- 进入播放器时会尝试恢复历史进度；如果进度过短或已接近播放完成，则不会提示恢复。
- 播放结束会清理当前集进度，并在开启自动连播时切到下一集。

### 现状说明

- 仓库当前测试文件很少，`app/src/test` 与 `app/src/androidTest` 基本仍是模板示例；修改 UI、播放器或数据集成时，不要假设已有完善自动化覆盖。
- README 中关于 `presentation/`、`Navigation Compose`、`di/AppModule.kt` 等描述已过时，新增工作应以当前实际包结构和实现为准。
- 仓库内存在部分占位实现：例如首页/详情有 `Fake*BizPort` 样例数据，会话登录目前是本地 mock 持久化，不要误认为已接入真实鉴权后端。
