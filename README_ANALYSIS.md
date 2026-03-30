# Minstrel Codebase Analysis - Complete Summary

This document provides a comprehensive understanding of the Minstrel music player codebase to support your sorting and grouping feature implementation.

## Documentation Files Created

Three detailed documents have been generated for your reference:

1. **CODEBASE_ANALYSIS.md** (16 KB)
   - Complete directory structure
   - Detailed data flow diagrams
   - PlayerViewModel internals
   - Track data model and state management
   - Repository pattern explanation
   - UI component structure
   - Code style guidelines
   - Testing information

2. **IMPLEMENTATION_GUIDE.md** (13 KB)
   - Step-by-step implementation plan
   - Code snippets for each component
   - Priority-ordered file modifications
   - Testing strategy
   - Performance considerations
   - Backward compatibility notes
   - Pattern alignment with existing code

3. **VISUAL_REFERENCE.md** (19 KB)
   - ASCII architecture diagrams
   - Component hierarchies
   - State management patterns
   - File organization and dependencies
   - UI layout structures
   - MediaStore query process
   - Search flow
   - Quick implementation checklist

---

## Executive Summary

### What You Need to Know

The Minstrel app follows a clean **MVVM (Model-View-ViewModel) architecture** with:

- **View Layer**: Jetpack Compose UI components (TrackListView, SearchOverlay, etc.)
- **ViewModel Layer**: PlayerViewModel manages all UI state using StateFlow
- **Repository Layer**: TrackRepository interface with MediaStoreTrackRepository implementation
- **Model Layer**: Track data class and state models (TrackListState, PlaybackState)

### Current Track Loading Flow

```
User sees UI (MainActivity)
      ↓
UI collects PlayerViewModel.tracks StateFlow
      ↓
ViewModel reactively calls TrackRepository.getTracks(filter)
      ↓
Repository queries Android MediaStore on background thread
      ↓
Tracks returned sorted by file path (DATA ASC)
      ↓
UI recomposes with new track list in LazyColumn
```

### Current Limitations

1. **Hard-coded sorting**: Tracks always sorted by file path ascending
2. **No grouping**: All tracks displayed in flat list
3. **No sort/group UI**: No controls to change sort order or grouping
4. **No persistence**: Sort preferences not saved

---

## Key Files You'll Modify

### Priority 1: Core Logic (Backend)

| File | Changes |
|------|---------|
| `data/TrackListState.kt` | Add `sortOrder: SortOrder` and `groupBy: GroupBy?` fields to Success case |
| `data/SortOrder.kt` | NEW - Define sort options (TITLE_ASC, ARTIST_DESC, etc.) |
| `data/GroupBy.kt` | NEW - Define grouping options (ARTIST, ALBUM, DIRECTORY, NONE) |
| `repository/TrackRepository.kt` | Add `sortOrder` and `groupBy` parameters to `getTracks()` |
| `repository/MediaStoreTrackRepository.kt` | Implement sorting logic in `query()` method; pass sort/group to Success state |
| `ui/PlayerViewModel.kt` | Add `sortOrder` and `groupBy` StateFlows; combine with searchQuery in reactive chain |

### Priority 2: UI Updates

| File | Changes |
|------|---------|
| `ui/TrackListView.kt` | Transform tracks into grouped display; add group headers; render grouped items |
| `ui/SortingMenu.kt` | NEW - Composable for sort/group selection (menu or bottom sheet) |
| `MainActivity.kt` | Add sort/group controls; collect new StateFlows from ViewModel |

### Priority 3: Polish & Testing

- Add unit tests for sorting logic
- Update @Preview functions
- Handle edge cases (null values)

---

## Architecture Overview

### MVVM Pattern Implementation

```
MainActivity (Entry Point)
    │
    ├─ Collects: trackListState, playbackState, etc.
    ├─ Shows: TrackList or SearchOverlay
    └─ Passes callbacks: onTrackSelected, onSearchQueryChanged, etc.
         │
         └────────────────────────────┐
                                      │
                         PlayerViewModel (State Manager)
                         ├─ Exposes: StateFlow<TrackListState>
                         ├─ Private: MutableStateFlow<String?> (search)
                         │
                         └─► trackRepository.getTracks(filter)
                                 │
                        ┌────────┴────────────┐
                        │                     │
            TrackRepository              (Interface)
            (Abstract contract)          Implemented by:
            │                            ↓
            └─► getTracks(                MediaStoreTrackRepository
                 filter,                 ├─ Queries MediaStore
                 sortOrder,              ├─ Handles permissions
                 groupBy                 ├─ Observes changes
                )                        └─ Returns Flow<TrackListState>
```

### State Management Pattern

```
MutableStateFlow (ViewModel maintains)
    ↓ (when user changes sort/group)
combine() + flatMapLatest()
    ↓ (triggers repository query)
Repository.getTracks()
    ↓ (queries Android MediaStore)
Returns Flow<TrackListState>
    ↓ (converts to StateFlow)
stateIn()
    ↓ (UI collects)
collectAsStateWithLifecycle()
    ↓ (recomposes Composables)
TrackList displays updated tracks
```

---

## Current Implementation Details

### Track Data Model
```kotlin
data class Track(
    val uri: Uri,                  // Content:// for playback
    val title: String?,            // Can be null
    val artist: String?,           // Can be null
    val album: String?,            // Can be null
    val filename: String,          // e.g., "song.mp3"
    val directory: String,         // Full path
    val albumArtUri: Uri?          // Thumbnail
)
```
**Available for sorting**: title, artist, album, filename, directory

### Current State Flows (PlayerViewModel)

| State | Type | Mutable | Purpose |
|-------|------|---------|---------|
| `tracks` | `StateFlow<TrackListState>` | No | Main track list to display |
| `playbackState` | `StateFlow<PlaybackState>` | No | Current playback info |
| `isPreviousEnabled` | `StateFlow<Boolean>` | No | Derived from track index |
| `isNextEnabled` | `StateFlow<Boolean>` | No | Derived from track index |
| `shuffleModeEnabled` | `StateFlow<Boolean>` | No | Shuffle mode status |
| `searchQuery` | `MutableStateFlow<String?>` | **Yes** | Filters tracks (private) |

**You'll add:**
- `sortOrder: MutableStateFlow<SortOrder>`
- `groupBy: MutableStateFlow<GroupBy?>`

### Current UI Structure

```
MainActivity
├─ Scaffold
│  ├─ topBar: SearchBar (current track display)
│  ├─ floatingActionButton: Search icon
│  ├─ bottomBar: TransportControls (play, pause, seek)
│  └─ content: TrackList (main display)
│     ├─ LazyColumn (virtualized scrolling)
│     └─ Items: Track rows (title, artist, directory)
└─ or SearchOverlay (full-screen search)
   ├─ SearchBar (text input + voice search)
   └─ TrackList (filtered results)
```

---

## How Sorting Currently Works

### Current Hard-coded Sort (in MediaStoreTrackRepository)

```kotlin
// Line 94 in MediaStoreTrackRepository.kt
val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"

// This means: sort by file path in ascending order
// Examples:
// /storage/emulated/0/Music/A/song1.mp3
// /storage/emulated/0/Music/A/song2.mp3
// /storage/emulated/0/Music/B/song3.mp3
```

### How to Implement Flexible Sorting

**Option 1: SQL Sorting (Recommended - Faster)**
```kotlin
private fun query(filter: String?, sortOrder: SortOrder): List<Track> {
    // Build SQL ORDER BY clause based on sortOrder parameter
    val sqlOrderBy = when (sortOrder) {
        SortOrder.TITLE_ASC -> "${MediaStore.Audio.Media.TITLE} ASC"
        SortOrder.TITLE_DESC -> "${MediaStore.Audio.Media.TITLE} DESC"
        SortOrder.ARTIST_ASC -> "${MediaStore.Audio.Media.ARTIST} ASC"
        // ... etc
    }
    
    // Pass sqlOrderBy to contentResolver.query()
    context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs.toTypedArray(),
        sqlOrderBy  // This is what changes
    )
    // Rest of query logic unchanged
}
```

**Option 2: Kotlin Sorting (Simpler, Slower for large lists)**
```kotlin
private fun query(filter: String?, sortOrder: SortOrder): List<Track> {
    val trackList = mutableListOf<Track>()
    
    // ... existing query code to populate trackList ...
    
    // Sort in Kotlin
    return when (sortOrder) {
        SortOrder.TITLE_ASC -> trackList.sortedBy { it.title ?: "" }
        SortOrder.TITLE_DESC -> trackList.sortedByDescending { it.title ?: "" }
        SortOrder.ARTIST_ASC -> trackList.sortedBy { it.artist ?: "" }
        // ... etc
        else -> trackList
    }
}
```

**Recommendation**: Use Option 1 (SQL sorting) for better performance.

---

## How to Implement Grouping

### Display Grouped Tracks

In `TrackListView.kt`:

```kotlin
// Transform flat list into display items (headers + tracks)
val displayItems: List<Any> = if (trackListState.groupBy != null) {
    tracks.groupBy { track ->
        when (trackListState.groupBy) {
            GroupBy.ARTIST -> track.artist ?: "Unknown Artist"
            GroupBy.ALBUM -> track.album ?: "Unknown Album"
            GroupBy.DIRECTORY -> track.directory
            GroupBy.NONE -> null  // Not grouped
        }
    }.flatMap { (header, groupedTracks) ->
        // Create display items: [header_string, track1, track2, ...]
        listOf(header) + groupedTracks.cast<Any>()
    }
} else {
    tracks.cast<Any>()
}

// Render mixed headers and tracks
LazyColumn {
    items(displayItems) { item ->
        when (item) {
            is String -> GroupHeader(text = item)
            is Track -> TrackItem(track = item)
        }
    }
}
```

### GroupBy Options to Support

```kotlin
enum class GroupBy {
    ARTIST,      // Group by: track.artist ?: "Unknown Artist"
    ALBUM,       // Group by: track.album ?: "Unknown Album"
    DIRECTORY,   // Group by: track.directory
    NONE         // No grouping
}
```

---

## Reactive Flow Pattern (Key to Understanding)

The codebase uses Kotlin Coroutines + Flow + StateFlow for all asynchronous state:

```kotlin
// ViewModel combines multiple StateFlows
val tracks: StateFlow<TrackListState> = combine(
    searchQuery,      // User's search text
    sortOrder,        // (You'll add this)
    groupBy           // (You'll add this)
) { query, sort, group ->
    Triple(query, sort, group)
}.flatMapLatest { (query, sort, group) ->
    // When any input changes, fetch fresh tracks
    trackRepository.getTracks(query, sort, group)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = TrackListState.Loading
)

// UI collects this
val trackListState by viewModel.tracks.collectAsStateWithLifecycle()
// When viewModel.tracks emits, UI recomposes automatically
```

**Key Points:**
- `combine()` = combine multiple flows/stateflows
- `flatMapLatest()` = when inputs change, cancel previous repository call and start new one
- `stateIn()` = convert Flow to StateFlow (holds latest value)
- `collectAsStateWithLifecycle()` = safely collect in Compose, respecting lifecycle

---

## Backward Compatibility

All changes maintain backward compatibility through default parameters:

```kotlin
// Existing code (no changes needed)
trackRepository.getTracks("search term")

// Will use defaults:
// sortOrder = SortOrder.FILENAME_ASC
// groupBy = null

// New code with explicit sorting/grouping
trackRepository.getTracks("search term", SortOrder.TITLE_ASC, GroupBy.ARTIST)
```

---

## Code Style Requirements

Follow existing patterns already used in codebase:

### Naming Conventions
- **Enums**: `SortOrder`, `GroupBy` (PascalCase)
- **Functions**: `onSortOrderChanged()` (camelCase)
- **StateFlows**: `val sortOrder: StateFlow<SortOrder>` (noun descriptors)
- **Variables**: `val currentTrack: Track` (camelCase)

### Architecture Patterns
- **State in ViewModel**: All UI state as StateFlow
- **DI with Hilt**: Use `@Inject` in ViewModel, `@Binds` in modules
- **Error Handling**: Use sealed classes (like `TrackListState`)
- **Reactive**: Use Flow + StateFlow, not callbacks or LiveData

### Compose Patterns
- **State Hoisting**: ViewModel is source of truth
- **Stable Parameters**: Pass enums (stable), not lambdas to nested Composables
- **Preview Functions**: Add `@Preview @Composable` for new Composables

---

## Testing Strategy

### Unit Tests to Add

```kotlin
class MediaStoreTrackRepositoryTest {
    @Test
    fun testSortByTitleAscending() {
        // Create mock tracks with different titles
        // Call query() with SortOrder.TITLE_ASC
        // Assert returned tracks are in title order
    }
    
    @Test
    fun testGroupByArtist() {
        // Create tracks with different artists
        // Create display items with GroupBy.ARTIST
        // Assert headers exist and tracks grouped correctly
    }
}

class PlayerViewModelTest {
    @Test
    fun testSortOrderChange() {
        // Set sortOrder to ARTIST_DESC
        // Assert tracks flow emits new value
        // Assert new tracks are sorted by artist descending
    }
}
```

### Build Commands

```bash
# Build project
./gradlew build

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (on emulator/device)
./gradlew connectedDebugAndroidTest
```

---

## Performance Considerations

### Sorting Performance
- **SQL Sorting (Recommended)**: O(n log n) in MediaStore engine, no Kotlin overhead
- **Kotlin Sorting**: O(n log n) but runs on main thread if not careful

### Grouping Performance
- **Grouping happens in UI layer**: `List.groupBy()` is fast for typical music libraries (<1000 songs)
- **Memoization**: If performance is concern, cache grouped results in ViewModel

### Recomposition
- **Stable types**: Enums and data classes are stable, won't cause extra recompositions
- **LazyColumn**: Only composes visible items, handles large lists efficiently
- **Group headers**: String type is stable, won't trigger recomposition

---

## Environment & Build Info

- **Language**: Kotlin
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 15)
- **Build System**: Gradle with Kotlin DSL
- **Hilt Version**: Latest (from libs)
- **Compose**: Latest (from libs)
- **Coroutines**: Latest (from libs)

---

## Next Steps

1. **Read the detailed documentation**:
   - `CODEBASE_ANALYSIS.md` - Deep dive into architecture
   - `IMPLEMENTATION_GUIDE.md` - Step-by-step coding guide
   - `VISUAL_REFERENCE.md` - Visual diagrams and flowcharts

2. **Start implementing (Priority Order)**:
   - Create `data/SortOrder.kt` enum
   - Create `data/GroupBy.kt` enum
   - Update `data/TrackListState.kt`
   - Update `repository/TrackRepository.kt` interface
   - Update `repository/MediaStoreTrackRepository.kt`
   - Update `ui/PlayerViewModel.kt`
   - Update `ui/TrackListView.kt`
   - Create `ui/SortingMenu.kt` UI
   - Update `MainActivity.kt`

3. **Test**:
   - Add unit tests
   - Test on actual device/emulator
   - Verify sort/group functionality

4. **Polish**:
   - Add animations
   - Persist sort preferences (DataStore)
   - Handle edge cases
   - Update UI previews

---

## Files Reference

### All Source Files
```
/home/jacob/repos/minstrel/app/src/main/java/com/jacobarau/minstrel/

data/
├─ Track.kt
├─ TrackListState.kt
└─ PlaybackStateRepository.kt

repository/
├─ TrackRepository.kt
└─ MediaStoreTrackRepository.kt

ui/
├─ PlayerViewModel.kt
├─ TrackListView.kt
├─ SearchOverlay.kt
├─ SearchBar.kt
├─ TransportControls.kt
└─ theme/

player/
├─ Player.kt
├─ ExoPlayerPlayer.kt
├─ PlayerRepository.kt
└─ PlaybackState.kt

media/
├─ PlayerService.kt
├─ BrowserService.kt
└─ MediaButtonReceiver.kt

di/
├─ RepositoryModule.kt
└─ PlayerModule.kt

MainActivity.kt
MinstrelApplication.kt
```

---

## Key Takeaways

1. **Architecture is Clean**: MVVM with clear separation of concerns
2. **Reactive by Design**: All state flows through StateFlow and Coroutines
3. **Scalable**: Adding sort/group fits perfectly with existing patterns
4. **Well-Tested**: Use patterns from existing tests as templates
5. **Type-Safe**: Sealed classes and Kotlin's type system prevent errors
6. **Maintainable**: Clear naming, organized structure, comprehensive Hilt DI

Your implementation will follow the exact same patterns already established in the codebase!

