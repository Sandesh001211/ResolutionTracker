package com.example.resolutionapp.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.resolutionapp.model.ChatMessage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GeminiHelper {

    private static final String API_KEY = "AIzaSyAl2p1wqSJGD0RfDwhFTc_nDVkzQJgC-sA"; // User should replace this
    private static final String MODEL_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onSuccess(String responseText);

        void onError(String error);
    }

    public GeminiHelper() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void chatWithGemini(List<ChatMessage> history, String userMessage, String systemContext,
            GeminiCallback callback) {
        if (API_KEY.equals("YOUR_API_KEY_HERE")) {
            callback.onError("API Key is missing. Please add your Gemini API Key in GeminiHelper.java");
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray contents = new JSONArray();

            // Add system context as a user message first (trick for simple context)
            if (systemContext != null && !systemContext.isEmpty()) {
                JSONObject systemPart = new JSONObject();
                systemPart.put("text", "System Context: " + systemContext);
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "user");
                systemMsg.put("parts", new JSONArray().put(systemPart));
                contents.put(systemMsg);

                // Add an acknowledgement from model to maintain flow
                JSONObject ackPart = new JSONObject();
                ackPart.put("text", "Understood.");
                JSONObject ackMsg = new JSONObject();
                ackMsg.put("role", "model");
                ackMsg.put("parts", new JSONArray().put(ackPart));
                contents.put(ackMsg);
            }

            // Add history
            for (ChatMessage msg : history) {
                JSONObject part = new JSONObject();
                part.put("text", msg.getContent());
                JSONObject chatMsg = new JSONObject();
                chatMsg.put("role", msg.getRole());
                chatMsg.put("parts", new JSONArray().put(part));
                contents.put(chatMsg);
            }

            // Add current user message
            JSONObject userPart = new JSONObject();
            userPart.put("text", userMessage);
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("parts", new JSONArray().put(userPart));
            contents.put(userMsg);

            jsonBody.put("contents", contents);

        } catch (JSONException e) {
            callback.onError("JSON Error: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(MODEL_URL)
                .addHeader("x-goog-api-key", API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network Error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    mainHandler.post(() -> callback
                            .onError("API Error (" + MODEL_URL + "): " + response.code() + " " + errorBody));
                    return;
                }

                try {
                    String respStr = response.body().string();
                    JSONObject jsonResponse = new JSONObject(respStr);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    if (candidates.length() > 0) {
                        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String text = parts.getJSONObject(0).getString("text");
                        mainHandler.post(() -> callback.onSuccess(text));
                    } else {
                        mainHandler.post(() -> callback.onError("No response from AI"));
                    }
                } catch (JSONException e) {
                    mainHandler.post(() -> callback.onError("Parsing Error: " + e.getMessage()));
                }
            }
        });
    }
}
