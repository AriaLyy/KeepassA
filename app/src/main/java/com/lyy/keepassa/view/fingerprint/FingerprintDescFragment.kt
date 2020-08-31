/*
 * Copyright (C) 2020 AriaLyy(https://github.com/AriaLyy/KeepassA)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */


package com.lyy.keepassa.view.fingerprint

import android.annotation.SuppressLint
import android.os.Build.VERSION_CODES
import android.view.View
import androidx.annotation.RequiresApi
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.lifecycle.ViewModelProvider
import com.arialyy.frame.util.KeyStoreUtil
import com.lyy.keepassa.R
import com.lyy.keepassa.base.BaseApp
import com.lyy.keepassa.base.BaseFragment
import com.lyy.keepassa.databinding.FragmentFingerprintDesxBinding
import com.lyy.keepassa.entity.QuickUnLockRecord
import com.lyy.keepassa.util.HitUtil
import com.lyy.keepassa.util.VibratorUtil
import java.lang.Exception

/**
 * 指纹解锁描述
 */
@RequiresApi(VERSION_CODES.M)
class FingerprintDescFragment : BaseFragment<FragmentFingerprintDesxBinding>(),
    View.OnClickListener {

  private val keyStoreUtil = KeyStoreUtil()
  private var isFullUnlock: Boolean = false
  private lateinit var module: FingerprintModule
  private var lastFlag = FingerprintActivity.FLAG_CLOSE

  override fun initData() {
    module = ViewModelProvider(requireActivity()).get(FingerprintModule::class.java)
    if (module.curFlag == FingerprintActivity.FLAG_FULL_UNLOCK) {
      isFullCheck(true)
    } else if (module.curFlag == FingerprintActivity.FLAG_QUICK_UNLOCK) {
      isFullCheck(false)
    }
    lastFlag = module.curFlag

    binding.rlQuick.setOnClickListener(this)
    binding.rlFull.setOnClickListener(this)
  }

  override fun setLayoutId(): Int {
    return R.layout.fragment_fingerprint_desx
  }

  override fun onClick(v: View) {
    if (v.id == R.id.rlQuick) {
      isFullUnlock = false
      module.curFlag = FingerprintActivity.FLAG_QUICK_UNLOCK
      showBiometricPrompt()
      isFullCheck(isFullUnlock)
      return
    }

    if (v.id == R.id.rlFull) {
      isFullUnlock = true
      module.curFlag = FingerprintActivity.FLAG_FULL_UNLOCK
      showBiometricPrompt()
      isFullCheck(isFullUnlock)
    }
  }

  private fun isFullCheck(isFull: Boolean) {
    binding.scFull.isChecked = isFull
    binding.scQuick.isChecked = !isFull
  }

  /**
   * 恢复状态
   */
  private fun goBackCheckStat() {
    when (lastFlag) {
      FingerprintActivity.FLAG_QUICK_UNLOCK -> isFullCheck(false)
      FingerprintActivity.FLAG_FULL_UNLOCK -> isFullCheck(true)
      else -> {
        binding.scFull.isChecked = false
        binding.scQuick.isChecked = false
      }
    }
  }

  /**
   * 显示指纹
   * https://developer.android.com/training/sign-in/biometric-auth#kotlin
   */
  @SuppressLint("RestrictedApi")
  fun showBiometricPrompt() {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(getString(R.string.fingerprint_unlock))
        .setSubtitle(getString(R.string.verify_finger))
        .setNegativeButtonText(getString(R.string.cancel))
//        .setConfirmationRequired(false)
        .build()
    val biometricPrompt = BiometricPrompt(this, ArchTaskExecutor.getMainThreadExecutor(),
        object : AuthenticationCallback() {
          override fun onAuthenticationError(
            errorCode: Int,
            errString: CharSequence
          ) {
            goBackCheckStat()
            if (!isAdded){
              return
            }
            val str = if (errorCode == BiometricConstants.ERROR_NEGATIVE_BUTTON) {
              "${getString(R.string.verify_finger)}${getString(R.string.cancel)}"
            } else {
              getString(R.string.verify_finger_fail)
            }
            HitUtil.snackShort(mRootView, str)
          }

          override fun onAuthenticationSucceeded(result: AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            val auth: CryptoObject? = result.cryptoObject
            if (auth == null || auth.cipher == null) {
              return
            }
            val cipher = auth.cipher!!

            val passPair = keyStoreUtil.encryptData(cipher, BaseApp.dbPass)
            val useKey = BaseApp.dbKeyPath.isNotEmpty()

            val quickInfo = QuickUnLockRecord(
                dbUri = BaseApp.dbRecord.localDbUri,
                dbPass = passPair.first,
                keyPath = BaseApp.dbKeyPath,
                isUseKey = useKey,
                isFullUnlock = isFullUnlock,
                passIv = passPair.second
            )
            module.saveQuickInfo(quickInfo)
            HitUtil.toaskShort("${getString(R.string.verify_finger)} ${getString(R.string.success)}")
            VibratorUtil.vibrator(300)

            module.oldFlag = if (isFullUnlock) {
              FingerprintActivity.FLAG_FULL_UNLOCK
            } else {
              FingerprintActivity.FLAG_QUICK_UNLOCK
            }

            requireActivity().finishAfterTransition()
            lastFlag = module.curFlag
          }

          override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            goBackCheckStat()
            HitUtil.snackShort(mRootView, getString(R.string.verify_finger_fail))
          }
        })
    try {
      // Displays the "log in" prompt.
      biometricPrompt.authenticate(
          promptInfo,
          CryptoObject(keyStoreUtil.getEncryptCipher())
      )
    }catch (e: Exception){
      keyStoreUtil.deleteKeyStore()
      e.printStackTrace()
    }

  }

}