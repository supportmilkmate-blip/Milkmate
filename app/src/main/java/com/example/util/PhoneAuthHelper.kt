package com.example.util

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Helper object providing real Firebase Phone SMS OTP authentication
 * with automatic SMS code retrieval and fallback callback handlers.
 */
object PhoneAuthHelper {
    private const val TAG = "PhoneAuthHelper"

    sealed class PhoneAuthResult {
        data class CodeSent(val verificationId: String, val token: PhoneAuthProvider.ForceResendingToken) : PhoneAuthResult()
        data class AutoVerified(val user: FirebaseUser) : PhoneAuthResult()
        data class Error(val message: String) : PhoneAuthResult()
    }

    /**
     * Dispatches real SMS OTP via Google Firebase Auth to user's mobile number.
     */
    fun sendSmsOtp(
        activity: Activity,
        fullPhoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onResult: (PhoneAuthResult) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val formattedNumber = if (fullPhoneNumber.startsWith("+")) fullPhoneNumber else "+91$fullPhoneNumber"

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
                // Sign in with credential automatically
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result?.user != null) {
                            onResult(PhoneAuthResult.AutoVerified(task.result!!.user!!))
                        } else {
                            val msg = task.exception?.localizedMessage ?: "Auto-verification failed"
                            onResult(PhoneAuthResult.Error(msg))
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                val cleanMessage = when {
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "SMS quota exceeded. Please try WhatsApp OTP verification."
                    e.message?.contains("invalid", ignoreCase = true) == true ->
                        "Invalid mobile number format. Please check the 10-digit number."
                    e.message?.contains("app-not-authorized", ignoreCase = true) == true ->
                        "App verification required. Use WhatsApp OTP or Google 1-Tap sign-in."
                    else -> e.localizedMessage ?: "Failed to send SMS OTP. Please check your internet connection."
                }
                onResult(PhoneAuthResult.Error(cleanMessage))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent: verificationId=$verificationId")
                onResult(PhoneAuthResult.CodeSent(verificationId, token))
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /**
     * Verifies the 6-digit OTP code entered by the user against Firebase Auth.
     */
    suspend fun verifyOtpCode(
        verificationId: String,
        code: String
    ): FirebaseUser? {
        val auth = FirebaseAuth.getInstance()
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        val result = auth.signInWithCredential(credential).await()
        return result.user
    }
}
