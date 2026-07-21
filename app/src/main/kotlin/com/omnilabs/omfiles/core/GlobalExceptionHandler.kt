package com.omnilabs.omfiles.core

import android.content.Context
import android.content.SharedPreferences
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global exception handler that catches uncaught exceptions,
 * logs them, and prevents a silent crash by persisting crash info
 * to be displayed on the next app launch.
 */
class GlobalExceptionHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val PREFS_NAME = "crash_prefs"
        private const val KEY_CRASH_EXISTS = "crash_exists"
        private const val KEY_CRASH_MESSAGE = "crash_message"
        private const val KEY_CRASH_STACK = "crash_stack"
        private const val KEY_CRASH_TIME = "crash_time"
        private const val KEY_CRASH_THREAD = "crash_thread"

        /**
         * Check if a previous crash was recorded.
         */
        fun hasCachedCrash(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_CRASH_EXISTS, false)
        }

        /**
         * Get the last crash details.
         */
        fun getCachedCrash(context: Context): CrashInfo? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_CRASH_EXISTS, false)) return null
            return CrashInfo(
                message = prefs.getString(KEY_CRASH_MESSAGE, "Unknown error") ?: "Unknown error",
                stackTrace = prefs.getString(KEY_CRASH_STACK, "") ?: "",
                time = prefs.getString(KEY_CRASH_TIME, "") ?: "",
                threadName = prefs.getString(KEY_CRASH_THREAD, "") ?: ""
            )
        }

        /**
         * Clear the cached crash info after displaying it.
         */
        fun clearCachedCrash(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_CRASH_EXISTS, false)
                .remove(KEY_CRASH_MESSAGE)
                .remove(KEY_CRASH_STACK)
                .remove(KEY_CRASH_TIME)
                .remove(KEY_CRASH_THREAD)
                .apply()
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Format the crash info
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val crashTime = timeFormat.format(Date())

            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            printWriter.flush()
            val stackTrace = stringWriter.toString()

            val message = throwable.message ?: throwable.javaClass.simpleName

            // Save crash info to SharedPreferences
            prefs.edit()
                .putBoolean(KEY_CRASH_EXISTS, true)
                .putString(KEY_CRASH_MESSAGE, message)
                .putString(KEY_CRASH_STACK, stackTrace)
                .putString(KEY_CRASH_TIME, crashTime)
                .putString(KEY_CRASH_THREAD, thread.name)
                .apply()

        } catch (_: Exception) {
            // Don't let the handler itself cause issues
        }

        // Pass to the default handler (which will crash the app)
        // but we have saved the crash info so it will be shown on next launch
        defaultHandler?.uncaughtException(thread, throwable)
    }

    data class CrashInfo(
        val message: String,
        val stackTrace: String,
        val time: String,
        val threadName: String
    )
}
