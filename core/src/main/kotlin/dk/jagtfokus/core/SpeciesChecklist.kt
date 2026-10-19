package dk.jagtfokus.core

import kotlinx.serialization.Serializable

/** Species the hunter has taken at least once. One id each. Not a count. */
@Serializable
data class SpeciesChecklist(
    val ids: Set<String> = emptySet(),
) {
    fun contains(id: String): Boolean = id in ids

    fun add(id: String): SpeciesChecklist = copy(ids = ids + id)

    fun remove(id: String): SpeciesChecklist = copy(ids = ids - id)

    /** First launch only. Later journal deletions must not empty a saved list. */
    fun seededFrom(journal: BagJournal): SpeciesChecklist =
        copy(ids = ids + journal.entries.map { it.speciesId })

    companion object {
        fun decode(bytes: ByteArray): SpeciesChecklist =
            catalogJson.decodeFromString(bytes.decodeToString())

        fun encode(list: SpeciesChecklist): ByteArray =
            catalogJson.encodeToString(list).encodeToByteArray()
    }
}
