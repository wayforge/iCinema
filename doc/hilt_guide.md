# Android Hilt 依赖注入框架官方文档介绍

## 什么是Hilt？

Hilt是Android的依赖注入库，可减少在项目中执行手动依赖项注入的样板代码。Hilt在热门DI库Dagger的基础上构建而成，因而能够受益于Dagger的编译时正确性、运行时性能、可伸缩性和Android Studio支持。

## Hilt的基本概念

### 1. 核心注解

- `@HiltAndroidApp`: 必须在Application类上添加此注解，它会触发Hilt的代码生成操作
- `@AndroidEntryPoint`: 用于标记需要依赖注入的Android组件（Activity、Fragment、Service等）
- `@Module`: 定义提供依赖项的模块
- `@InstallIn`: 指定模块安装的组件范围
- `@Provides`: 在模块中提供依赖项的方法
- `@Inject`: 标记需要注入的字段或构造函数

### 2. Hilt组件生命周期

Hilt提供多个预定义的组件，对应不同的Android生命周期：

- `SingletonComponent`: 应用生命周期
- `ActivityRetainedComponent`: Activity保留的生命周期
- `ActivityComponent`: 单个Activity生命周期
- `FragmentComponent`: Fragment生命周期
- `ViewComponent`: View生命周期
- `ServiceComponent`: Service生命周期

## 配置Hilt

### 1. 添加依赖项

在项目级别的`build.gradle`文件中添加插件：

```gradle
plugins {
    id 'com.google.dagger.hilt.android' version '2.51.1' apply false
}
```

在模块级别的`build.gradle`文件中启用Hilt：

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.dagger.hilt.android'
}

android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'com.google.dagger:hilt-android:2.51.1'
    kapt 'com.google.dagger:hilt-compiler:2.51.1'
}
```

### 2. 初始化Hilt

创建带有`@HiltAndroidApp`注解的Application类：

```kotlin
@HiltAndroidApp
class MyApplication : Application() {
    // 应用程序代码
}
```

在AndroidManifest.xml中声明此应用类：

```xml
<application
    android:name=".MyApplication"
    ... >
    <!-- 其他配置 -->
</application>
```

## Hilt使用示例

### 1. 定义依赖模块

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
```

### 2. 构造函数注入

对于自己的类，可以直接在构造函数上使用`@Inject`：

```kotlin
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase
) {
    suspend fun getUsers(): List<User> {
        return apiService.getUsers()
    }
}
```

### 3. 字段注入

在Activity或Fragment中注入依赖：

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 使用注入的依赖
        lifecycleScope.launch {
            val users = userRepository.getUsers()
            // 处理结果
        }
    }
}
```

## Hilt最佳实践

### 1. 作用域注解

为特定类型定义作用域：

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DatabaseInfo

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    @DatabaseInfo
    fun provideDatabaseUrl(): String {
        return "jdbc:mysql://localhost:3306/mydb"
    }
}
```

### 2. 接口绑定

当注入接口类型时，需要绑定具体实现：

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}
```

### 3. ViewModel注入

配合Jetpack ViewModel使用：

```kotlin
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users
    
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _users.value = userRepository.getUsers()
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }
}
```

在Activity中使用：

```kotlin
@AndroidEntryPoint
class UserActivity : AppCompatActivity() {
    
    private lateinit var userViewModel: UserViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        
        userViewModel = hiltViewModel()
    }
}
```

## Hilt与其他Android组件集成

### 1. Fragment中的依赖注入

```kotlin
@AndroidEntryPoint
class UserFragment : Fragment() {
    
    @Inject
    lateinit var userRepository: UserRepository
    
    private val userViewModel: UserViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 使用注入的依赖
    }
}
```

### 2. Worker注入

```kotlin
@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            userRepository.syncUserData()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

Hilt为Android开发提供了一套完整的依赖注入解决方案，它与Android框架深度集成，简化了依赖管理，提高了代码的可测试性和可维护性。