package com.ownwn.baseless

import com.ownwn.server.request.Request
import com.ownwn.server.intercept.Intercept
import com.ownwn.server.intercept.InterceptReceiver
import com.ownwn.server.response.WholeBodyResponse
import jdk.internal.joptsimple.internal.Strings.isNullOrEmpty
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

const val loginPath: String = "/login"
const val cookieName: String = "COOKIE_VALUE"

class Auth {
    @Intercept
    fun auth(request: Request, interceptor: InterceptReceiver) {
        if (loginPath == request.path()) {
            return
        }

        if (request.cookies()?.java().isNullOrEmpty()) {
            interceptor.closeWithResponse(WholeBodyResponse.unauthorised())
            return
        }

        val cookieValue = BaselessApplication.getEnv(cookieName) ?: run {
            System.err.println("Missing cookie value!")
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