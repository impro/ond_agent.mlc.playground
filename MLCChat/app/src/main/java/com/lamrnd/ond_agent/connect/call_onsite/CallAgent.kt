package com.lamrnd.ond_agent.connect.call_onsite

import android.util.Log
import com.lamrnd.ond_agent.connect.call_onsite.msg.AgentRequest

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.lamrnd.ond_agent.connect.call_onsite.msg.AgentResponse;

class CallAgent (){
    fun callAgent(inputString:String, callback: AgentCallback) { // Add callback parameter
        // AGENT CALL_CALENDAR On Site
        Log.d("CALL_AGENT START", "6")
        val agentService: AgentService = ApiClient.getClient().create(AgentService::class.java)
        //val request = AgentRequest(command = "오늘 3시에 일정 확인해줘")
        val request = AgentRequest(command = inputString)

        val call = agentService.runAgent(request)

        call.enqueue(object : Callback<AgentResponse> { // Use Kotlin's object expression for Callback
            override fun onResponse(call: Call<AgentResponse>, response: Response<AgentResponse>) {
                Log.d("CALL_AGENT", "Response received") // 추가된 로그
                if (response.isSuccessful) {
                    val agentResponse = response.body()
                    if (agentResponse != null && agentResponse.result != null) {
                        val output = agentResponse.result.output
                        val skillset_command = agentResponse.result.skillset_command
                        callback.invoke(output)
                    } else {
                        Log.e("CallAgent", "Response body or result is null")
                        callback.invoke(null)
                    }
                } else {
                    Log.e("MainActivity", "Request failed: ${response.message()}")
                    callback.invoke(null)
                }
            }
            override fun onFailure(call: Call<AgentResponse>, t: Throwable) {
                Log.e("MainActivity", "Network error: ${t.message}")
                callback.invoke(null)
            }
        })
    }

    fun callWVAgent(inputString:String, callback: AgentCallback) { // Add callback parameter
        // AGENT CALL_CALENDAR On Site
        Log.d("CALL_AGENT START", "6")
        val agentService: AgentService = ApiClient.getWVClient().create(AgentService::class.java) //getWVClient
        val request = AgentRequest(command = inputString)

        val call = agentService.runAgent(request)

        call.enqueue(object : Callback<AgentResponse> { // Use Kotlin's object expression for Callback
            override fun onResponse(call: Call<AgentResponse>, response: Response<AgentResponse>) {
                Log.d("CALL_WVAGENT", "Response received") // 추가된 로그
                if (response.isSuccessful) {
                    val agentResponse = response.body()
                    if (agentResponse != null && agentResponse.result != null) {
                        val output = agentResponse.result.output
                        //val skillset_command = agentResponse.result.skillset_command
                        callback.invoke(output)
                    } else {
                        Log.e("CALL_WVAGENT", "Response body or result is null")
                        callback.invoke(null)
                    }
                } else {
                    Log.e("CALL_WVAGENT", "Request failed: ${response.message()}")
                    callback.invoke(null)
                }
            }
            override fun onFailure(call: Call<AgentResponse>, t: Throwable) {
                Log.e("CALL_WVAGENT", "Network error: ${t.message}")
                callback.invoke(null)
            }
        })
    }
}