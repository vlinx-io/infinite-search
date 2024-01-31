package io.vlinx.infinite.utils

import java.io.PrintWriter
import java.io.StringWriter


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2023/12/8
 */

object ExceptionUtils {

    fun getStackTrace(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

}