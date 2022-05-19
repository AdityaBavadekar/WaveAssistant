package com.adityaamolbavadekar.waveassistant

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyHelper {

    private lateinit var spotifyAppRemote: SpotifyAppRemote
    private var isConnected = false
    private var INSTANCE: Context? = null

    fun initialise(context: Context): SpotifyHelper {
        if (INSTANCE != null) {
            INSTANCE!!.debug("SpotifyHelper was already initialised")
        } else {
            this.INSTANCE = context
            INSTANCE!!.debug("SpotifyHelper was initialised successfully")
        }
        return this
    }

    fun onCreate() {

    }

    fun onResume() {

    }

    fun authorise(activity:AppCompatActivity){
        val builder = AuthorizationRequest.Builder(CLIENT_ID,AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
        builder.setScopes(arrayOf("streaming"))
        val request = builder.build()
        AuthorizationClient.openLoginActivity(activity, REQUEST_CODE,request)
    }

    fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?){
        if (requestCode == REQUEST_CODE){
            val response = AuthorizationClient.getResponse(resultCode,data)
            INSTANCE!!.debug("RESPONSE_SPOTIFY = $response")
            response.type?.let {
                when(it){
                    AuthorizationResponse.Type.TOKEN->{
                        INSTANCE!!.debug("AuthorizationResponseType=TOKEN")
                    }
                    AuthorizationResponse.Type.CODE->{
                        INSTANCE!!.debug("AuthorizationResponseType=CODE")
                    }
                    AuthorizationResponse.Type.EMPTY->{
                        INSTANCE!!.debug("AuthorizationResponseType=EMPTY")
                    }
                    AuthorizationResponse.Type.ERROR->{
                        INSTANCE!!.debug("AuthorizationResponseType=ERROR")
                    }
                    AuthorizationResponse.Type.UNKNOWN->{
                        INSTANCE!!.debug("AuthorizationResponseType=UNKNOWN")
                    }
                }

            }

        }
    }

    fun onStart() {
            val params = ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build()
            SpotifyAppRemote.connect(INSTANCE, params, object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote?) {
                    spotifyAppRemote?.let {
                        this@SpotifyHelper.spotifyAppRemote = it
                        isConnected = true
                        INSTANCE!!.debug("Successfully connected to Spotify Service!")
                    }
                }

                override fun onFailure(error: Throwable?) {
                    INSTANCE!!.debug(error.toString())
                }
            })
    }

    fun onDestroy() {

    }

    fun onStop() {

    }

    companion object {
        const val CLIENT_ID = "cd03dea0517b402d935e50e414ec9ae5"
        const val REDIRECT_URI = "wave://callback"
        const val REQUEST_CODE = 1739
    }

}