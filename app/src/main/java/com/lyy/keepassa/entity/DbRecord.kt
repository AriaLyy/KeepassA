package com.lyy.keepassa.entity

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lyy.keepassa.util.KeepassAUtil
import com.lyy.keepassa.view.DbPathType

/**
 * 历史记录实体
 */
@Entity
data class DbRecord(

  @PrimaryKey(autoGenerate = true) var uid: Int = 0,

  @ColumnInfo var time: Long,
    // 打开类型
  @ColumnInfo(defaultValue = "AFS") var type: String,
    // 本地数据库uri
  @ColumnInfo var localDbUri: String,

    // 云端路径
  @ColumnInfo var cloudDiskPath: String? = null,
    //密钥的路径
  @ColumnInfo var keyUri: String,
    // 数据库名
  var dbName: String

//  val uri:ByteArray
) : Parcelable {
  constructor(parcel: Parcel) : this(
      parcel.readInt(),
      parcel.readLong(),
      parcel.readString()!!,
      parcel.readString()!!,
      parcel.readString(),
      parcel.readString()!!,
      parcel.readString()!!
  ) {
  }

  fun getDbPathType(): DbPathType {
    return DbPathType.valueOf(type)
  }

  fun getDbUri(): Uri {
    return KeepassAUtil.convertUri(localDbUri)!!
  }

  /**
   * 不能使用 getkeyUri()，否则kotlin 编译会报错
   */
  fun getDbKeyUri(): Uri? {
    return KeepassAUtil.convertUri(keyUri)
  }

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeInt(uid)
    parcel.writeLong(time)
    parcel.writeString(type)
    parcel.writeString(localDbUri)
    parcel.writeString(cloudDiskPath)
    parcel.writeString(keyUri)
    parcel.writeString(dbName)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Creator<DbRecord> {
    override fun createFromParcel(parcel: Parcel): DbRecord {
      return DbRecord(parcel)
    }

    override fun newArray(size: Int): Array<DbRecord?> {
      return arrayOfNulls(size)
    }
  }
}