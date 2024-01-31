package io.vlinx.infinite.utils

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2024/1/14
 */

object ResponseUtils {

    private val logger = LoggerFactory.getLogger(ResponseUtils::class.java)

    fun internalError(response: HttpServletResponse, t: Throwable) {
        logger.error(ExceptionUtils.getStackTrace(t))
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.writer.write(t.message ?: "error")
    }

}