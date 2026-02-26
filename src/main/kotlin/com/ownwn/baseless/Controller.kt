package com.ownwn.baseless

import com.ownwn.server.Handle
import com.ownwn.server.HttpMethod
import com.ownwn.server.request.PostRequest
import com.ownwn.server.request.Request
import com.ownwn.server.response.Response
import com.ownwn.server.response.TemplateResponse
import com.ownwn.server.response.WholeBodyResponse
import java.util.*
import kotlin.collections.getOrNull
import kotlin.collections.isNullOrEmpty


class Controller {
    @Handle("entries")
    fun entries(request: Request): Response {
        return WholeBodyResponse.json(Database.getEntries())
    }

    @Handle("submit", method = HttpMethod.POST)
    fun submit(request: PostRequest): Response {
        val formData = request.loadFormData() ?: return WholeBodyResponse.badRequest()
        val text = formData["text"]?.java()?.getOrNull(0)
        val files = formData["file"]

        if (text?.bytes()?.isNotEmpty() != true && formData["file"]?.java().isNullOrEmpty()) {
            return WholeBodyResponse.badRequest("missing attachments")
        }

        text?.bytes()?.let { Database.addEntry(it) }

        if (!files?.java().isNullOrEmpty()) {
            for (file in files) {
                if (file == null || file.bytes().isEmpty()) continue

                try {
                    Database.addEntry(file.bytes(), file.fileName())
                } catch (e: Exception) {
                    return WholeBodyResponse.badRequest(e.message)
                }
            }
        }


        return WholeBodyResponse.ok()
    }

    @Handle("/delete", method = HttpMethod.DELETE)
    fun deleteEntry(request: Request): Response {
        val created = request.queryParameters()!!["created"]?.toLongOrNull()
        val id = request.queryParameters()!!["id"]?.toIntOrNull()

        if (created == null || id == null) {
            return WholeBodyResponse.badRequest("missing params")
        }
        if (Database.deleteEntry(created, id)) {
            return WholeBodyResponse.ok("Successfully deleted")
        }
        return WholeBodyResponse.notFound
    }

    @Handle("/downloadfile")
    fun downloadFile(request: Request): Response {
        val created = request.queryParameters()["created"]?.toLongOrNull()
        val fileNameBase64 = request.queryParameters()["filename"]
        if (created == null || fileNameBase64 == null) {
            return WholeBodyResponse.badRequest("missing filename")
        }

        val fileBytes = Database.getFileBytes(created, Base64.getDecoder().decode(fileNameBase64).decodeToString())
            ?: return WholeBodyResponse.notFound

        return WholeBodyResponse.ok(fileBytes)
    }

    @Handle("clearcookie")
    fun clearCookies(request: Request): Response {
        return TemplateResponse.of("clearcookie")
    }

    @Handle("login")
    fun login(request: Request): Response {
        return TemplateResponse.of("login")
    }
}
