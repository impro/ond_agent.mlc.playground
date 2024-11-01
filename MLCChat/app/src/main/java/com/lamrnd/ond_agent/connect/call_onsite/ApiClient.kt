package com.lamrnd.ond_agent.connect.call_onsite

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object ApiClient {//public class ApiClient {

    //val trustManager = UnsafeTrustManager()
    //val sslContext = trustManager.allowAllSSL()
    private var instance: ApiClient? = null

    private var retrofit: Retrofit? = null
    //private var BASE_URL = "http://localhost:5000/"  // Replace with your server IP and port
    //private var BASE_URL = "http://172.23.253.114:80/"  // ILSAN /WALKERHILL
    var BASE_AGENT = "lam-agent"
    //var WV_AGENT = "appium"
    var WV_AGENT = "pim-agent"

    @JvmStatic
    var BASE_URL_HOST = "192.168.55.175"  // HOME
    //var BASE_URL_HOST = "192.168.55.176"  // HOME
    //var BASE_URL_HOST = "172.23.150.47"  // HQ_20240826
    //var BASE_URL_HOST = "172.23.150.85" // HQ_20241021
    //var BASE_URL_HOST = "172.23.253.244"  // ILSAN /WALKERHILL
    val BASE_AGENT_URL = "http://" + BASE_URL_HOST + ":80/" + BASE_AGENT + "/"  // HOME
    //val BASE_URL = "http://" + BASE_URL_HOST + ":80/"  // HOME
    var BASE_URL = BASE_AGENT_URL  // BASE_AGENT_URL 값을 BASE_URL에 대입
    val WV_AGENT_URL = "http://" + BASE_URL_HOST + ":80/" + WV_AGENT + "/"  // HOME

    //private var BASE_URL = "http://192.168.55.176:80/"  // HOME
    //private var BASE_URL = "http://172.23.148.193:80/" // HQ
    //private var BASE_URL = "http://172.23.149.43:80/" // HQ_20240812
    //private var BASE_URL = "http://localhost:5000/run-agent"  // Replace with your server IP and port
     fun setBaseUrl(newUrl: String) {
        //BASE_URL = newUrl
        retrofit = null // Reset Retrofit when the base URL changes
    }

    private val client = OkHttpClient.Builder()
        //.sslSocketFactory(sslContext.socketFactory, trustManager)
        //.hostnameVerifier { _, _ -> true }
        .connectTimeout(500, TimeUnit.SECONDS)  // 연결 타임아웃을 300초로 설정
        .writeTimeout(500, TimeUnit.SECONDS)   // 쓰기 타임아웃을 300초로 설정
        .readTimeout(500, TimeUnit.SECONDS)    // 읽기 타임아웃을 300초로 설정
        .build()
/*
    private val client: OkHttpClient by lazy {
        if (BASE_URL.contains("localhost") || BASE_URL.contains("127.0.0.1") ||
            BASE_URL.contains("172.23.253.114") || BASE_URL.contains("192.168.55.176") ||
            BASE_URL.contains("172.23.148.193")) {
            getUnsafeOkHttpClient()
        } else {
            OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout
                .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout
                .readTimeout(60, TimeUnit.SECONDS)    // Read timeout
                .build()
        }
    }
*/

    /*
    private val client = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManager)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout
        .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout
        .readTimeout(60, TimeUnit.SECONDS)    // Read timeout
        .build()
    //fun getClient(): Retrofit? {
    //    if (retrofit == null) {
    //        retrofit = Retrofit.Builder()
    //            .baseUrl("http://localhost:5000/") // Replace with your server IP and port
    //            .addConverterFactory(GsonConverterFactory.create())
    //            .build()
    //    }
    //    return retrofit
    //}
    */
    fun getClient(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun getWVClient(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(WV_AGENT_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }


    private fun getUnsafeOkHttpClient(): OkHttpClient {
        //val trustManager = UnsafeTrustManager()
        //val sslContext = trustManager.allowAllSSL()
        //UnsafeTrustManager.allowAllSSL();

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)

        //val (sslContext, trustManager) = UnsafeTrustManager.allowAllSSL()

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(60, TimeUnit.SECONDS)  // Connection timeout
            .writeTimeout(60, TimeUnit.SECONDS)   // Write timeout
            .readTimeout(60, TimeUnit.SECONDS)    // Read timeout
            .build()
    }
}