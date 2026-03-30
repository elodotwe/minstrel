# Sorting & Grouping Feature - Implementation Guide

## Quick Reference: Key Files to Modify

```
Priority 1 (Core Logic):
├─ data/TrackListState.kt          ← Add sort/group state
├─ data/SortOrder.kt               ← NEW: Define sort options
├─ data/GroupBy.kt                 ← NEW: Define group options
├─ ui/PlayerViewModel.kt           ← Add sort/group state flows & actions
├─ repository/TrackRepository.kt    ← Update interface signature
└─ repository/MediaStoreTrackRepository.kt ← Implement sorting/grouping logic

Priority 2 (UI):
├─ ui/TrackListView.kt             ← Update to display grouped tracks
├─ ui/SortingMenu.kt               ← NEW: Sort/group selector UI
└─ MainActivity.kt                 ← Add sort/group controls

Priority 3 (Polish):
├─ Tests                           ← Unit tests for sorting logic
└─ Previews                        ← Update @Preview functions
```

---

## Implementation Flow

### Step 1: Define Sort/Group Options (NEW FILES)

**File**: `data/SortOrder.kt`
```kotlin
enum class SortOrder {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
    ARTIST_DESC,
    ALBUM_ASC,
    ALBUM_DESC,
    FILENAME_ASC,
    FILENAME_DESC,
    DATE_ADDED_DESC,  // If available in metadata
    // etc.
}
```

**File**: `data/GroupBy.kt`
```kotlin
enum class GroupBy {
    ARTIST,
    ALBUM,
    DIRECTORY,
    NONE
}
```

### Step 2: Update TrackListState

**File**: `data/TrackListState.kt`

BEFORE:
```kotlin
sealed interface TrackListState {
    data object Loading : TrackListState
    data class Success(val tracks: List<Track>) : TrackListState
    data object MissingPermissions : TrackListState
}
```

AFTER:
```kotlin
sealed interface TrackListState {
    data object Loading : TrackListState
    data class Success(
        val tracks: List<Track>,
        val sortOrder: SortOrder = SortOrder.FILENAME_ASC,
        val groupBy: GroupBy? = null
    ) : TrackListState
    data object MissingPermissions : TrackListState
}

// For grouped display:
data class TrackGroup(
    val header: String,  // e.g., "Artist Name" or "Album Name"
    val tracks: List<Track>
)
```

### Step 3: Update TrackRepository Interface

**File**: `repository/TrackRepository.kt`

BEFORE:
```kotlin
interface TrackRepository {
    fun getTracks(filter: String? = null): Flow<TrackListState>
}
```

AFTER:
```kotlin
interface TrackRepository {
    fun getTracks(
        filter: String? = null,
        sortOrder: SortOrder = SortOrder.FILENAME_ASC,
        groupBy: GroupBy? = null
    ): Flow<TrackListState>
}
```

### Step 4: Update MediaStoreTrackRepository

**File**: `repository/MediaStoreTrackRepository.kt`

Changes needed:
1. Update function signature
2. Pass sortOrder to query() method
3. Implement sorting logic in query()
4. Return tracks in TrackListState.Success with sort/group info

```kotlin
override fun getTracks(
    filter: String?,
    sortOrder: SortOrder,
    groupBy: GroupBy?
): Flow<TrackListState> = callbackFlow {
    // ... existing observer code ...
    
    if (ContextCompat.checkSelfPermission(...) != PERMISSION_GRANTED) {
        trySend(TrackListState.MissingPermissions)
    } else {
        // ... existing registration code ...
        trySend(TrackListState.Success(
            tracks = query(filter, sortOrder),  // Pass sortOrder
            sortOrder = sortOrder,
            groupBy = groupBy
        ))
    }
    // ... existing awaitClose ...
}

private fun query(
    filter: String?,
    sortOrder: SortOrder
): List<Track> {
    val trackList = mutableListOf<Track>()
    // ... existing query setup ...
    
    // MODIFY: Change sortOrder based on parameter
    val sqlSortOrder = when (sortOrder) {
        SortOrder.TITLE_ASC -> "${MediaStore.Audio.Media.TITLE} ASC"
        SortOrder.TITLE_DESC -> "${MediaStore.Audio.Media.TITLE} DESC"
        SortOrder.ARTIST_ASC -> "${MediaStore.Audio.Media.ARTIST} ASC"
        SortOrder.ARTIST_DESC -> "${MediaStore.Audio.Media.ARTIST} DESC"
        // ... etc
        else -> "${MediaStore.Audio.Media.DATA} ASC"  // default
    }
    
    // ... use sqlSortOrder in query ...
    return trackList
}
```

### Step 5: Update PlayerViewModel

**File**: `ui/PlayerViewModel.kt`

Add new state and actions:

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {
    
    // ... existing code ...
    
    // NEW: Sort and Group state
    private val sortOrder = MutableStateFlow<SortOrder>(SortOrder.FILENAME_ASC)
    private val groupBy = MutableStateFlow<GroupBy?>(null)
    
    // Expose as StateFlow
    val currentSortOrder: StateFlow<SortOrder> = sortOrder.asStateFlow()
    val currentGrouping: StateFlow<GroupBy?> = groupBy.asStateFlow()
    
    // MODIFY: Existing tracks flow
    val tracks: StateFlow<TrackListState> = combine(
        searchQuery,
        sortOrder,
        groupBy
    ) { query, sort, group ->
        Triple(query, sort, group)
    }.flatMapLatest { (query, sort, group) ->
        trackRepository.getTracks(query, sort, group)  // Pass sort/group
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrackListState.Loading
    )
    
    // NEW: Actions
    fun onSortOrderChanged(sortOrder: SortOrder) {
        this.sortOrder.value = sortOrder
    }
    
    fun onGroupingChanged(groupBy: GroupBy?) {
        this.groupBy.value = groupBy
    }
}
```

### Step 6: Update TrackListView UI

**File**: `ui/TrackListView.kt`

For grouped display:

```kotlin
@Composable
fun TrackList(
    trackListState: TrackListState,
    playbackState: PlaybackState,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (trackListState) {
            // ... Loading and MissingPermissions cases ...
            
            is TrackListState.Success -> {
                // IF grouped, transform tracks
                val displayItems: List<Any> = if (trackListState.groupBy != null) {
                    createGroupedItems(trackListState.tracks, trackListState.groupBy)
                } else {
                    trackListState.tracks
                }
                
                LazyColumn(/* ... */) {
                    items(displayItems) { item ->
                        when (item) {
                            is String -> {
                                // Group header
                                GroupHeader(text = item)
                            }
                            is Track -> {
                                // Track item (existing code)
                                TrackItem(
                                    track = item,
                                    isPlaying = item == currentTrack,
                                    onSelected = { onTrackSelected(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to create grouped display
private fun createGroupedItems(
    tracks: List<Track>,
    groupBy: GroupBy
): List<Any> {
    return when (groupBy) {
        GroupBy.ARTIST -> {
            tracks.groupBy { it.artist ?: "Unknown Artist" }
                .flatMap { (artist, groupedTracks) ->
                    listOf(artist) + groupedTracks
                }
        }
        GroupBy.ALBUM -> {
            tracks.groupBy { it.album ?: "Unknown Album" }
                .flatMap { (album, groupedTracks) ->
                    listOf(album) + groupedTracks
                }
        }
        GroupBy.DIRECTORY -> {
            tracks.groupBy { it.directory }
                .flatMap { (dir, groupedTracks) ->
                    listOf(dir) + groupedTracks
                }
        }
        GroupBy.NONE -> tracks
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    )
}
```

### Step 7: Add Sort/Group UI Controls (NEW FILE)

**File**: `ui/SortingMenu.kt`

```kotlin
@Composable
fun SortingMenu(
    currentSortOrder: SortOrder,
    currentGroupBy: GroupBy?,
    onSortOrderChanged: (SortOrder) -> Unit,
    onGroupByChanged: (GroupBy?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Implement as dropdown menu or bottom sheet
    // Show options for sorting and grouping
}
```

### Step 8: Update MainActivity

**File**: `MainActivity.kt`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
        MinstrelTheme {
            val trackListState by viewModel.tracks.collectAsStateWithLifecycle()
            val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
            val sortOrder by viewModel.currentSortOrder.collectAsStateWithLifecycle()
            val groupBy by viewModel.currentGrouping.collectAsStateWithLifecycle()
            // ... other states ...
            
            Scaffold(
                // ... existing topBar ...
                floatingActionButton = {
                    // Could add sort/group menu here or in topBar
                    FloatingActionButton(
                        onClick = { showSortingMenu = true }
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                },
                // ... rest of scaffold ...
            )
            
            if (showSortingMenu) {
                SortingMenu(
                    currentSortOrder = sortOrder,
                    currentGroupBy = groupBy,
                    onSortOrderChanged = { viewModel.onSortOrderChanged(it) },
                    onGroupByChanged = { viewModel.onGroupingChanged(it) },
                )
            }
        }
    }
}
```

---

## Testing Strategy

### Unit Tests

**File**: `app/src/test/java/.../repository/MediaStoreTrackRepositoryTest.kt`

```kotlin
class MediaStoreTrackRepositoryTest {
    
    @Test
    fun testSortByTitleAscending() {
        // Create test tracks with different titles
        // Verify they're returned in correct order
    }
    
    @Test
    fun testSortByArtistDescending() {
        // Test descending sort
    }
    
    @Test
    fun testGroupByArtist() {
        // Verify grouping logic
    }
}
```

---

## Backward Compatibility

Since existing code calls `getTracks()` without sort params:

```kotlin
// Add default parameters to maintain compatibility
interface TrackRepository {
    fun getTracks(
        filter: String? = null,
        sortOrder: SortOrder = SortOrder.FILENAME_ASC,  // Default
        groupBy: GroupBy? = null                        // Default
    ): Flow<TrackListState>
}
```

All existing calls will work unchanged, using default sorting.

---

## Performance Considerations

1. **Sorting in Repository**: Happens once during query
   - Better: MediaStore SQL handles it
   - Alternative: Sort in Kotlin after fetching (slower for large libraries)

2. **Grouping in UI**: Happens during recomposition
   - Consider memoizing grouped results if list is large
   - Use `.stateIn()` to cache grouped display items

3. **Avoid Recomposition**:
   - Group headers are stable Strings
   - Pass sort/group as parameters to Composables (stable types)

---

## Related Code Patterns Already Used

### Similar to SearchQuery:
```kotlin
private val searchQuery = MutableStateFlow<String?>(null)

val tracks: StateFlow<TrackListState> = searchQuery.flatMapLatest { query ->
    trackRepository.getTracks(query)
}
```

YOUR IMPLEMENTATION FOLLOWS SAME PATTERN:
```kotlin
private val sortOrder = MutableStateFlow<SortOrder>(SortOrder.FILENAME_ASC)
private val groupBy = MutableStateFlow<GroupBy?>(null)

val tracks: StateFlow<TrackListState> = combine(
    searchQuery, sortOrder, groupBy
).flatMapLatest { (query, sort, group) ->
    trackRepository.getTracks(query, sort, group)
}
```

### Similar to isNextEnabled:
```kotlin
val isNextEnabled: StateFlow<Boolean> = combine(tracks, playbackState) { ... }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), false)
```

Use same pattern for your new StateFlows.

