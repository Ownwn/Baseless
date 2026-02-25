package com.ownwn.baseless

import com.ownwn.server.JsonConvertible
import java.io.File
import java.util.*

object Database {
    val dataRoot = File("data/")

    init {
        if (!dataRoot.exists()) dataRoot.mkdir()
    }

    fun addEntry(content: ByteArray) {
        dataRoot.mkdir()

        val file = dataRoot.resolve(createFileName(null))

        if (file.exists()) throw RuntimeException("File $file already exists!")
        file.outputStream().write(content)

    }

    @Throws(Exception::class)
    fun addEntry(fileBytes: ByteArray, fileName: String?) {
        dataRoot.mkdir()
        fileName ?: throw RuntimeException("Bad file name")

        val filePath = dataRoot.resolve(createFileName(fileName))

        if (filePath.exists()) throw RuntimeException("File $filePath already exists!")
        filePath.outputStream().write(fileBytes)
    }

    fun createFileName(fileName: String?): String {
        val randomId = (1000.. (1 shl 30)).random()
        val fileNameEncoded = Base64.getUrlEncoder().encodeToString((fileName ?: "TEXT").toByteArray(
            Charsets.UTF_8))
        return "${if (fileName == null) "TEXT" else "FILE"}|${System.currentTimeMillis()}|$randomId|$fileNameEncoded"
    }

    fun deleteEntry(createdAt: Long, id: Int): Boolean {
        val targetFile = getFiles().firstOrNull { Entry.createEntry(it)?.let { e -> e.id == id && e.createdAt == createdAt } ?: false }
        return targetFile?.delete() == true
    }

    fun getEntries(): List<Entry> {
       return getFiles().mapNotNull {

           Entry.createEntry(it) ?: run {
               System.err.println("Error mapping entry from file ${it.name}")
               null
           }
       }
    }

    fun getFileBytes(time: Long, fileName: String): ByteArray? {
        return dataRoot.listFiles()?.firstOrNull { Entry.createEntry(it)?.createdAt == time && Entry.createEntry(it)?.name == fileName }?.readBytes()
    }

    private fun getFiles(): List<File> {
        return dataRoot.listFiles()?.toList() ?: listOf()
    }
}

@ConsistentCopyVisibility
data class Entry private constructor(val name: String, val plainText: Boolean, val content: String, val createdAt: Long, val id: Int) : JsonConvertible {
    override fun toJson(): String {
        return "{\"name\": \"$name\", \"plainText\": $plainText, \"content\": \"$content\", \"createdAt\": \"$createdAt\", \"id\": $id}"
    }

    companion object {
        fun createEntry(f: File): Entry? {
            if (!f.name.startsWith("TEXT") && !f.name.startsWith("FILE")) {
                return null
            }

            val parts = f.name.split("|")
            if (parts.size != 4) {
                return null
            }

            val name = Base64.getUrlDecoder().decode(parts[3]).toString(Charsets.UTF_8)
            val created = parts[1].toLong()
            val randomId = parts[2].toInt()


            val plainText = f.name.startsWith("TEXT")
            if (plainText) {
                return Entry(name, true, f.readText(), created, randomId)
            }

            return Entry(name, false, "FILE", created, randomId)
        }
    }
}