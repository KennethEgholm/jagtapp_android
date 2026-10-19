package dk.jagtfokus.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

/** One decoder config for the bundled data files. */
internal val catalogJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class HuntingCatalog(
    val version: Int,
    val updated: String,
    @SerialName("source_name") val sourceName: String,
    @SerialName("source_url") val sourceURL: String,
    val groups: List<SpeciesGroup>,
    val places: List<Place> = emptyList(),
    val overrides: List<LocalOverride> = emptyList(),
) {
    val allSpecies: List<Species>
        get() = groups.flatMap { it.species }

    companion object {
        fun decode(bytes: ByteArray): HuntingCatalog =
            catalogJson.decodeFromString(bytes.decodeToString())

        fun encode(catalog: HuntingCatalog): ByteArray =
            catalogJson.encodeToString(catalog).encodeToByteArray()
    }
}

@Serializable
data class SpeciesGroup(
    val id: String,
    val title: String,
    val species: List<Species>,
)

@Serializable
data class Species(
    val id: String,
    val name: String,
    val periods: List<SeasonPeriod>,
    val note: String? = null,
) {
    /** Red deer or fallow deer. They get the note about local management plans. Display only. */
    val isRedOrFallowDeer: Boolean
        get() = id.startsWith("kron") || id.startsWith("daa")
}

@Serializable
data class SeasonPeriod(
    val from: MonthDay,
    val to: MonthDay,
)

@Serializable(with = MonthDaySerializer::class)
data class MonthDay(val month: Int, val day: Int) : Comparable<MonthDay> {
    val formatted: String
        get() = "%02d-%02d".format(month, day)

    override fun compareTo(other: MonthDay): Int {
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        fun parse(raw: String): MonthDay? {
            val parts = raw.split("-")
            if (parts.size != 2) return null
            val month = parts[0].toIntOrNull() ?: return null
            val day = parts[1].toIntOrNull() ?: return null
            if (month !in 1..12 || day !in 1..31) return null
            return MonthDay(month, day)
        }
    }
}

object MonthDaySerializer : KSerializer<MonthDay> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MonthDay", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MonthDay) {
        encoder.encodeString(value.formatted)
    }

    override fun deserialize(decoder: Decoder): MonthDay {
        val raw = decoder.decodeString()
        return MonthDay.parse(raw)
            ?: throw SerializationException("Ugyldig MM-DD: $raw")
    }
}
