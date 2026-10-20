package dk.jagtfokus.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonTimelineInitialsTest {
    @Test
    fun monthRowStartsInApril() {
        val timeline = SeasonTimeline()
        assertEquals("AMJJASONDJFM", timeline.months.joinToString("") { timeline.initial(it) })
    }
}
