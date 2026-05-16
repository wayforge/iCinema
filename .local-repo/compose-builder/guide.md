# Compose Builder Guide

Guide schema: compose-builder-v2

## Project Mode
- Compose adoption: Android app uses Jetpack Compose for main screens, hosted from `ComponentActivity.setContent`.
- UI architecture: screen functions usually wrap a Hilt/ViewModel boundary, then delegate to page/section composables.
- State pattern: feature contracts expose immutable `UiState` data classes and `UiIntent`; current code often collects with `collectAsState`, but lifecycle-aware collection is available.
- Resource pattern: Material3 theme and runtime remote images through Coil `AsyncImage`; no app-wide Compose design system components found.

## Hard Rules
- Follow root `AGENTS.md`: understand existing implementation first, keep changes minimal, verify when possible, do not touch unrelated changes.
- UI changes should not redesign navigation, reducers, repositories, use cases, or data contracts unless explicitly requested.
- Use existing `iCinemaTheme`, Material3 color/typography, Coil, and local page components before adding new abstractions.

## Entry Points
- Theme: `app/src/main/java/com/icinema/ui/theme/Theme.kt`, `iCinemaTheme`.
- Colors: dark Material3 scheme in `Theme.kt`; `Color.kt` contains starter unused-looking sample colors.
- Strings: mostly inline Chinese UI strings; `app/src/main/res/values/strings.xml` is minimal.
- Images: remote poster/backdrop images through `coil.compose.AsyncImage`; launcher and a few drawable assets under `res`.
- Host/navigation: Activity launchers in page activities; home uses `HomeActivity` and `HomeScreen`.
- Dialog: none found as shared component; Material3 dialogs are not established as a reusable app pattern.
- List/paging: `LazyVerticalGrid`, `LazyRow`, pull refresh from Material 2 pullrefresh.

## Verification Discovery
- Gradle wrapper: `./gradlew`.
- Affected module compile: `./gradlew :app:compileDebugKotlin`.
- Lint/test commands: `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`, `./gradlew :app:assembleDebug`.
- Compose UI tests: dependencies exist, but no home-specific UI tests found.
- Screenshot tests: none found for Paparazzi, Roborazzi, Shot, or similar.
- Preview/screenshot conventions: previews exist mostly in detail/player UI files with same-file or colocated preview fixtures.

## Component Map
- Button: Material3 `TextButton`, `IconButton`, `FilterChip`, `AssistChip`, `Card(onClick)`.
- Image: Coil `AsyncImage`.
- Text: Material3 `Text` with `MaterialTheme.typography`.
- Top bar: home has `PageHeader`; other screens often use `Scaffold.topBar`.
- Dialog: none found.
- Empty/loading: home has `SimpleEmptyState`, `SimpleErrorState`, `VideoCardSkeleton`; widgets package has simple loading/error/empty cells.
- Refresh/paging: home `VideoGrid` owns pull refresh and infinite load trigger.
- Tabs/pager: home has custom bottom tab UI; Material3 navigation components are available.
- Legacy/View interop: player screens bridge Media3/Android view surfaces; not relevant to home page UI.

## Page Patterns
- New screen: Activity obtains ViewModel, collects state, forwards events into a pure content/page composable where possible.
- Dialog: no confirmed reusable pattern.
- List: use `LazyColumn`, `LazyRow`, or `LazyVerticalGrid`; provide stable keys when item identity matters.
- Grid: home video cards use `GridCells.Fixed(3)` with `VideoCard` and `VideoCardSkeleton`.
- Mixed View/Compose: player feature only.

## Before Coding Checklist
- Keep home work inside `app/src/main/java/com/icinema/pages/home` unless a verified shared rule requires otherwise.
- Prefer lifecycle-aware Flow collection for Android screen wrappers when touching state collection.
- Keep image assets bound to existing remote URLs; do not fake missing poster artwork.
- Add previews for materially changed pure UI surfaces when practical.
- Preserve user-visible Chinese copy unless the change explicitly targets content.

## Legacy or Conflict Notes
- Several existing screen wrappers still use `collectAsState`; this is common local history, not a hard rule.
- The app mixes Material 2 pull refresh with Material3 UI because Material3 pull refresh is not established here.
- Some typography definitions use non-zero and negative letter spacing; treat theme as project fact and do not refactor globally during page UI work.
