package dk.jagtfokus.app.ui

import java.util.Locale

/** "mandag 19. oktober" → "Mandag 19. oktober". */
internal fun String.capitalizedFirst(locale: Locale): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
