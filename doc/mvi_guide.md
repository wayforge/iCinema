# 友好的MVI架构模式标准示例

## 概述

MVI（Model-View-Intent）是一种响应式架构模式，它将应用程序的状态变化视为一个单向数据流。本文档展示了如何实现一个友好的MVI架构模式，特别注重组件的职责分离和预览功能的支持。

## MVI架构核心概念

1. **Model (模型)**：代表应用的数据和业务逻辑，以及应用的状态。
2. **View (视图)**：负责展示UI，并将用户的操作转化为意图(Intent)。
3. **Intent (意图)**：代表用户在视图上的操作，例如点击按钮、输入文本等。

## 标准实现

### 1. 定义State

```kotlin
data class UserState(
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

data class User(
    val name: String,
    val email: String
)
```

### 2. 定义Intent

```kotlin
sealed class UserIntent {
    object LoadUser : UserIntent()
    data class UpdateUser(val user: User) : UserIntent()
    object Error : UserIntent()
}
```

### 3. 实现ViewModel

```kotlin
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUser -> loadUser()
            is UserIntent.UpdateUser -> updateUser(intent.user)
            is UserIntent.Error -> _state.value = _state.value.copy(error = "Intent Error")
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val user = userRepository.getUser()
                _state.value = _state.value.copy(loading = false, user = user)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    private fun updateUser(user: User) {
        _state.value = _state.value.copy(user = user)
    }
}
```

### 4. 实现UI组件

```kotlin
// 在实际使用时，UI组件直接与ViewModel集成
@Composable
fun UserScreen(
    viewModel: UserViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    
    UserContent(
        state = state,
        onRetry = { viewModel.handleIntent(UserIntent.LoadUser) },
        modifier = modifier
    )
}

@Composable
private fun UserContent(
    state: UserState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = state.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("重试")
                }
            }
        }
        state.user != null -> {
            Column(modifier = modifier.padding(16.dp)) {
                Text(
                    text = "User: ${state.user.name}",
                    style = MaterialTheme.typography.headlineMedium
                )
                // 显示其他用户信息...
            }
        }
        else -> {
            // 默认空状态
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无数据")
            }
        }
    }
}
```

### 5. 预览实现

```kotlin
// 预览模式下可以直接传递状态
@Preview(showBackground = true)
@Composable
fun UserScreenPreview() {
    val sampleUser = User(
        name = "张三",
        email = "zhangsan@example.com"
    )
    
    UserContent(
        state = UserState(
            loading = false,
            user = sampleUser,
            error = null
        ),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
fun UserScreenLoadingPreview() {
    UserContent(
        state = UserState(
            loading = true,
            user = null,
            error = null
        ),
        onRetry = {}
    )
}

@Preview(showBackground = true)
@Composable
fun UserScreenErrorPreview() {
    UserContent(
        state = UserState(
            loading = false,
            user = null,
            error = "加载失败，请稍后重试"
        ),
        onRetry = {}
    )
}