//package com.lamrnd.call_onsite.msg
package com.lamrnd.ond_agent.connect.call_onsite.msg

data class AgentResponse(
    val result: Result?
)

data class Result(
    val code: String?,
    val instruction: String?,
    val output: String?,
    val success: Boolean?,
    val skillset_command: String?,
    val total_estimated_cost: Int?
)

/*
public class AgentResponse {
    //var result:String =
    private var result: String? = null

    fun getResult(): String?{
        return result
    }

    //fun setResult(result : String){
    //    this.result = result
    //}
}
 */