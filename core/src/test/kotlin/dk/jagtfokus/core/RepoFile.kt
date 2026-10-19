package dk.jagtfokus.core

import java.io.File

/**
 * Repo files for tests that pin the bundled data. Works from `:core` and the root,
 * like CatalogFixture's dual paths.
 */
fun repoFile(relative: String): File =
    listOf(File(relative), File("..", relative)).firstOrNull { it.isFile }
        ?: error("Missing $relative")
