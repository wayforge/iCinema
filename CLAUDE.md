# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

- 构建 Debug APK：`./gradlew :app:assembleDebug`
- 安装到已连接设备：`./gradlew :app:installDebug`
- 运行 Lint：`./gradlew :app:lint`
- 运行全部单元测试：`./gradlew :app:testDebugUnitTest`
- 运行全部仪器测试（需设备/模拟器）：`./gradlew :app:connectedDebugAndroidTest`
- 运行单个单元测试类：`./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest"`
- 运行单个单元测试方法：`./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest.addition_isCorrect"`
- 运行单个仪器测试方法：`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.icinema.ExampleInstrumentedTest#useAppContext`

## 关键配置

- CMS API 基地址在 `app/src/main/java/com/icinema/di/ICinemaModule.kt` 的 `BASE_URL` 常量。
- 目前 `AndroidManifest.xml` 开启了 `usesCleartextTraffic=true`；涉及网络策略改动时要同步检查。

## 高层架构

这是一个单模块 Android 项目（仅 `:app`），技术栈为 Compose + Hilt + Retrofit + Room + Media3。

### 分层与依赖方向

- `data`：
  - `api/`：`CmsApiService` 对接苹果 CMS 接口。
  - `repository/`：`CmsRepositoryImpl` 统一处理分类、列表、详情、搜索。
  - `local/`：Room（分类缓存 + 播放进度）。
  - `mappers/`：API/Data/Domain 模型转换。
- `domain`：核心业务模型（`Video`、`PlaySource`、`PlayableEpisode` 等）。
- `pages`：按页面组织的 UI + 状态流转（home/detail/player）。
- `di`：应用级依赖注入（Retrofit/Repository/Room）。

依赖方向保持为：`pages -> (BizPort) -> repository -> api/local`，UI 不直接访问 Retrofit/Room。

### 页面状态管理模式（Home/Detail/Player 共用）

每个页面基本都采用同一套结构：

- `*Contract`：定义 `UiState / UiIntent / UiEffect / Mutation`
- `*ViewModel`：接收 intent、调用 `BizPort`、提交 mutation
- `*Reducer`：纯函数归并 state
- `*BizPort` + `*BindingsModule`：页面级业务端口与 Hilt 绑定

这是该仓库最重要的协作约定；新增页面或改复杂交互时优先复用此模式。

### 播放链路

- 入口：`PlayerActivity` + `PlayerViewModel`
- 播放器：`ExoPlayer`（Media3）
- 能力：选源/选集、进度持久化、下一集预加载
- 播放进度通过 `PlaybackHistoryDao` 持久化；切换源/集与生命周期事件会触发读写。

### 数据链路

- 首页加载分类时优先读本地 Room 分类缓存，再回源 CMS。
- 视频列表/详情/搜索走统一 CMS 接口（`vodDetail`），由 repository 分发不同调用方式。
- Domain 模型是 UI 层唯一消费模型，避免 UI 直接依赖 API 返回结构。