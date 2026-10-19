package dk.jagtfokus.app.jagtbart

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dk.jagtfokus.app.data.BagFileStore
import dk.jagtfokus.app.data.SpeciesChecklistFileStore
import dk.jagtfokus.core.BagEntry
import dk.jagtfokus.core.BagJournal
import dk.jagtfokus.core.Place
import dk.jagtfokus.core.SpeciesChecklist
import java.io.File
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Date and place for the Journal tab only. Does not change Jagtbart.
 * Port of BagDraft.swift.
 */
class BagDraft {
    var followToday by mutableStateOf(true)
        private set
    var pickedDate by mutableStateOf(LocalDate.now())
        private set
    var followHere by mutableStateOf(true)
        private set
    var pickedPlace by mutableStateOf<Place?>(null)
        private set

    fun date(now: LocalDate): LocalDate = if (followToday) now else pickedDate

    /** Nil while following here and GPS has not landed. A picked place is stored. */
    fun placeId(gpsPlace: Place?): String? =
        if (followHere) gpsPlace?.id else pickedPlace?.id

    fun place(now: LocalDate, gpsPlace: Place?, denmark: Place): Place =
        when {
            followHere && gpsPlace != null -> gpsPlace
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
    private val fileStore = BagFileStore(File(filesDir, "vildtudbytte.json"))
    private val checklistStore = SpeciesChecklistFileStore(File(filesDir, "artsliste.json"))
    private val scope = CoroutineScope(Dispatchers.IO)

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

    val draft = BagDraft()

    init {
        val loaded = fileStore.load()
        journal = loaded
        speciesList = if (checklistStore.fileExists) {
            checklistStore.load() ?: SpeciesChecklist()
        } else {
            SpeciesChecklist().seededFrom(loaded).also { checklistStore.save(it) }
        }
    }

    val pending: BagEntry?
        get() = pendingId?.let { journal.entry(it) }

    fun record(speciesId: String, date: LocalDate, placeId: String?, now: Instant = Instant.now()) {
        val saved = journal.record(speciesId, date, placeId, now)
        journal = saved
        speciesList = speciesList.add(speciesId)
        pendingId = saved.entries.lastOrNull()?.id
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

    private fun persist(saved: BagJournal) {
        scope.launch { fileStore.save(saved) }
    }

    private fun persistChecklist() {
        val current = speciesList
        scope.launch { checklistStore.save(current) }
    }
}
