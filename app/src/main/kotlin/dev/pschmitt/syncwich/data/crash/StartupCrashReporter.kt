package dev.pschmitt.syncwich.data.crash

import android.content.Context
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.system.exitProcess

private const val MAX_REPORT_LENGTH = 16_384

/** A sanitized crash captured before the normal Compose UI became available. */
@Immutable data class StartupCrashReport(val capturedAt: Long, val details: String)

/**
 * Persists the last uncaught startup exception synchronously so it survives process death. The
 * report deliberately contains no preferences, request headers, or arbitrary application state;
 * throwable text is redacted before it is written to disk.
 */
@Singleton
class StartupCrashReporter @Inject constructor(@ApplicationContext private val context: Context) {

    fun pending(): StartupCrashReport? {
        val preferences = preferences()
        val capturedAt = preferences.getLong(KEY_CAPTURED_AT, 0L)
        val details = preferences.getString(KEY_DETAILS, null)?.takeIf(String::isNotBlank)
        return if (capturedAt > 0L && details != null) {
            StartupCrashReport(capturedAt, details)
        } else {
            null
        }
    }

    fun clear() {
        preferences().edit().clear().apply()
    }

    private fun preferences() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES_NAME = "startup_crash_report"
        private const val KEY_CAPTURED_AT = "captured_at"
        private const val KEY_DETAILS = "details"
        private val installed = AtomicBoolean(false)

        /** Installs before Hilt/WorkManager setup so early application crashes are captured too. */
        fun install(context: Context) {
            if (!installed.compareAndSet(false, true)) return

            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching {
                    context
                        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_CAPTURED_AT, System.currentTimeMillis())
                        .putString(KEY_DETAILS, formatStartupCrash(throwable))
                        // A crash handler must finish writing before Android kills the process.
                        .commit()
                }
                if (previousHandler != null) {
                    previousHandler.uncaughtException(thread, throwable)
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(10)
                }
            }
        }
    }
}

internal fun formatStartupCrash(throwable: Throwable): String {
    val output = StringWriter()
    throwable.printStackTrace(PrintWriter(output))
    return sanitizeCrashText(output.toString()).take(MAX_REPORT_LENGTH)
}

internal fun sanitizeCrashText(text: String): String =
    text
        .replace(URL_PATTERN) { "<redacted-url>" }
        .replace(SECRET_PATTERN) { match ->
            "${match.groupValues[1]}${match.groupValues[2]}<redacted>"
        }

private val URL_PATTERN = Regex("""https?://[^\s"'<>)]*""")
private val SECRET_PATTERN =
    Regex(
        """(?i)\b(authorization|api[-_ ]?token|password|secret)\b(\s*[:=]\s*)(?:bearer\s+)?[^\s,;]+"""
    )
