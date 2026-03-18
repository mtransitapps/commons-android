package org.mtransit.android.commons.provider.ca.info.stm

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Url

// https://portail.developpeurs.stm.info/apihub/
// https://api.stm.info/pub/od/i3/v2/messages/etatservice"
interface StmInfoApi {

    companion object {
        const val BASE_HOST_URL = "https://api.stm.info/pub/od/i3/"
    }

    @GET // ("v2/messages/etatservice")
    fun getV2MessageEtatService(@Url url: String, @HeaderMap headers: Map<String, String> = emptyMap()): Call<EtatServiceResponse>
}
