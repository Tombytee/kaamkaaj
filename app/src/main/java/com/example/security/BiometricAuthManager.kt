package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class AuthCapability {
    BIOMETRIC_AND_CREDENTIAL,
    CREDENTIAL_ONLY,
    NONE_ENROLLED,
    UNSUPPORTED
}

data class AuthInfo(
    val capability: AuthCapability,
    val title: String,
    val description: String
)

object BiometricAuthManager {

    private var cachedAuthInfo: AuthInfo? = null

    /**
     * Determines whether the device supports biometric auth, credential auth, or neither.
     */
    fun checkAuthCapability(context: Context): AuthCapability {
        val biometricManager = BiometricManager.from(context)
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        val hasBiometrics = biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        val hasDeviceLock = keyguardManager?.isDeviceSecure == true || 
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && biometricManager.canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS)

        return when {
            hasBiometrics -> AuthCapability.BIOMETRIC_AND_CREDENTIAL
            hasDeviceLock -> AuthCapability.CREDENTIAL_ONLY
            biometricManager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthCapability.NONE_ENROLLED
            keyguardManager != null && !keyguardManager.isKeyguardSecure -> AuthCapability.NONE_ENROLLED
            else -> AuthCapability.UNSUPPORTED
        }
    }

    fun isAuthAvailable(context: Context): Boolean {
        val capability = getAuthInfo(context).capability
        return capability == AuthCapability.BIOMETRIC_AND_CREDENTIAL || capability == AuthCapability.CREDENTIAL_ONLY
    }

    fun getAuthInfo(context: Context, forceRefresh: Boolean = false): AuthInfo {
        if (!forceRefresh && cachedAuthInfo != null) {
            return cachedAuthInfo!!
        }
        val capability = checkAuthCapability(context)
        val title = when (capability) {
            AuthCapability.BIOMETRIC_AND_CREDENTIAL -> "Biometrics & Device PIN / Pattern"
            AuthCapability.CREDENTIAL_ONLY -> "Device PIN / Pattern / Password"
            AuthCapability.NONE_ENROLLED -> "No Screen Lock Configured"
            AuthCapability.UNSUPPORTED -> "Security Hardware Unavailable"
        }
        val description = when (capability) {
            AuthCapability.BIOMETRIC_AND_CREDENTIAL ->
                "Fingerprint or Face Unlock with Device PIN/Pattern fallback is active."
            AuthCapability.CREDENTIAL_ONLY ->
                "Biometric hardware not enrolled or unavailable. App Lock will authenticate using your device PIN, Pattern, or Password."
            AuthCapability.NONE_ENROLLED ->
                "No screen lock or biometric is set up on this device. Please configure a Screen Lock in Android Settings to activate App Lock."
            AuthCapability.UNSUPPORTED ->
                "This device does not support secure device credentials or biometric authentication."
        }
        val info = AuthInfo(capability, title, description)
        cachedAuthInfo = info
        return info
    }

    fun getAuthMethodTitle(context: Context): String = getAuthInfo(context).title

    fun getAuthMethodDescription(context: Context): String = getAuthInfo(context).description

    /**
     * Prompts the user with BiometricPrompt (or device credentials fallback).
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock KaamKaaj",
        subtitle: String = "Authenticate to access your private records",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                AppLockManager.isAuthPromptActive = false
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                AppLockManager.isAuthPromptActive = false
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        // Allowed authenticators: BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        // Note: When DEVICE_CREDENTIAL is used, setNegativeButtonText MUST NOT be called!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(
                BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            )
        } else {
            promptInfoBuilder.setAllowedAuthenticators(
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            )
        }

        try {
            AppLockManager.isAuthPromptActive = true
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Fallback for edge cases on specific Android 9/10 vendor implementations
            try {
                val fallbackPrompt = BiometricPrompt(activity, executor, callback)
                val fallbackInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Cancel")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                    .build()
                fallbackPrompt.authenticate(fallbackInfo)
            } catch (fallbackError: Exception) {
                AppLockManager.isAuthPromptActive = false
                onError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, fallbackError.localizedMessage ?: "Authentication failed")
            }
        }
    }
}
