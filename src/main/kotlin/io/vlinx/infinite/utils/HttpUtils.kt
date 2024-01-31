package io.vlinx.infinite.utils

import io.vlinx.infinite.exception.HttpRequestException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


/**
 * @author:  vlinx (vlinx@vlinx.io)
 * @date:    2023/11/25
 */

object HttpUtils {

    const val DEFAULT_CONNECT_TIMEOUT = 30
    const val DEFAULT_READ_TIMEOUT = 30

    fun newClient(): OkHttpClient {
        return newClient(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
    }

    fun newClient(connectTimeout: Int, readTimeout: Int): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeout.toLong(), TimeUnit.SECONDS).build()
    }

    fun get(url: String): String {
        return get(url, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
    }

    fun get(url: String, connectTimeout: Int, readTimeout: Int): String {
        val request = Request.Builder().url(url).get().build()
        return submitRequest(request, connectTimeout, readTimeout)
    }

    fun post(url: String, payload: String): String {
        return post(url, payload, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
    }

    fun post(url: String, payload: String, connectTimeOut: Int, readTimeOut: Int): String {
        val request = Request.Builder().url(url).post(payload.toRequestBody()).build()
        return submitRequest(request, connectTimeOut, readTimeOut)
    }

    fun submitRequest(request: Request, connectTimeout: Int, readTimeout: Int): String {
        val response = newClient(connectTimeout, readTimeout).newCall(request).execute()

        val content = String(response.body!!.bytes())
        response.close()

        if (!response.isSuccessful) {
            throw HttpRequestException("Visit: " + request.url.toString() + "\n " + content)
        }

        return content
    }

    fun submitRequest(request: Request): String {
        return submitRequest(request, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT)
    }


}