package com.arialyy.frame.util

import android.os.Build.VERSION_CODES
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.IOException
import java.lang.Exception
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * https://github.com/stevenocean/UnpasswdDecrypt/blob/master/app/src/main/java/io/github/stevenocean/unpasswddecrypt/MainActivity.java
 */
@RequiresApi(VERSION_CODES.M)
class KeyStoreUtil {
  private val TAG = javaClass.simpleName
  private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
  private val keystoreAlias = "KeepassA"

  companion object {
    var keyStorePass = "".toCharArray()
  }

  init {
    try {
      keyStore.load(null)
      if (keyStore.getKey(keystoreAlias, keyStorePass) == null) {
        generateKey()
      }
    }catch (e:Exception){
      e.printStackTrace()
      deleteKeyStore()
    }
  }

  fun deleteKeyStore(){
    try {
      keyStore.deleteEntry(keystoreAlias)
    }catch (e:Exception){
      e.printStackTrace()
    }
  }

  /**
   * 生成key，并将key存到keystore中
   */
  @Throws(
    NoSuchProviderException::class,
    NoSuchAlgorithmException::class,
    IOException::class,
    CertificateException::class,
    InvalidAlgorithmParameterException::class
  ) private fun generateKey():SecretKey {
    // AES + CBC + PKCS7
    val generator: KeyGenerator =
      KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    keyStore.load(null)
    generator.init(
      KeyGenParameterSpec.Builder(
        keystoreAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
          .setUserAuthenticationRequired(true)
          .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
          .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
          .build()
    )

    // Generate (symmetric) key, and store to KeyStore
    val sk: SecretKey = generator.generateKey()
    Log.d(TAG, String.format("Generate key success %s, %s", sk.algorithm, sk.format))
    return sk
  }

  /**
   * 获取加密对象
   */
  fun getEncryptCipher(): Cipher {
    keyStore.load(null)
    val sk = keyStore.getKey(keystoreAlias, keyStorePass) as SecretKey

    val cipher = Cipher.getInstance(
      KeyProperties.KEY_ALGORITHM_AES
          + "/"
          + KeyProperties.BLOCK_MODE_CBC
          + "/"
          + KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
    // https://stackoverflow.com/questions/44886119/key-permanently-invalidated-exception-after-adding-removing-fingerprint
    try {
      cipher.init(Cipher.ENCRYPT_MODE, sk)
    } catch (e: KeyPermanentlyInvalidatedException) {
      keyStore.deleteEntry(keystoreAlias)
      return getEncryptCipher()
    }

    return cipher
  }

  /**
   * 获取解密对象
   */
  fun getDecryptCipher(iv: ByteArray): Cipher {
    keyStore.load(null)

    val cipher = Cipher.getInstance(
      KeyProperties.KEY_ALGORITHM_AES
          + "/"
          + KeyProperties.BLOCK_MODE_CBC
          + "/"
          + KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
    try {
      cipher.init(Cipher.DECRYPT_MODE, getSk(), IvParameterSpec(iv))
    } catch (e: KeyPermanentlyInvalidatedException) {
      keyStore.deleteEntry(keystoreAlias)
      return getDecryptCipher(iv)
    }

    return cipher
  }

  private fun getSk(): SecretKey {
    var key = keyStore.getKey(keystoreAlias, keyStorePass)
    if (key == null){
      key = generateKey()
    }
    return key as SecretKey
  }

  /**
   * 加密数据
   * @param text 明文
   * @return first 秘文，second iv
   */
  fun encryptData(
      cipher: Cipher,
      text: String
  ): Pair<String, ByteArray> {
    val encrypted = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
    val iv = cipher.iv
    val encryptedWithBase64: String = Base64.encodeToString(encrypted, Base64.URL_SAFE)
    return Pair(encryptedWithBase64, iv)
  }

  /**
   * 解密数据
   * @param text 密文
   * @return 明文
   */
  fun decryptData(
      cipher: Cipher,
      text: String
  ): String {
    val encryptedBytes: ByteArray = Base64.decode(text, Base64.URL_SAFE)
    val decryptedBytes = cipher.doFinal(encryptedBytes)
    return decryptedBytes.toString(Charsets.UTF_8)
  }

}