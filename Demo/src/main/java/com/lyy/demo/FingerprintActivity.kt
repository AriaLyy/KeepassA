package com.lyy.demo

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.arch.core.executor.ArchTaskExecutor.getMainThreadExecutor
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.PromptInfo
import com.arialyy.frame.core.AbsActivity
import com.arialyy.frame.util.KeyStoreUtil
import com.lyy.demo.databinding.ActivityFingerprintBinding

@TargetApi(Build.VERSION_CODES.O)
class FingerprintActivity : AbsActivity<ActivityFingerprintBinding>() {
  var isEncode = true
  var text = "laoyuyu789"
  var encodeText = ""
  val keyStoreUtil = KeyStoreUtil()
  var iv: ByteArray? = null

  override fun setLayoutId(): Int {
    return R.layout.activity_fingerprint
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)
    binding.tvMsg.append(text)
  }

  fun onClick(view: View) {
    when (view.id) {
      R.id.btnReg -> {
        isEncode = true
        showBiometricPrompt()
      }
      R.id.btnAuth -> {
        isEncode = false
        showBiometricPrompt()
      }
    }
  }

  @SuppressLint("RestrictedApi")
  private fun showBiometricPrompt() {
    val authenticationCallback: AuthenticationCallback = getAuthenticationCallback()
    val mBiometricPrompt = BiometricPrompt(
        this,
        getMainThreadExecutor(),
        authenticationCallback
    )

    // Set prompt info
    val promptInfo: PromptInfo = PromptInfo.Builder()
        .setDescription("Description")
        .setTitle("Title")
        .setSubtitle("Subtitle")
        .setNegativeButtonText("Cancel")
        .build()

    mBiometricPrompt.authenticate(
        promptInfo,
        CryptoObject(
            if (isEncode) keyStoreUtil.getEncryptCipher() else keyStoreUtil.getDecryptCipher(iv!!)
        )
    )
  }

  /**
   * 指纹验证回调
   */
  private fun getAuthenticationCallback(): AuthenticationCallback {
    // Callback for biometric authentication result
    return object : AuthenticationCallback() {
      override fun onAuthenticationError(
        errorCode: Int,
        @NonNull errString: CharSequence
      ) {
        super.onAuthenticationError(errorCode, errString)
      }

      override fun onAuthenticationSucceeded(@NonNull result: AuthenticationResult) {
        Log.i(TAG, "onAuthenticationSucceeded")
        super.onAuthenticationSucceeded(result)
        val auth: CryptoObject? = result.cryptoObject
        val clipher = auth!!.cipher!!
        if (isEncode) {
          val p = keyStoreUtil.encryptData(clipher, text)
          iv = p.second
          encodeText = p.first
          binding.tvEncode.append(encodeText)
        } else {
          val decodeText = keyStoreUtil.decryptData(clipher, encodeText)
          binding.tvDecode.append(decodeText)
        }

      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
      }
    }
  }

  /**
   * 是否支持生物识别：指纹，面部
   * @return true 支持，false 硬件不支持，或者用户没有设置指纹
   */
  fun hasBiometricPrompt(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    var can = false
    when (biometricManager.canAuthenticate()) {
      BiometricManager.BIOMETRIC_SUCCESS -> {
        Log.d(TAG, "App can authenticate using biometrics.")
        can = true
      }

      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
        Log.e(TAG, "No biometric features available on this device.")
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
        Log.e(TAG, "Biometric features are currently unavailable.")
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
        Log.e(
            TAG, "The user hasn't associated any biometric credentials " +
            "with their account."
        )
    }
    return can
  }
}