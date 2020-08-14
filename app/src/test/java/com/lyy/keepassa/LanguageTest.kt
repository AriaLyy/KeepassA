package com.lyy.keepassa

import com.lyy.keepassa.util.LanguageUtil
import org.junit.Test

class LanguageTest {

  @Test
  fun getSysCurrentLanguage(){
    print(LanguageUtil.getSysCurrentLan())
  }

  @Test
  fun checkUrl(){
    val rs = "^(((file|gopher|news|nntp|telnet|http|ftp|https|ftps|sftp)://)|(www\\.))+(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(/[a-zA-Z0-9\\&%_\\./-~-]*)?\$"
    val r1 = Regex(rs)
    print(r1.matches("http://baiducom"))
  }
}