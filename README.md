# iCinema - 苹果CMS V10 Android客户端

这是一个基于Jetpack Compose和MVVM架构的Android视频播放器应用，对接苹果CMS V10 API。

## 功能特性

- 视频列表浏览和搜索
- 视频详情展示
- 支持多播放源和多剧集选择
- 使用Media3 (ExoPlayer) 进行视频播放
- 分页加载和下拉刷新
- 搜索功能

## 技术栈

- **UI框架**: Jetpack Compose
- **架构模式**: MVVM + Clean Architecture
- **网络请求**: Retrofit + OkHttp
- **视频播放**: Media3 ExoPlayer
- **状态管理**: StateFlow
- **图片加载**: Coil
- **导航**: Navigation Compose

## 项目结构

```
app/src/main/java/com/icinema/
├── data/                    # 数据层
│   ├── api/                # API接口和映射
│   ├── model/              # 数据模型
│   └── repository/         # Repository实现
├── domain/                  # 领域层
│   ├── model/              # 领域模型
│   ├── repository/         # Repository接口
│   └── usecase/            # 用例
├── presentation/            # 表示层
│   ├── components/         # 通用UI组件
│   ├── detail/             # 视频详情页面
│   ├── home/               # 首页
│   ├── navigation/         # 导航配置
│   ├── player/             # 视频播放器
│   └── theme/              # 主题配置
└── di/                     # 依赖注入
```

## 配置说明

### 1. 配置CMS API地址

修改 `app/src/main/java/com/icinema/di/AppModule.kt` 文件中的 `BASE_URL`：

```kotlin
private const val BASE_URL = "https://your-cms-domain.com/"
```

### 2. 确保CMS后台配置

在苹果CMS V10后台确保：
1. 已开启开放API
2. 正确配置图片域名
3. API接口地址应为: `你的域名/api.php/provide/vod/`

### 3. 运行项目

```bash
# 调试构建
./gradlew assembleDebug

# 运行测试
./gradlew test

# 代码检查
./gradlew lint
```

## API接口

应用使用了以下CMS V10 API接口：

- `?ac=list` - 获取视频列表（支持分页、分类、搜索）
- `?ac=detail&id=` - 获取视频详情

## 依赖版本

- Kotlin: 2.0.21
- Compose BOM: 2024.09.00
- Media3: 1.2.0
- Retrofit: 2.9.0
- OkHttp: 4.12.0

## 许可证

MIT License
# iCinema
