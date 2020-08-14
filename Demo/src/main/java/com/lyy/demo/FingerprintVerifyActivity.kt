package com.lyy.demo

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.view.View
import androidx.annotation.Nullable
import androidx.biometric.BiometricPrompt
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityFingerprintVerifyBinding
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.Executor

class FingerprintVerifyActivity : AbsActivity<ActivityFingerprintVerifyBinding>() {
  private val KEY_NAME = "FingerprintVerifyActivity"
  private var isReg: Boolean = false

  override fun setLayoutId(): Int {
    return R.layout.activity_fingerprint_verify
  }

  override fun initData(savedInstanceState: Bundle?) {
    super.initData(savedInstanceState)

  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.btnReg -> {
        isReg = true
        val keyPair = generateKeyPair(KEY_NAME, true)
        Log.d(TAG, keyPair.toString())
        val signature = initSignature(KEY_NAME)
        showBiometricPrompt(signature!!)
      }
      R.id.btnAuth -> {
        isReg = false
        val signature = initSignature(KEY_NAME)
        showBiometricPrompt(signature!!)
      }
    }
  }

  private fun showBiometricPrompt(signature: Signature) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("指纹验证")
        .setNegativeButtonText("取消")
//        .setConfirmationRequired(false)
        .build()
    val executor = Executor { }
    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence
          ) {
            Log.e(TAG, "指纹验证错误，errorCode = $errorCode, errorString = $errString")
          }

          override fun onAuthenticationSucceeded(
            result: BiometricPrompt.AuthenticationResult
          ) {
            super.onAuthenticationSucceeded(result)
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Log.e(TAG, "指纹验证失败")
          }
        })
    // Displays the "log in" prompt.
    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
  }

  /**
   * 生成密钥对
   */
  @TargetApi(Build.VERSION_CODES.M)
  @Throws(Exception::class) fun generateKeyPair(
    keyName: String,
    invalidatedByBiometricEnrollment: Boolean
  ): KeyPair? {
    val keyPairGenerator: KeyPairGenerator =
      KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    val builder: KeyGenParameterSpec.Builder = KeyGenParameterSpec.Builder(
        keyName,
        KeyProperties.PURPOSE_SIGN
    )
        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        .setDigests(
            KeyProperties.DIGEST_SHA256,
            KeyProperties.DIGEST_SHA384,
            KeyProperties.DIGEST_SHA512
        ) // Require the user to authenticate with a biometric to authorize every use of the key
        .setUserAuthenticationRequired(true)

    // Generated keys will be invalidated if the biometric templates are added more to user device
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
    }
    keyPairGenerator.initialize(builder.build())
    return keyPairGenerator.generateKeyPair()
  }

  /**
   * 获取密钥对
   */
  @TargetApi(Build.VERSION_CODES.M)
  @Throws(java.lang.Exception::class)
  fun getKeyPair(keyName: String): KeyPair? {
    val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    if (keyStore.containsAlias(keyName)) {
      // Get public key
      val publicKey: PublicKey = keyStore.getCertificate(keyName).publicKey
      // Get private key
      val privateKey: PrivateKey = keyStore.getKey(keyName, null) as PrivateKey
      // Return a key pair
      return KeyPair(publicKey, privateKey)
    }
    return null
  }

  @TargetApi(Build.VERSION_CODES.M)
  @Nullable @Throws(java.lang.Exception::class)
  fun initSignature(keyName: String): Signature? {
    val keyPair = getKeyPair(keyName)
    if (keyPair != null) {
      val signature: Signature = Signature.getInstance("SHA256withECDSA")
      signature.initSign(keyPair.private)
      return signature
    }
    return null
  }

}