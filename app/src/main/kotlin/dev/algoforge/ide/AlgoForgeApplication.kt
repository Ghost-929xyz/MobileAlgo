package dev.algoforge.ide

import android.app.Application
import dev.algoforge.ide.data.AppContainer

class AlgoForgeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
