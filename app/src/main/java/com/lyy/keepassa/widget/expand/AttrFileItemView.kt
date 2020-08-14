package com.lyy.keepassa.widget.expand

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.R

/**
 * 附件item字段
 */
@SuppressLint("ViewConstructor")
class AttrFileItemView(
  context: Context,
  var titleStr: String,
  var file: ProtectedBinary? = null,
  var fileUri: Uri? = null
) : RelativeLayout(context) {
  val valueTx: TextView

  init {
    LayoutInflater.from(context)
        .inflate(R.layout.layout_expand_child_file, this, true)
    valueTx = findViewById(R.id.value)
    updateData(titleStr, file, fileUri)
  }

  fun updateData(
    titleStr: String,
    valueInfo: ProtectedBinary? = null,
    fileUri: Uri? = null
  ) {
    this.fileUri = fileUri
    this.titleStr = titleStr
    this.file = valueInfo
    valueTx.text = titleStr
  }

}