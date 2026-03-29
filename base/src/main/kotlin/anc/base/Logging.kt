package anc.base

/**
 * Thin logging facade. On Android, swap the printer for Timber.
 * On JVM, defaults to stdout/stderr.
 */
object Logging {
    var printer: Printer = DefaultPrinter()

    fun v(message: String) = printer.log(Level.VERBOSE, message)
    fun d(message: String) = printer.log(Level.DEBUG, message)
    fun i(message: String) = printer.log(Level.INFO, message)
    fun w(message: String) = printer.log(Level.WARN, message)
    fun e(message: String, throwable: Throwable? = null) = printer.log(Level.ERROR, message, throwable)

    enum class Level { VERBOSE, DEBUG, INFO, WARN, ERROR }

    interface Printer {
        fun log(level: Level, message: String, throwable: Throwable? = null)
    }

    class DefaultPrinter : Printer {
        override fun log(level: Level, message: String, throwable: Throwable?) {
            val tag = "[AncKit/${level.name[0]}]"
            if (level == Level.ERROR || level == Level.WARN) {
                System.err.println("$tag $message")
                throwable?.printStackTrace(System.err)
            } else {
                println("$tag $message")
            }
        }
    }
}
