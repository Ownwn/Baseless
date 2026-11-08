package com.ownwn.baseless

import com.ownwn.server.request.Request
import com.ownwn.server.intercept.Intercept
import com.ownwn.server.intercept.InterceptReceiver
import com.ownwn.server.response.WholeBodyResponse
import jdk.internal.joptsimple.internal.Strings.isNullOrEmpty
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

const val loginPath: String = "/login"
const val submitSilentPath: String = "/submitsilent"
const val cookieName: String = "COOKIE_VALUE"
const val silentKeyName: String = "SILENT_KEY"

class Auth {
    @Intercept
    fun auth(request: Request, interceptor: InterceptReceiver) {
        if (loginPath == request.path()) {
            return
        }
        if (submitSilentPath == request.path()) {
            if (BaselessApplication.getEnv(silentKeyName).isNullOrBlank()) {
                interceptor.closeWithResponse(WholeBodyResponse.of(500, "silent submit key not set in env!"))
                return
            }

            if (request.queryParameters()?.containsKey("key") != true) {
                interceptor.closeWithResponse(WholeBodyResponse.badRequest("Missing key query param"))
                return
            }

            if (request.queryParameters()["key"] != BaselessApplication.getEnv(silentKeyName)) {
                interceptor.closeWithResponse(WholeBodyResponse.unauthorised())
                return
            }
            return
        }

        if (request.cookies()?.java().isNullOrEmpty()) {
            interceptor.closeWithResponse(WholeBodyResponse.unauthorised())
            return
        }

        val cookieValue = BaselessApplication.getEnv(cookieName) ?: run {
            System.err.println("Missing cookie value from .env! this should've been inited in in startup")
            interceptor.closeWithResponse(WholeBodyResponse.unauthorised())
            return
        }


        val authenticated = request.cookies()?.get(cookieName)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) } == cookieValue
        if (!authenticated) {
            interceptor.closeWithResponse(WholeBodyResponse.unauthorised())
            return
        }
    }
}