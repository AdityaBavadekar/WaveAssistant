package com.adityaamolbavadekar.waveassistant

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleCustomSearchApi {

    @GET("customsearch/v1?key=AIzaSyBXsj1-HZMtCv50F566zJDllJuE5fVDX6k&cx=4f8678047c402451e&q=")
    fun search(@Query("q") query: String): Response<Any>

}
