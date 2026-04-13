# iCinema Project Context

## Project Overview
iCinema is an Android video player application built with Jetpack Compose and MVVM architecture. It connects to Apple CMS V10 API to provide video browsing and playback functionality. The app features video list browsing, search capabilities, multi-playback sources, and multi-episode selection with Media3 (ExoPlayer) for video playback.

## Key Technologies
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture + MVI (Model-View-Intent)
- **Network**: Retrofit + OkHttp
- **Video Playback**: Media3 ExoPlayer
- **State Management**: StateFlow/SharedFlow
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **Navigation**: Navigation Compose
- **Local Database**: Room
- **Pagination**: LazyVerticalStaggeredGrid with infinite scrolling
- **Pull-to-Refresh**: Accompanist SwipeRefresh

## Project Structure
```
app/src/main/java/com/icinema/
├── data/                    # Data layer
│   ├── api/                # API interfaces and mappings
│   ├── local/              # Local database entities and DAOs
│   ├── mappers/            # Data mapping functions
│   ├── model/              # Data models
│   └── repository/         # Repository implementations
├── domain/                  # Domain layer
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Use cases
├── pages/                   # Presentation layer
│   ├── category/           # Category browsing page
│   ├── detail/             # Video detail page
│   ├── home/               # Home screen and view model
│   ├── player/             # Video player
│   └── widgets/            # Common UI components
├── di/                     # Dependency injection
├── ui/                     # UI themes and common composables
└── util/                   # Utility functions
```

## Architecture Patterns
- **MVI (Model-View-Intent)**: Used for state management in pages (Home/Detail/Player)
  - `*Contract`: Defines `UiState / UiIntent / UiEffect / Mutation`
  - `*ViewModel`: Receives intents, calls `BizPort`, submits mutations
  - `*Reducer`: Pure function for state merging
  - `*BizPort` + `*BindingsModule`: Page-level business ports with Hilt bindings
- **Clean Architecture**: Separation of concerns with data, domain, and presentation layers
- **MVVM**: ViewModels manage UI-related data and handle business logic
- **Dependency Flow**: `pages -> (BizPort) -> repository -> api/local`

## Key Features
- Video list browsing with category filtering
- Search functionality
- Video detail page with multiple playback sources
- Episode selection for series
- Full-screen video player with source/episode switching
- Pull-to-refresh and infinite scrolling
- Error handling and loading states
- Playback history persistence via Room
- Category caching with Room database

## Configuration
The API base URL is configured in `app/src/main/java/com/icinema/di/ICinemaModule.kt`:
```kotlin
private const val BASE_URL = "https://caiji.dyttzyapi.com/"
```

### CMS Backend Requirements
In Apple CMS V10 backend, ensure:
1. Open API is enabled
2. Image domain is correctly configured
3. API interface URL should be: `your-domain/api.php/provide/vod/`

## Building and Running
```bash
# Debug build
./gradlew assembleDebug

# Install to connected device
./gradlew :app:installDebug

# Run tests
./gradlew test

# Run unit tests (debug)
./gradlew :app:testDebugUnitTest

# Run instrumentation tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest

# Run single unit test class
./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest"

# Run single unit test method
./gradlew :app:testDebugUnitTest --tests "com.icinema.ExampleUnitTest.addition_isCorrect"

# Run single instrumentation test method
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.icinema.ExampleInstrumentedTest#useAppContext

# Code linting
./gradlew :app:lint
```

## Development Conventions
- Uses MVI (Model-View-Intent) pattern with contracts
- Follows Clean Architecture principles
- Uses Hilt for dependency injection
- Implements pagination and pull-to-refresh
- Uses coroutines for asynchronous operations
- Follows Android Jetpack Compose best practices
- Proper error handling with Result wrapper
- State management using StateFlow
- Domain models are the only consumption model for UI layer
- UI should not directly depend on API response structures

## Application Structure
- **ICinemaApp**: Application class with Hilt dependency injection setup
- **HomeActivity**: Main entry point of the application
- **DetailActivity**: Shows video details and player
- **CategoryActivity**: Category browsing page
- **PlayerActivity**: Video playback with ExoPlayer

## API Integration
The app communicates with Apple CMS V10 using these endpoints:
- `?ac=list` - Get video list (with pagination, categories, search)
- `?ac=detail&id=` - Get video details

## Key Dependencies
- Kotlin: 2.0.21
- Compose BOM: 2024.09.00
- Media3: 1.2.0
- Retrofit: 2.9.0
- OkHttp: 4.12.0
- Hilt: (via version catalog)
- Room: (via version catalog)
- Coroutines: (via version catalog)
- Coil: (via version catalog)
- KSP: (via version catalog)

## Build Configuration
- **Compile SDK**: 36
- **Min SDK**: 24
- **Target SDK**: 36
- **Java Version**: 17
- **Namespace**: com.icinema
- **Clear Traffic**: Enabled (`usesCleartextTraffic=true`)

## Key Modules
- **ICinemaModule**: Hilt module for dependency injection (Retrofit, OkHttp, Repository)
- **DatabaseModule**: Hilt module for Room database setup
- **CmsApiService**: API service interface for CMS communication
- **CmsRepositoryImpl**: Repository implementation for data operations
- **VideoPlayer**: ExoPlayer integration for video playback
