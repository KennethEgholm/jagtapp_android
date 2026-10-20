package dk.jagtfokus.app.jagtbart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dk.jagtfokus.core.BagEntry
import dk.jagtfokus.core.BagFileStore
import dk.jagtfokus.core.BagJournal
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.SeasonCalendar
import dk.jagtfokus.core.SpeciesChecklist
import dk.jagtfokus.core.SpeciesChecklistFileStore
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * Date and place for the Journal tab only. Does not change Jagtbart.
 * Port of BagDraft.swift.
 */
class BagDraft {
    var followToday by mutableStateOf(true)
        private set
    var pickedDate by mutableStateOf(LocalDate.now(SeasonCalendar.ZONE))
        private set
    var followHere by mutableStateOf(true)
        private set
    var pickedPlace by mutableStateOf<Place?>(null)
        private set

    fun date(now: LocalDate): LocalDate = if (followToday) now else pickedDate

    /** Nil while following here and the position is not found. A picked place is stored. */
    fun placeId(gpsPlace: Place?, state: LocationState): String? =
        if (followHere) gpsPlace?.takeIf { state == LocationState.Found }?.id else pickedPlace?.id

    fun place(gpsPlace: Place?, state: LocationState, denmark: Place): Place =
        when {
            followHere && gpsPlace != null && state == LocationState.Found -> gpsPlace
            followHere -> denmark
            else -> pickedPlace ?: denmark
        }

    fun select(date: LocalDate, now: LocalDate) {
        followToday = date == now
        pickedDate = date
    }

    fun useHere() {
        followHere = true
    }

    fun select(place: Place) {
        followHere = false
        pickedPlace = place
    }
}

/**
 * Local bag with rolling-backup file storage. No cloud: the journal stays on
 * the phone. Port of BagStore.swift, without the iCloud key-value layer.
 */
class BagStore(filesDir: File) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val writer: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val fileStore = BagFileStore(File(filesDir, "vildtudbytte.json"))
    private val checklistStore = SpeciesChecklistFileStore(File(filesDir, "artsliste.json"))
    private val scope = CoroutineScope(writer)

    var journal by mutableStateOf(BagJournal())
        private set
    /** Species taken at least once. Independent of individual journal rows. */
    var speciesList by mutableStateOf(SpeciesChecklist())
        private set
    /** Entry the confirmation refers to. */
    var pendingId by mutableStateOf<String?>(null)
        private set
    /** Bumped when a swipe action or species page records from another tab. */
    var tabRequests by mutableStateOf(0)
        private set
    /** Bumped on every note and +1, so the screen can give haptic feedback. */
    var saves by mutableStateOf(0)
        private set

    val draft = BagDraft()

    init {
        // Two small files. Failures fall back to an empty bag rather than crash the start.
        val loaded = runCatching { fileStore.load() }.getOrDefault(BagJournal())
        journal = loaded
        speciesList = if (checklistStore.fileExists) {
            runCatching { checklistStore.load() }.getOrNull() ?: SpeciesChecklist()
        } else {
            SpeciesChecklist().seededFrom(loaded).also { persistChecklist(it) }
        }
    }

    val pending: BagEntry?
        get() = pendingId?.let { journal.entry(it) }

    fun record(speciesId: String, date: LocalDate, placeId: String?, now: Instant = Instant.now()) {
        val saved = journal.record(speciesId, date, placeId, now)
        journal = saved
        speciesList = speciesList.add(speciesId)
        pendingId = saved.entries.lastOrNull()?.id
        saves += 1
        persist(saved)
        persistChecklist()
    }

    fun recordAndSwitchTab(speciesId: String, date: LocalDate, placeId: String?) {
        record(speciesId, date, placeId)
        tabRequests += 1
    }

    fun incrementPending() {
        val id = pendingId ?: return
        val saved = journal.increment(id, Instant.now())
        journal = saved
        saves += 1
        persist(saved)
    }

    fun undoPending() {
        val id = pendingId ?: return
        val saved = journal.remove(id)
        journal = saved
        pendingId = null
        persist(saved)
    }

    fun clearPending() {
        pendingId = null
    }

    fun update(id: String, date: LocalDate, count: Int, placeId: String?) {
        val saved = journal.update(id, date.toString(), count, placeId, Instant.now())
        journal = saved
        persist(saved)
    }

    /** Manual mark. A later journal note puts a removed species back. */
    fun setSpecies(id: String, included: Boolean) {
        speciesList = if (included) speciesList.add(id) else speciesList.remove(id)
        persistChecklist()
    }

    fun removeSpecies(id: String) {
        speciesList = speciesList.remove(id)
        persistChecklist()
    }

    fun remove(id: String) {
        val saved = journal.remove(id)
        journal = saved
        if (pendingId == id) pendingId = null
        persist(saved)
    }

    /** Puts a deleted entry back, for Fortryd. */
    fun restore(entry: BagEntry) {
        val saved = journal.restore(entry)
        journal = saved
        persist(saved)
    }

    // One writer thread runs the jobs in the order they were queued, so the
    // last write is always the latest state.
    private fun persist(saved: BagJournal) {
        scope.launch { runCatching { fileStore.save(saved) } }
    }

    private fun persistChecklist(list: SpeciesChecklist = speciesList) {
        scope.launch { runCatching { checklistStore.save(list) } }
    }
}
