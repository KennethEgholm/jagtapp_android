package dk.jagtfokus.app

import android.app.Application
import dk.jagtfokus.app.jagtbart.BagStore

/**
 * Holds what must live once per process. iOS keeps one `BagStore` in `JagtFokusApp`;
 * an activity that is rebuilt on rotation must not build a second one.
 */
class JagtFokusApp : Application() {
    val bag: BagStore by lazy { BagStore(filesDir) }
}
