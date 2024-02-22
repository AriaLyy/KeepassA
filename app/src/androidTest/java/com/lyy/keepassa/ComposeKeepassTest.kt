package com.lyy.keepassa

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lyy.keepassa.entity.TimeOtp2Bean
import com.lyy.keepassa.util.totp.ComposeKeepass
import com.lyy.keepassa.util.totp.SecretHexType
import com.lyy.keepassa.util.totp.TokenCalculator.HashAlgorithm
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @Author laoyuyu
 * @Description
 * @Date 9:57 AM 2024/2/22
 **/
@RunWith(AndroidJUnit4::class)
class ComposeKeepassTest {

  // @MockK
  // lateinit var timeOtp2Bean: TimeOtp2Bean

  @Before
  fun setUp() {
    // 在每个测试方法开始前初始化所有标注了@MockK的字段
    MockKAnnotations.init(this)
  }

  @Test
  fun testHandleTotp() {
    val timeOtp2Bean = mockk<TimeOtp2Bean>()
    every { timeOtp2Bean.secret } returns "123"
    every { timeOtp2Bean.secretType } returns SecretHexType.BASE_32
    every { timeOtp2Bean.period } returns 30
    every { timeOtp2Bean.digits } returns 6
    every { timeOtp2Bean.algorithm } returns HashAlgorithm.SHA1
    val instance = ComposeKeepass::class.java.getField("INSTANCE").get(null)
    val method =
      ComposeKeepass::class.java.getDeclaredMethod("handleTotp", TimeOtp2Bean::class.java)
    method.isAccessible = true
    val token = method.invoke(instance, timeOtp2Bean)

    println("otp: $token")
  }
}