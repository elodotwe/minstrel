# Minstrel Codebase Structure and Architecture

## 1. Directory Structure

```
/home/jacob/repos/minstrel/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/jacobarau/minstrel/
│   │   │   │   ├── data/              # Data models and state
│   │   │   │   │   ├── Track.kt
│   │   │   │   │   └── TrackListState.kt
│   │   │   │   ├── repository/        # Data access layer
│   │   │   │   │   ├── TrackRepository.kt (interface)
│   │   │   │   │   └── MediaStoreTrackRepository.kt (impl)
│   │   │   │   ├── ui/                # UI layer (Jetpack Compose)
│   │   │   │   │   ├── PlayerViewModel.kt
│   │   │   │   │   ├── TrackListView.kt
│   │   │   │   │   ├── SearchOverlay.kt
│   │   │   │   │   ├── SearchBar.kt
│   │   │   │   │   ├── TransportControls.kt
│   │   │   │   │   └── theme/
│   │   │   │   ├── player/            # Playback logic
│   │   │   │   │   ├── Player.kt (interface)
│   │   │   │   │   ├── ExoPlayerPlayer.kt
│   │   │   │   │   ├── PlayerRepository.kt
│   │   │   │   │   └── PlaybackState.kt
│   │   │   │   ├── media/             # Media services
│   │   │   │   │   ├── PlayerService.kt
│   │   │   │   │   ├── BrowserService.kt
│   │   │   │   │   └── MediaButtonReceiver.kt
│   │   │   │   ├── di/                # Dependency injection
│   │   │   │   │   ├── RepositoryModule.kt
│   │   │   │   │   └── PlayerModule.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   └── MinstrelApplication.kt
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 2. How Tracks Are Currently Loaded and Displayed

### Data Flow Diagram

```
MainActivity (View)
    ↓
PlayerViewModel (ViewModel)
    ├─ tracks: StateFlow<TrackListState>
    ├─ playbackState: StateFlow<PlaybackState>
    └─ searches using: searchQuery: MutableStateFlow<String?>
    ↓
TrackRepository (Interface)
    ├─ getTracks(filter: String?): Flow<TrackListState>
    ↓
MediaStoreTrackRepository (Implementation)
    ├─ Queries Android MediaStore
    ├─ Handles permissions
    └─ Returns List<Track>
    ↓
UI Components (Jetpack Compose)
    ├─ TrackList (main display)
    └─ SearchOverlay (search results)
```

### Track Loading Process

1. **Initialization** (MainActivity.onCreate):
   - PlayerViewModel is created via Hilt injection
   - Views are set up with Compose

2. **State Collection**:
   - UI collects states: `trackListState`, `playbackState`, `isPreviousEnabled`, `isNextEnabled`, `shuffleModeEnabled`
   - All states are collected as `StateFlow<T>` with `.collectAsStateWithLifecycle()`

3. **Track Fetching** (PlayerViewModel):
   - Initial state: `TrackListState.Loading`
   - `searchQuery` triggers reactive chain: `searchQuery.flatMapLatest { query -> trackRepository.getTracks(query) }`
   - When search query changes → repository is called with new filter
   - Result: `TrackListState.Success(tracks)` or `TrackListState.MissingPermissions`

4. **Display** (TrackListView.kt):
   - When `TrackListState.Success` → renders `LazyColumn` with all tracks
   - Each track shows:
     - Title (or filename if no title)
     - Artist (or "Unknown Artist")
     - Directory path
   - Highlights currently playing track with primaryContainer background
   - Uses snap fling behavior for smooth scrolling
   - Auto-scrolls to currently playing track

### Current Track Sorting

**DEFAULT SORT ORDER** (hardcoded in MediaStoreTrackRepository, line 94):
```kotlin
val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"
```
- Sorts by file path in ascending order
- No grouping currently implemented

---

## 3. TrackViewModel (Actually PlayerViewModel)

**Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/ui/PlayerViewModel.kt`

### State Management

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playerRepository: PlayerRepository
) : ViewModel()
```

#### Exposed State Flows (State):

| StateFlow | Type | Purpose |
|-----------|------|---------|
| `tracks` | `StateFlow<TrackListState>` | All tracks or search results |
| `playbackState` | `StateFlow<PlaybackState>` | Current playback info |
| `isPreviousEnabled` | `StateFlow<Boolean>` | Can skip to previous |
| `isNextEnabled` | `StateFlow<Boolean>` | Can skip to next |
| `shuffleModeEnabled` | `StateFlow<Boolean>` | Shuffle mode status |

#### Private State:

```kotlin
private val searchQuery = MutableStateFlow<String?>(null)
```
- Used to trigger reactive track queries
- Changed via `onSearchQueryChanged(query: String)`

#### Key Methods (Actions):

```kotlin
fun onTrackSelected(track: Track, trackListState: TrackListState)
fun onPlayPauseClicked()
fun onSeek(position: Long)
fun onSearchQueryChanged(query: String)
fun onPreviousClicked()
fun onNextClicked()
fun onShuffleClicked()
```

#### Reactive Flow Chain:

```kotlin
val tracks: StateFlow<TrackListState> = searchQuery
    .flatMapLatest { query ->                    // When query changes
        trackRepository.getTracks(query)          // Fetch new tracks
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrackListState.Loading
    )
```

---

## 4. Track Data Model

**Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/data/Track.kt`

```kotlin
data class Track(
    val uri: Uri,                      // Content URI for playback
    val title: String? = null,         // Track title from metadata
    val artist: String? = null,        // Artist name
    val album: String? = null,         // Album name
    val filename: String,              // Filename only (e.g., "song.mp3")
    val directory: String,             // Full directory path
    val albumArtUri: Uri? = null,      // Album art thumbnail URI
)
```

### Properties Available for Sorting/Grouping:
- Title (nullable)
- Artist (nullable)
- Album (nullable)
- Filename
- Directory
- URI (for grouping by storage location)

---

## 5. TrackListState (State Model)

**Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/data/TrackListState.kt`

```kotlin
sealed interface TrackListState {
    data object Loading : TrackListState
    data class Success(val tracks: List<Track>) : TrackListState
    data object MissingPermissions : TrackListState
}
```

### Usage Pattern:
- **Loading**: Show spinner while fetching tracks
- **Success**: Contains `List<Track>` to display
- **MissingPermissions**: Show permission request message

---

## 6. TrackRepository Pattern

### Interface (Abstract Contract)

**Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/repository/TrackRepository.kt`

```kotlin
interface TrackRepository {
    fun getTracks(filter: String? = null): Flow<TrackListState>
}
```

### Implementation

**Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/repository/MediaStoreTrackRepository.kt`

```kotlin
class MediaStoreTrackRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : TrackRepository
```

#### Key Features:

1. **Reactive with callbackFlow**:
   ```kotlin
   override fun getTracks(filter: String?): Flow<TrackListState> = callbackFlow {
       val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
           override fun onChange(selfChange: Boolean) {
               trySend(TrackListState.Success(query(filter)))
           }
       }
       // Register observer for MediaStore changes
       context.contentResolver.registerContentObserver(
           MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
           true,
           observer
       )
       // Send initial results
       trySend(TrackListState.Success(query(filter)))
       
       awaitClose {
           context.contentResolver.unregisterContentObserver(observer)
       }
   }.flowOn(Dispatchers.IO)
   ```

2. **Permission Handling**:
   - Checks `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (older)
   - Returns `TrackListState.MissingPermissions` if not granted

3. **Query Method**:
   ```kotlin
   private fun query(filter: String?): List<Track>
   ```
   - Queries MediaStore with filter (title, artist, album, filename, path)
   - Default sort: `${MediaStore.Audio.Media.DATA} ASC` (file path)
   - Returns: `List<Track>`

4. **Runs on IO Dispatcher**:
   ```kotlin
   .flowOn(Dispatchers.IO)  // MediaStore queries run on background thread
   ```

#### Query Details:

- **Collection**: `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`
- **Projection** (columns):
  - `_ID` → used to construct content URI
  - `DATA` → file path (used for sort)
  - `TITLE`
  - `ALBUM`
  - `ARTIST`
  - `ALBUM_ID` → used to construct album art URI

- **Selection** (WHERE clause):
  - `IS_MUSIC != 0` (filter to music files)
  - If filter provided: `(DATA LIKE ? OR TITLE LIKE ? OR ALBUM LIKE ? OR ARTIST LIKE ?)`

---

## 7. Current UI Structure for Displaying Tracks

### Main Components

#### MainActivity.kt
- Scaffold with TopBar (SearchBar) + BottomBar (TransportControls)
- Shows TrackList or SearchOverlay
- Manages overall screen layout

#### TrackListView.kt (TrackList Composable)
```kotlin
@Composable
fun TrackList(
    trackListState: TrackListState,
    playbackState: PlaybackState,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
)
```

**Rendering Logic**:
- `Loading` → CircularProgressIndicator
- `MissingPermissions` → Error text
- `Success` → LazyColumn with track items

**Track Item Display**:
```
┌─────────────────────────────────────┐
│ Track Title or Filename             │  ← headlineSmall
│ /path/to/directory                  │  ← bodyLarge (start ellipsis)
│ Artist Name or "Unknown Artist"     │  ← bodyMedium
└─────────────────────────────────────┘
```

**Current Playing Track**:
- Highlighted with `primaryContainer` background
- Uses `onPrimaryContainer` text color

**Scrolling Behavior**:
- `LazyColumn` with snap fling behavior
- `SnapPosition.Start` alignment
- Auto-scrolls to currently playing track

#### SearchOverlay.kt
- Full-screen search interface
- Reuses `TrackList` component for results display
- Has text input + voice search (microphone button)
- Shows search results in same format as main track list

#### SearchBar.kt
- Top bar display showing current track info
- Triggers search overlay when tapped/interacted

#### TransportControls.kt
- Bottom bar with play/pause, prev/next, shuffle buttons
- Shows current playback progress
- Seek slider

---

## 8. Dependency Injection Pattern

### RepositoryModule.kt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindTrackRepository(impl: MediaStoreTrackRepository): TrackRepository
}
```

- **Scope**: Singleton (one instance for entire app)
- **Pattern**: Interface binding to implementation
- **Location**: `/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/di/RepositoryModule.kt`

### Usage in ViewModel

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackRepository: TrackRepository,  // Injected as interface
    private val playerRepository: PlayerRepository
) : ViewModel()
```

---

## 9. Key Observations for Sorting & Grouping Feature

### Current Limitations:
1. **Hard-coded sorting**: File path ascending only
2. **No grouping**: All tracks displayed in flat list
3. **Search only filters**: Doesn't sort/group results
4. **No persistence**: Sort order isn't saved

### Opportunities for Enhancement:

1. **Extend TrackListState** to include:
   ```kotlin
   data class Success(
       val tracks: List<Track>,
       val sortOrder: SortOrder = SortOrder.FILENAME_ASC,
       val groupBy: GroupBy? = null
   ) : TrackListState
   ```

2. **Add to PlayerViewModel**:
   ```kotlin
   val sortOrder = MutableStateFlow<SortOrder>(SortOrder.FILENAME_ASC)
   val groupBy = MutableStateFlow<GroupBy?>(null)
   
   fun changeSortOrder(sortOrder: SortOrder)
   fun changeGrouping(groupBy: GroupBy?)
   ```

3. **Modify TrackRepository interface**:
   ```kotlin
   fun getTracks(
       filter: String? = null,
       sortOrder: SortOrder = SortOrder.FILENAME_ASC,
       groupBy: GroupBy? = null
   ): Flow<TrackListState>
   ```

4. **Update UI** to:
   - Add sort/group selector (buttons/menu)
   - Group tracks with headers
   - Sort within groups

### Current Tech Stack:
- **State Management**: StateFlow + Coroutines
- **DI**: Hilt
- **UI**: Jetpack Compose with LazyColumn
- **Data Access**: Room/MediaStore
- **Threading**: Dispatchers.IO for repository

---

## 10. Architecture Summary

```
MVVM Pattern
│
├─ View Layer (UI)
│  ├─ MainActivity.kt
│  ├─ TrackListView.kt (LazyColumn)
│  ├─ SearchOverlay.kt
│  └─ Other Composables
│
├─ ViewModel Layer
│  └─ PlayerViewModel.kt
│     ├─ Exposes: tracks, playbackState, etc.
│     └─ Actions: onTrackSelected, onSearchQueryChanged, etc.
│
└─ Model Layer
   ├─ Repository (Data Access)
   │  ├─ TrackRepository (interface)
   │  └─ MediaStoreTrackRepository (impl)
   ├─ Data Models
   │  ├─ Track
   │  ├─ TrackListState
   │  └─ PlaybackState
   └─ Dependencies (Hilt DI)

Reactive Flow:
searchQuery → flatMapLatest → trackRepository.getTracks() → StateFlow<TrackListState>
                                    ↓
                          MediaStoreTrackRepository.query()
                                    ↓
                          ContentObserver (reactive updates)
```

---

## 11. Code Style Guidelines (from AGENTS.md)

### Key Points for Your Implementation:

1. **Naming**:
   - Classes/Interfaces: `PascalCase` (e.g., `SortOrder`, `GroupByOption`)
   - Functions: `camelCase` (e.g., `changeSortOrder()`)
   - StateFlow: Noun descriptors (e.g., `val sortOrder: StateFlow<SortOrder>`)

2. **Imports**: Group by package (android, androidx, com, kotlin, kotlinx, java)

3. **Error Handling**: Use sealed classes (pattern already in use with `TrackListState`)

4. **Architecture**: Hilt for DI, Coroutines for async, Flow for state

5. **Jetpack Compose**:
   - Hoist state to ViewModel
   - Use stable types for Composable parameters
   - Create @Preview functions

6. **No new third-party libraries** without compelling reason

---

## 12. Testing Information

### Test Locations:
- Unit tests: `/home/jacob/repos/minstrel/app/src/test/java/`
- Instrumented tests: `/home/jacob/repos/minstrel/app/src/androidTest/java/`

### Build Commands:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

### Dependencies:
- JUnit
- AndroidX Test (Espresso, Compose UI Test)
- Compose UI Test Junit4

