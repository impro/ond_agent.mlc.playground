package com.lamrnd.ond_agent.connect.call_onsite

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import com.lamrnd.ond_agent.connect.call_onsite.msg.AgentRequest
import com.lamrnd.ond_agent.connect.call_onsite.msg.AgentResponse
import retrofit2.http.Headers

interface AgentService {
    @Headers("Content-Type: application/json")
    @POST("run-agent")
    //fun runAgent(@Body request: AgentRequest?): Call<AgentResponse?>
    fun runAgent(@Body request: AgentRequest): Call<AgentResponse>

}