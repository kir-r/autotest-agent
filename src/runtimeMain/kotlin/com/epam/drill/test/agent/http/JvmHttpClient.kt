/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.test.agent.http

import com.epam.drill.kni.*
import com.epam.drill.logger.*
import com.epam.drill.test.agent.config.*
import java.net.*
import java.security.*
import java.security.cert.*
import javax.net.ssl.*


@Kni
actual object JvmHttpClient {
    private val logger = Logging.logger(JvmHttpClient::class.java.name)

    init {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String,
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )
        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true };
    }


    //TODO EPMDJ-8916 Remove okhttp
    actual fun httpCall(endpoint: String, request: String): String {
        val httpRequest = HttpRequest.serializer() parse request
        return kotlin.runCatching {
            val url = endpoint.takeIf { endpoint.startsWith("http") } ?: "http://$endpoint"
            val httpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = httpRequest.method
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                httpRequest.headers.forEach { (k, v) ->
                    setRequestProperty(k, v)
                }
                outputStream.use {
                    it.write(httpRequest.body.toByteArray())
                }
                connect()
            }
            val httpResponse = HttpResponse(
                httpURLConnection.responseCode,
                httpURLConnection.headerFields.filter { it.key != null }.map { it.key to it.value.first() }
                    .associate { it.first to it.second },
                httpURLConnection.inputStream.reader().readText()
            )
            HttpResponse.serializer() stringify httpResponse
        }.onFailure {
            if (it is SocketTimeoutException) {
                logger.warn { "Can't get response to request: $request. Read time out" }
            } else logger.error(it) { "Can't get response. Reason:" }
        }.getOrDefault(HttpResponse.serializer() stringify HttpResponse(500))
    }
}
