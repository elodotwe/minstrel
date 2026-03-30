# Minstrel Codebase - Visual Reference Guide

## Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                       UI Layer (Jetpack Compose)                │
│  MainActivity.kt                                                │
│  ├─ Scaffold + TopBar (SearchBar) + BottomBar (Controls)       │
│  ├─ TrackList (or SearchOverlay)                               │
│  └─ Displays: trackListState, playbackState, etc.              │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          │ Collects StateFlow with
                          │ collectAsStateWithLifecycle()
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│                  ViewModel Layer (PlayerViewModel)              │
│                                                                 │
│  State Flows (Read-Only from UI perspective):                  │
│  ├─ tracks: StateFlow<TrackListState>        ◄── MAIN STATE   │
│  ├─ playbackState: StateFlow<PlaybackState>                   │
│  ├─ isPreviousEnabled: StateFlow<Boolean>                     │
│  ├─ isNextEnabled: StateFlow<Boolean>                         │
│  └─ shuffleModeEnabled: StateFlow<Boolean>                    │
│                                                                 │
│  Private Mutable State:                                        │
│  └─ searchQuery: MutableStateFlow<String?>  ◄── FILTER       │
│                                                                 │
│  Public Actions (Functions):                                   │
│  ├─ onTrackSelected(track, state)                             │
│  ├─ onSearchQueryChanged(query)                               │
│  ├─ onPlayPauseClicked()                                      │
│  └─ ... other player control actions ...                      │
│                                                                 │
│  Reactive Chain:                                               │
│  searchQuery                                                   │
│    └─► flatMapLatest → trackRepository.getTracks(query)        │
│          └─► stateIn(viewModelScope)                           │
│            └─► tracks StateFlow (emits to UI)                  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          │ Calls getTracks(filter)
                          │ on background thread
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│              Repository Layer (Interfaces)                      │
│                                                                 │
│  TrackRepository (Interface)                                   │
│  └─ getTracks(filter: String?): Flow<TrackListState>          │
│                                                                 │
│  PlayerRepository (Interface)                                  │
│  └─ getPlayer(): Player                                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          │ Injected via Hilt
                          │ RepositoryModule (Singleton)
                          │
┌─────────────────────────▼───────────────────────────────────────┐
│         Implementation Layer (MediaStore + Content Observer)    │
│                                                                 │
│  MediaStoreTrackRepository                                     │
│  ├─ getTracks(filter):                                         │
│  │  ├─ Checks READ_MEDIA_AUDIO permission                     │
│  │  ├─ Creates ContentObserver for MediaStore changes          │
│  │  ├─ Calls query(filter) to fetch tracks                    │
│  │  └─ Returns callbackFlow: Flow<TrackListState>              │
│  │                                                              │
│  └─ query(filter): List<Track>                                 │
│     ├─ Queries MediaStore.Audio.Media.EXTERNAL_CONTENT_URI   │
│     ├─ Projects: _ID, DATA, TITLE, ALBUM, ARTIST, ALBUM_ID   │
│     ├─ Filters: IS_MUSIC != 0 AND text search                 │
│     ├─ Sorts: DATA ASC (by file path)                         │
│     └─ Returns: List<Track>                                    │
│                                                                 │
│  Reactive Features:                                            │
│  ├─ ContentObserver → automatic updates on MediaStore changes │
│  ├─ callbackFlow → Flow emissions                             │
│  └─ flowOn(Dispatchers.IO) → background thread execution      │
│                                                                 │
│  Performance: Sorting happens in SQL, not in Kotlin             │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          │ Queries Android System
                          │ MediaStore ContentProvider
                          │
                  ┌───────▼────────┐
                  │  Android File  │
                  │  System Audio  │
                  │  Metadata DB   │
                  └────────────────┘
```

---

## State Management Pattern

### State Container (TrackListState)
```
TrackListState (sealed interface)
├─ Loading (idle, fetching)
├─ Success(tracks: List<Track>) (main display state)
└─ MissingPermissions (error state)
```

### State Transformation Pipeline
```
User Action (search)
    ↓
searchQuery.value = newQuery
    ↓
Reactive Chain Triggered
    ↓
searchQuery.flatMapLatest { query → 
    trackRepository.getTracks(query)
}
    ↓
Repository fetches from MediaStore
    ↓
Returns Flow<TrackListState>
    ↓
stateIn() converts to StateFlow
    ↓
tracks StateFlow updated
    ↓
UI recomposes with new tracks
```

---

## File Organization & Dependencies

```
com.jacobarau.minstrel/
│
├─ data/                          # Pure data models & state
│  ├─ Track                        # Data class (immutable)
│  ├─ TrackListState              # Sealed interface for state management
│  └─ PlaybackState               # Player state (from player module)
│
├─ repository/                    # Data access abstraction
│  ├─ TrackRepository             # Interface (contract)
│  └─ MediaStoreTrackRepository   # Implementation (uses Android APIs)
│
├─ ui/                            # Presentation layer
│  ├─ PlayerViewModel             # State holder & business logic
│  ├─ TrackListView               # Main composable (uses StateFlow)
│  ├─ SearchOverlay               # Reuses TrackList
│  ├─ SearchBar                   # UI component
│  ├─ TransportControls           # Player controls
│  └─ theme/                      # Material Design theme
│
├─ player/                        # Playback abstraction
│  ├─ Player                      # Interface
│  ├─ ExoPlayerPlayer             # Implementation
│  ├─ PlayerRepository            # Provides Player instance
│  └─ PlaybackState               # Playback state model
│
├─ media/                         # Android media services
│  ├─ PlayerService               # MediaSession service
│  ├─ BrowserService              # MediaBrowser service
│  └─ MediaButtonReceiver         # Hardware button receiver
│
├─ di/                            # Dependency injection
│  ├─ RepositoryModule            # Hilt module (binds interfaces)
│  └─ PlayerModule                # Hilt module (binds Player)
│
├─ MainActivity                   # Activity (entry point)
└─ MinstrelApplication            # Application class (Hilt setup)

Dependency Flow:
MainActivity → PlayerViewModel
           ↓
         TrackRepository (injected)
         PlayerRepository (injected)
           ↓
    MediaStoreTrackRepository
    ExoPlayerPlayer
```

---

## Key Components Reference

### Track Data Model
```kotlin
data class Track(
    val uri: Uri,              // Content:// URI for playback
    val title: String?,        // Metadata: track name
    val artist: String?,       // Metadata: artist name
    val album: String?,        // Metadata: album name
    val filename: String,      // Filesystem: song.mp3
    val directory: String,     // Filesystem: /storage/.../Music
    val albumArtUri: Uri?      // Thumbnail for UI
)
```

### State Models
```kotlin
// For track list state
sealed interface TrackListState {
    object Loading
    data class Success(val tracks: List<Track>)
    object MissingPermissions
}

// For playback state
sealed class PlaybackState {
    object Stopped
    data class Playing(
        val isPaused: Boolean,
        val tracks: List<Track>,       // Current queue
        val currentTrackIndex: Int,    // Position in queue
        val trackProgressMillis: Long,
        val trackDurationMillis: Long
    )
}
```

### ViewModel (State Manager)
```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playerRepository: PlayerRepository
) : ViewModel()

// Exposed StateFlows
val tracks: StateFlow<TrackListState>
val playbackState: StateFlow<PlaybackState>
val isPreviousEnabled: StateFlow<Boolean>
val isNextEnabled: StateFlow<Boolean>
val shuffleModeEnabled: StateFlow<Boolean>

// Private mutable state
private val searchQuery: MutableStateFlow<String?>

// Public actions
fun onTrackSelected(track: Track, trackListState: TrackListState)
fun onPlayPauseClicked()
fun onSeek(position: Long)
fun onSearchQueryChanged(query: String)
fun onPreviousClicked()
fun onNextClicked()
fun onShuffleClicked()
```

---

## Current UI Structure

### Main Screen Layout
```
┌─────────────────────────────────────┐
│       SearchBar (Top)               │  ◄── MinstrelSearchBar()
│       - Shows current playing track │
│       - Click to open search        │
├─────────────────────────────────────┤
│                                     │
│       TrackList (LazyColumn)        │  ◄── TrackList()
│       - Displays all tracks         │     or SearchOverlay()
│       - Item: Track                 │
│         ├─ Title/Filename           │
│         ├─ Artist                   │
│         ├─ Directory                │
│         ├─ Highlighted if playing   │
│         └─ Clickable (play)         │
│                                     │
├─────────────────────────────────────┤
│  ◄ ⏮ ⏯ ⏭ ► [====o====]          │  ◄── TransportControls()
│  Shuffle Prev Play Next   Seek      │     + Seeking capabilities
└─────────────────────────────────────┘
  FAB: Search Icon
```

### Search Overlay Layout
```
┌─────────────────────────────────────┐
│ [✕] [Search Input] [🎤]            │  ◄── Close + TextField + Mic
├─────────────────────────────────────┤
│                                     │
│  TrackList (same as above)          │  ◄── Results display
│  - Filtered by search query         │
│  - Same styling & interactions      │
│                                     │
└─────────────────────────────────────┘
```

---

## Compose Component Hierarchy

```
MainActivity
├─ MinstrelTheme
│  └─ if showSearchOverlay
│     └─ SearchOverlay
│        ├─ TextField (search input)
│        ├─ IconButton (close)
│        ├─ IconButton (voice search)
│        └─ TrackList (results)
│  else
│     └─ Scaffold
│        ├─ topBar: MinstrelSearchBar
│        ├─ bottomBar: TransportControls
│        ├─ floatingActionButton: FloatingActionButton (search)
│        └─ content: TrackList

TrackList
├─ Box (center loading/errors)
├─ LazyColumn (with snap behavior)
│  └─ itemsIndexed(tracks)
│     └─ Column (track item)
│        ├─ Text (title)
│        ├─ Text (directory)
│        └─ Text (artist)
```

---

## MediaStore Query Process

```
getTracks(filter: String?)
│
├─ Check Permission
│  ├─ YES → Continue
│  └─ NO → Return MissingPermissions
│
├─ Register ContentObserver
│  └─ Listens for MediaStore changes
│
├─ Call query(filter)
│  │
│  ├─ Build SQL Query
│  │  ├─ Collection: EXTERNAL_CONTENT_URI
│  │  ├─ Projection: _ID, DATA, TITLE, ALBUM, ARTIST, ALBUM_ID
│  │  ├─ Where: IS_MUSIC != 0 AND text search filter
│  │  └─ OrderBy: DATA ASC
│  │
│  └─ Execute cursor iteration
│     ├─ Extract: id, path, title, album, artist, albumId
│     ├─ Construct: Track objects with URIs
│     └─ Return: List<Track>
│
├─ Emit TrackListState.Success(tracks)
│
└─ On MediaStore Change
   └─ Re-query and re-emit
```

---

## Search Flow

```
User Types Search Term
    ↓
SearchOverlay.onSearchQueryChanged(text)
    ↓
MainActivity calls viewModel.onSearchQueryChanged(text)
    ↓
PlayerViewModel.searchQuery.value = text
    ↓
Reactive Chain Triggered:
  searchQuery.flatMapLatest { query →
      trackRepository.getTracks(query)  ◄─ Passes filter
  }
    ↓
MediaStoreTrackRepository.getTracks(filter)
    ↓
query(filter) → SQL LIKE on TITLE, ARTIST, ALBUM, DATA
    ↓
Returns filtered List<Track>
    ↓
TrackListState.Success(filtered_tracks)
    ↓
UI recomposes with filtered results
```

---

## Testing Structure

```
app/src/
├─ main/
│  ├─ java/com/jacobarau/minstrel/ (production code)
│  └─ AndroidManifest.xml
│
├─ test/
│  └─ java/com/jacobarau/minstrel/ (unit tests)
│     └─ ExampleUnitTest.kt
│     └─ TODO: Add repository, viewmodel, composable tests
│
└─ androidTest/
   └─ java/com/jacobarau/minstrel/ (instrumented tests)
      └─ ExampleInstrumentedTest.kt
      └─ TODO: Add UI, integration tests
```

### Build Commands
```bash
# Build entire project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run single unit test
./gradlew testDebugUnitTest --tests "com.jacobarau.minstrel.ExampleUnitTest.method"

# Run instrumented tests (on emulator/device)
./gradlew connectedDebugAndroidTest
```

---

## Permissions & Manifest

```kotlin
// Required Permissions (AndroidManifest.xml)
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />  <!-- Android 13+ -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />  <!-- Older Android -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />  <!-- For voice search -->
```

MediaStoreTrackRepository checks:
```kotlin
val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Manifest.permission.READ_MEDIA_AUDIO  // Android 13+
} else {
    Manifest.permission.READ_EXTERNAL_STORAGE  // Before Android 13
}
```

---

## Dependencies (build.gradle.kts)

```
Core Framework:
- androidx.core:core-ktx
- androidx.lifecycle (lifecycle-runtime, lifecycle-compose)
- androidx.activity:activity-compose

Jetpack Compose:
- androidx.compose.* (UI, Material3, Icons, etc.)

State Management & DI:
- dagger.hilt.android
- kotlin.org.jetbrains.kotlinx:kotlinx-coroutines

Media:
- androidx.media3:media3-exoplayer
- androidx.media:media

Storage:
- androidx.datastore:datastore-preferences

Testing:
- junit
- androidx.test (espresso, compose.ui.test)
```

---

## Quick Implementation Checklist for Sorting Feature

```
Priority 1 - Core Logic:
[ ] Create data/SortOrder.kt (enum)
[ ] Create data/GroupBy.kt (enum)
[ ] Update data/TrackListState.kt (add sort/group fields)
[ ] Update repository/TrackRepository.kt (add parameters)
[ ] Update repository/MediaStoreTrackRepository.kt (implement)
[ ] Update ui/PlayerViewModel.kt (add state flows & actions)

Priority 2 - UI:
[ ] Update ui/TrackListView.kt (render grouped/sorted)
[ ] Create ui/SortingMenu.kt (sort/group selector)
[ ] Update MainActivity.kt (add controls)

Priority 3 - Polish:
[ ] Add unit tests
[ ] Update @Preview functions
[ ] Handle edge cases (null artist, etc.)

Priority 4 - Enhancement:
[ ] Persist sort preference (DataStore)
[ ] Add animations for grouping collapse/expand
[ ] Add view options (list vs grid)
```

