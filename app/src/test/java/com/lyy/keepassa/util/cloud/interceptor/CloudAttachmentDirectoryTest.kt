package com.lyy.keepassa.util.cloud.interceptor

import com.keepassdroid.database.PwEntryV4
import com.keepassdroid.database.security.ProtectedBinary
import com.lyy.keepassa.util.cloud.merge.EntryDifferImpl
import com.lyy.keepassa.util.cloud.merge.ThreeWay
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files

class CloudAttachmentDirectoryTest {

  @Test
  fun create_keepsCloudDatabaseAttachmentsSeparateFromLocalDatabaseAttachments() {
    val root = Files.createTempDirectory("keepassa-attachment-isolation").toFile()
    val localFilesDir = File(root, "files").apply { mkdirs() }
    val cloudRoot = File(root, "cloud-files").apply { mkdirs() }
    val payload = byteArrayOf(1, 2, 3, 4)
    val local = entryWithBinary(writeBinary(File(localFilesDir, "0"), payload))

    val cloudFilesDir = CloudAttachmentDirectory.create(cloudRoot)
    val cloud = entryWithBinary(writeBinary(File(cloudFilesDir, "0"), payload))

    assertNotEquals(localFilesDir.canonicalPath, cloudFilesDir.canonicalPath)
    assertTrue(EntryDifferImpl().diff(local, cloud).binaries[ATTACHMENT_NAME] is ThreeWay.Same)
  }

  @Test
  fun create_usesANewDirectoryForEveryCloudDatabaseOpen() {
    val filesDir = Files.createTempDirectory("keepassa-cloud-files").toFile()

    val first = CloudAttachmentDirectory.create(filesDir)
    val second = CloudAttachmentDirectory.create(filesDir)

    assertNotEquals(first.canonicalPath, second.canonicalPath)
    assertTrue(first.isDirectory)
    assertTrue(second.isDirectory)
  }

  private fun entryWithBinary(binary: ProtectedBinary): PwEntryV4 {
    return PwEntryV4().apply {
      binaries[ATTACHMENT_NAME] = binary
    }
  }

  private fun writeBinary(file: File, payload: ByteArray): ProtectedBinary {
    file.parentFile?.mkdirs()
    file.writeBytes(payload)
    return TestFileBackedBinary(file, payload.size)
  }

  private class TestFileBackedBinary(
    private val file: File,
    private val size: Int
  ) : ProtectedBinary(false, null) {
    override fun length(): Int = size

    override fun getData(): InputStream = FileInputStream(file)
  }

  private companion object {
    const val ATTACHMENT_NAME = "attachment.bin"
  }
}
