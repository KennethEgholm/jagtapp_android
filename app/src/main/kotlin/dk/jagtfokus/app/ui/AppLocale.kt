package dk.jagtfokus.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dk.jagtfokus.app.R
import java.util.Locale

/**
 * The language the app actually shows: "da" or "en", read from the resource that was picked.
 * A German phone gets the Danish strings, so it must get Danish dates too, never German ones.
 * iOS: `Localization.swift`.
 */
@Composable
fun appLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.locale_tag))
