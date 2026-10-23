package com.example.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLockManager {
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L
    var isAuthPromptActive: Boolean = false

    fun initOnStart(appLockEnabled: Boolean) {
        if (appLockEnabled) {
            _isLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded(appLockEnabled: Boolean, timeoutSeconds: Int) {
        if (!appLockEnabled) {
            _isLocked.value = false
            return
        }

        // If already locked, remain locked
        if (_isLocked.value) return

        // If never set before (e.g. cold start), lock immediately
        if (lastBackgroundTimestamp == 0L) {
            _isLocked.value = true
            return
        }

        val backgroundDuration = System.currentTimeMillis() - lastBackgroundTimestamp
        val timeoutMillis = timeoutSeconds * 1000L

        // If background duration exceeds timeout, lock the app
        if (backgroundDuration >= timeoutMillis) {
            _isLocked.value = true
        }
    }

    fun lockNow() {
        _isLocked.value = true
    }

    fun unlock() {
        _isLocked.value = false
        lastBackgroundTimestamp = 0L
    }
}
