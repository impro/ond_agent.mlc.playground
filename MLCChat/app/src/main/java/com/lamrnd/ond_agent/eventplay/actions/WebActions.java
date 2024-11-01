package com.lamrnd.ond_agent.eventplay.actions;

import android.content.Context;
import android.util.Log;

import com.lamrnd.ond_agent.connect.call_onsite.AgentCallback;
import com.lamrnd.ond_agent.connect.call_onsite.CallAgent;

public class WebActions {
    public void performActions(Context context, String inputString, AgentCallback callback) {
        // CallAgent 초기화를 시작하는 로그
        Log.d("performWebActions", "WebActions:CallAgent callAgent = new CallAgent(); START ");

        // CallAgent 인스턴스 초기화
        CallAgent callWebactionAgent = new CallAgent();

        // CallAgent 초기화를 완료하는 로그
        Log.d("performCalendarActions", "CallAgent callAgent = new CallAgent(); END ");

        // Java 람다를 사용하여 Kotlin 메서드 호출
        callWebactionAgent.callAgent(inputString, new AgentCallback() {
            @Override
            public void invoke(String result) {
                // 콜백이 호출되었음을 로그로 기록
                Log.d("WebActions:performCalendarActions", "WebActions:callAgent callback invoked");

                if (result != null) {
                    // 에이전트의 결과를 로그로 기록
                    Log.d("WebActions:performCalendarActions", "WebActions:Agent result: " + result);
                    callback.invoke(result);
                    // 필요 시 ViewModel 또는 UI를 업데이트
                } else {
                    // 에이전트로부터 결과를 가져오지 못했음을 로그로 기록
                    Log.d("performWebActions", "WebActions:Failed to get result from agent");
                    callback.invoke(null);  // 콜백에 실패 전달
                }
            }
        });

        Log.d("performWebActions", "WebActions:CallAgent callAgent = new CallAgent(); END ");

    }

    public void performWVActions(Context context, String inputString, AgentCallback callback) {
        // CallAgent 초기화를 시작하는 로그
        Log.d("performWVActions", "WebActions:CallAgent callAgent = new CallAgent(); START ");

        // CallAgent 인스턴스 초기화
        CallAgent callWebactionAgent = new CallAgent();

        // CallAgent 초기화를 완료하는 로그
        Log.d("performCalendarActions", "CallAgent callAgent = new CallAgent(); END ");

        // Java 람다를 사용하여 Kotlin 메서드 호출
        callWebactionAgent.callWVAgent(inputString, new AgentCallback() { // callWVAgent **
            @Override
            public void invoke(String result) {
                // 콜백이 호출되었음을 로그로 기록
                Log.d("WebActions:performWVActions", "WebActions:callAgent callback invoked");

                if (result != null) {
                    // 에이전트의 결과를 로그로 기록
                    Log.d("WebActions:performWVActions", "WebActions:Agent result: " + result);
                    callback.invoke(result);
                    // 필요 시 ViewModel 또는 UI를 업데이트
                } else {
                    // 에이전트로부터 결과를 가져오지 못했음을 로그로 기록
                    Log.d("performWVActions", "WebActions:Failed to get result from agent");
                    callback.invoke(null);  // 콜백에 실패 전달
                }
            }
        });

        Log.d("performWVActions", "WebActions:CallAgent callAgent = new CallAgent(); END ");

    }
}
