package io.vlinx.infinite.utils

import com.fasterxml.jackson.databind.ObjectMapper


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2023/11/23
 */

object JSONUtils {

    val objectMapper = ObjectMapper()

    fun toJson(obj: Any?): String {
        return objectMapper.writeValueAsString(obj)
    }

    inline fun <reified T> fromJson(json: String): T? {
        try {
            return objectMapper.readValue(json, T::class.java)
        } catch (t: Throwable) {
            throw Exception("Can't convert [$json] to ${T::class.java}")
        }
    }

}