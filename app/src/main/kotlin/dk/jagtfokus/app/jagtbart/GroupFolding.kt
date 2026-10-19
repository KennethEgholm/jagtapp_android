package dk.jagtfokus.app.jagtbart

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * "Sammenfold grupper". On: Jagtbart and Alle arter start with groups collapsed.
 * One shared preference, like iOS' @AppStorage.
 */
internal class GroupFolding(context: Context) {
    private val prefs = context.getSharedPreferences("jagtapp", Context.MODE_PRIVATE)
    var collapse by mutableStateOf(prefs.getBoolean("sammenfoldGrupper", true))
        private set

    fun set(value: Boolean) {
        collapse = value
        prefs.edit().putBoolean("sammenfoldGrupper", value).apply()
    }
}
