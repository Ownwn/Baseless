package com.ownwn.baseless

import com.ownwn.server.Handle
import com.ownwn.server.HttpMethod
import com.ownwn.server.java.lang.replacement.stream.OutputStream
import com.ownwn.server.request.PostRequest
import com.ownwn.server.request.Request
import com.ownwn.server.response.Response
import com.ownwn.server.response.TemplateResponse
import com.ownwn.server.response.WholeBodyResponse
import java.util.*


class Controller {
    @Handle("entries")
    fun entries(request: Request): Response {
        return WholeBodyResponse.json(Database.getEntries())
    }

    @Handle("submit", method = HttpMethod.POST)
    fun submit(request: PostRequest): Response {

        val attachments = request.formAttachmentHandler() ?: return WholeBodyResponse.badRequest()

        attachments.consumeAttachments { attachment ->
            val target = Database.dataRoot.resolve(Database.createFileName(attachment.fileName()))
            target.createNewFile()
            target.outputStream()
        }

        return WholeBodyResponse.ok()
    }

    @Handle("/submitsilent", method = HttpMethod.POST)
    fun submitsilent(request: PostRequest): Response {

        val attachments = request.formAttachmentHandler() ?: return WholeBodyResponse.badRequest()
        val isTooBig = request.requestHeaders()?.get("Content-Length")?.toIntOrNull()?.let { it > 100_000_000 } ?: true
        if (isTooBig) {
            return WholeBodyResponse.badRequest("too big")
        }
        attachments.consumeAttachments { attachment ->
            val target = Database.dataRoot.resolve(Database.createPlaintextFileName(attachment.fileName()))
            target.createNewFile()
            target.outputStream()
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
