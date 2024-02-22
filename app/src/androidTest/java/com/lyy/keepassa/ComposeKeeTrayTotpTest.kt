package com.lyy.keepassa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedString
import com.lyy.keepassa.util.totp.ComposeKeeTrayTotp
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @Author laoyuyu
 * @Description
 * @Date 1:50 PM 2024/2/22
 **/
@RunWith(AndroidJUnit4::class)
class ComposeKeeTrayTotpTest {

  @Test
  fun testGetOtpPass() {
    val entry = PwEntryV4()
    entry.strings = hashMapOf<String?, ProtectedString?>().apply {
      put("STR_URL", ProtectedString(false, "sss"))
      put("TOTP Settings", ProtectedString(false, "30;6"))
      put("TOTP Seed", ProtectedString(true, "123"))
    }

    val token = ComposeKeeTrayTotp.getOtpPass(entry)
    println("token = $token")
  }
}