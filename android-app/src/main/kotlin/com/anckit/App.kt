package com.anckit

import android.app.Application
import anc.base.Logging
import anc.core.Framework
import anc.modules.ModuleRegistry
import anc.ui.android.viewmodel.FrameworkViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            modules(module {
                viewModel { FrameworkViewModel() }
            })
        }

        // Point the logging facade at Android's Logcat
        Logging.printer = object : Logging.Printer {
            override fun log(level: Logging.Level, message: String, throwable: Throwable?) {
                val tag = "AncKit"
                when (level) {
                    Logging.Level.VERBOSE -> android.util.Log.v(tag, message, throwable)
                    Logging.Level.DEBUG   -> android.util.Log.d(tag, message, throwable)
                    Logging.Level.INFO    -> android.util.Log.i(tag, message, throwable)
                    Logging.Level.WARN    -> android.util.Log.w(tag, message, throwable)
                    Logging.Level.ERROR   -> android.util.Log.e(tag, message, throwable)
                }
            }
        }

        // Initialize the framework with all built-in modules
        try {
            Framework.initialize(ModuleRegistry.all)
            Logging.i("AncKit started — ${Framework.getInstance().moduleManager.count} modules ready")
        } catch (e: Exception) {
            Logging.e("Framework initialization failed: ${e.message}", e)
        }
    }
}
