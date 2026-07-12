package com.lyy.keepassa.util.cloud.merge.pending

data class CloudRevision(
  val modifiedTime: Long?,
  val contentHash: String?
) {
  fun matches(other: CloudRevision): Boolean {
    if (modifiedTime != other.modifiedTime) {
      return false
    }
    return contentHash == null || other.contentHash == null || contentHash == other.contentHash
  }
}
