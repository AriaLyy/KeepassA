package com.lyy.demo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.arialyy.frame.core.AbsActivity
import com.lyy.demo.databinding.ActivityUriTestBinding
import kotlinx.android.synthetic.main.activity_uri_test.content
import kotlinx.android.synthetic.main.activity_uri_test.textUrl
import kotlin.random.Random

/**
 * 出现崩溃的步骤
 * 1、获取uri
 * 2、清空应用数据
 * 3、直接使用该uri
 */
class UriActivity : AbsActivity<ActivityUriTestBinding>() {
  var curUri: Uri? = null
  val temp = "content://com.android.providers.downloads.documents/document/msf%3A27"
  override fun setLayoutId(): Int {
    return R.layout.activity_uri_test

  }

  fun onClick(v: View) {
    when (v.id) {
      R.id.load -> {
        curUri = Uri.parse(temp)
        if (!checkPermissions(curUri!!)){
          Log.d(TAG, packageName)
          grantUriPermission(null, curUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
          Log.e(TAG, "uri[$curUri]没有权限")
          loadContent()
          revokeUriPermission(curUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }else{
          loadContent()
        }
      }
      R.id.modify -> {
        val nu = Random(2000).nextInt().toString()
        if (curUri == null){
          Log.d(TAG, "uri 为空")
          return
        }
        contentResolver.openOutputStream(curUri!!)?.apply {
          write(nu.toByteArray(Charsets.UTF_8))
          flush()
          close()
        }
        content.text = nu
      }
      R.id.choose -> {
        openSysFileManager(this, "*/*", 2)
      }
    }
  }

  private fun checkPermissions(uri: Uri):Boolean{
    val urls = contentResolver.persistedUriPermissions
    for (p in urls){
      if (p.uri == uri && p.isWritePermission){
        return true
      }
    }
    return false
  }

  private fun loadContent(){
    if (curUri == null){
      Log.d(TAG, "uri 为空")
      return
    }
    contentResolver.openInputStream(curUri!!)?.apply {
      val b = readBytes()
      content.text = String(b, Charsets.UTF_8)
      close()
    }
  }

  fun openSysFileManager(
    obj: Any,
    type: String,
    requestCode: Int
  ) {
    Intent.ACTION_GET_CONTENT
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      this.type = type
      addCategory(Intent.CATEGORY_OPENABLE)
      addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    }
    if (obj is Activity) {
      obj.startActivityForResult(intent, requestCode)
    } else if (obj is Fragment) {
      obj.startActivityForResult(intent, requestCode)
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 2) {

      val takeFlags: Int =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      contentResolver.takePersistableUriPermission(data?.data!!, takeFlags)
      textUrl.text = data.data.toString()
      Log.d(TAG, data.data.toString())
      curUri = data.data

      loadContent()
    }

  }

}