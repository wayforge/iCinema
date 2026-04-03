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
├── pages/                   # Presentation layer
│   ├── detail/             # Video detail page
│   ├── home/               # Home screen and view model
│   ├── models/             # Domain models
│   ├── player/             # Video player
│   ├── settings/           # Settings page
│   └── widgets/            # Common UI components
├── di/                     # Dependency injection
├── ui/                     # UI themes and common composables
└── util/                   # Utility functions
```

## Configuration
The API base URL is configured in `app/src/main/java/com/icinema/di/ICinemaModule.kt`:
```kotlin
private const val BASE_URL = "https://caiji.dyttzyapi.com/"
```

## Building and Running
```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Code linting
./gradlew lint
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

## Key Components
- **ICinemaApp**: Application class with Hilt dependency injection setup
- **HomeActivity**: Main entry point of the application
- **HomeScreen**: Compose UI for the home page with video grid and search
- **HomeViewModel**: Manages state and business logic for home screen
- **HomeState**: Defines state, intents, and events for the home screen (MVI pattern)
- **DetailActivity**: Shows video details and player
- **DetailViewModel**: Manages state for video details
- **ICinemaModule**: Hilt module for dependency injection
- **CmsApiService**: API service interface for CMS communication
- **CmsRepositoryImpl**: Repository implementation for data operations
- **VideoPlayer**: ExoPlayer integration for video playback

## Architecture Patterns
- **MVI (Model-View-Intent)**: Used for state management in Home screen
- **Clean Architecture**: Separation of concerns with data, domain, and presentation layers
- **MVVM**: ViewModels manage UI-related data and handle business logic

## Key Features
- Video list browsing with category filtering
- Search functionality
- Video detail page with multiple playback sources
- Episode selection for series
- Full-screen video player
- Pull-to-refresh and infinite scrolling
- Error handling and loading states

## API Integration
The app communicates with Apple CMS V10 using these endpoints:
- `?ac=list` - Get video list (with pagination, categories, search)
- `?ac=detail&id=` - Get video details

## Dependencies
- Kotlin 2.0.21
- Compose BOM 2024.09.00
- Media3 1.2.0
- Retrofit 2.9.0
- OkHttp 4.12.0
- Hilt 2.48
- Coroutines 1.7.3
- Coil 2.5.0
- Accompanist SwipeRefresh 0.34.0
