package com.example.shifttestbinlist.network

import android.content.Context
import com.example.shifttestbinlist.R
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


private const val BASE_URL = "https://lookup.binlist.net/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

/*
Подключение через дефолтный retrofit вызывает исключение:
javax.net.ssl.SSLHandshakeException:
java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.

Из оффициальной документации понятно, что это происходит по одной из причин:
1) The CA that issued the server certificate was unknown.
2) The server certificate wasn't signed by a CA, but was self signed.

Фикс, предложенный документацией,
(Добавление сертификата сайта в trust-anchors файла network-security-config.xml)
по какой-то причине не работает (p.s. файл network-security-config добавлен в манифест).
Пытался разобраться, но тщетно.

Поэтому пришлось взять функцию createAdapter со StackOverflow,
которая добавляет okhttp клиент, доверяющий сертификату сервера, к объекту retrofit.
Функция возвращает объект retrofit.
Так как функции нужен Context, то она вызывается через объект BinApi в BinFragment

На момент сдачи тестового (06.03.2023) подключение через certRetrofit успешно.
Но загруженный с сервера сертифика действителен до 22.03.2023.
Поэтому на случай истечения сертификата используется подключение через unsafeRetrofit.
unsafeRetrofit использует okhttp клиент, который доверяет любому ssl-сертификату.
*/

private lateinit var certRetrofit: Retrofit

private val unsafeRetrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .client(unsafeOkHttpClient().build())
    .build()

interface BinApiService {
    @Headers("Accept-Version: 3")
    @GET("{id}")
    suspend fun getBin(@Path("id") id: String): BinInfo
}

object BinApi {
    // функция вызывается в BinFragment и инициализирует
    // certRetrofit с помощью createAdapter
    fun createCertRetrofit (context: Context) {
        certRetrofit = createAdapter(context)
    }

    // Вызов функции create() для объекта Retrofit является ресурсоемким
    // поэтому используется lazy-инициализация
    val unsafeRetrofitService : BinApiService by lazy {
        unsafeRetrofit.create(BinApiService::class.java) }

    /*
    Если сертификат сервера просрочен, используется unsafeRetrofit.
    В таком случае certRetrofitService больше не нужен.
    Поэтому, чтобы в будущем certRetrofitService можно было сделать
    доступным для сбора мусора (присвоить переменной null),
    используется механизм инициализации через функцию, а не через lazy.
    */
    var certRetrofitService : BinApiService? = null
    fun createCertRetrofitService() {
        certRetrofitService = certRetrofit.create(BinApiService::class.java)
    }

}


private fun createAdapter(context: Context): Retrofit {
    // loading CAs from an InputStream
    val cf = CertificateFactory.getInstance("X.509")
    val certStream = context.resources.openRawResource(R.raw.my_cert)
    val ca = certStream.use {
        cf.generateCertificate(it)
    }

    // creating a KeyStore containing our trusted CAs
    val keyStoreType = KeyStore.getDefaultType()
    val keyStore = KeyStore.getInstance(keyStoreType)
    keyStore.load(null, null)
    keyStore.setCertificateEntry("ca", ca)

    // creating a TrustManager that trusts the CAs in our KeyStore
    val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
    val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
    tmf.init(keyStore)

    // creating an SSLSocketFactory that uses our TrustManager
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.trustManagers, null)

    // creating an OkHttpClient that uses our SSLSocketFactory
    val okHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
        .build()

    // creating a Retrofit that uses this custom client
    return Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()
}

private fun unsafeOkHttpClient() : OkHttpClient.Builder {
    val okHttpClient = OkHttpClient.Builder()
    try {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts:  Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?){}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate>  = arrayOf()
        })

        // Install the all-trusting trust manager
        val  sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        if (trustAllCerts.isNotEmpty() &&  trustAllCerts.first() is X509TrustManager) {
            okHttpClient.sslSocketFactory(sslSocketFactory, trustAllCerts.first() as X509TrustManager)
            okHttpClient.hostnameVerifier { _, _ -> true }
        }

        return okHttpClient
    } catch (e: Exception) {
        return okHttpClient
    }
}
