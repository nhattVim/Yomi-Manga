package com.example.yomi_manga.util

import android.content.Context
import android.util.Log
import com.example.yomi_manga.core.constants.AppConstants
import org.json.JSONObject
import java.io.File

object FirebaseConfig {
    private var cachedWebClientId: String? = null

    /**
     * Get Web Client ID from google-services.json
     */
    fun getWebClientId(context: Context): String? {
        cachedWebClientId?.let { return it }
        
        return try {
            try {
                val inputStream = context.assets.open("google-services.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val webClientId = parseWebClientId(jsonString)
                if (webClientId != null) {
                    cachedWebClientId = webClientId
                    return webClientId
                }
            } catch (e: Exception) {
            }
            
            val possiblePaths = listOf(
                "app/google-services.json",
                "../app/google-services.json",
                "../../app/google-services.json"
            )
            
            for (path in possiblePaths) {
                val file = File(path)
                if (file.exists()) {
                    val jsonString = file.readText()
                    val webClientId = parseWebClientId(jsonString)
                    if (webClientId != null) {
                        cachedWebClientId = webClientId
                        return webClientId
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_FIREBASE, "Error reading google-services.json", e)
            null
        }
    }
    
    /**
     * Parse Web Client ID từ JSON string
     */
    private fun parseWebClientId(jsonString: String): String? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val clients = jsonObject.getJSONArray("client")
            
            for (i in 0 until clients.length()) {
                val client = clients.getJSONObject(i)
                val oauthClients = client.optJSONArray("oauth_client")
                
                if (oauthClients != null && oauthClients.length() > 0) {
                    for (j in 0 until oauthClients.length()) {
                        val oauthClient = oauthClients.getJSONObject(j)
                        val clientType = oauthClient.optInt("client_type", -1)
                        
                        if (clientType == 3) {
                            val clientId = oauthClient.optString("client_id", null)
                            if (clientId != null) {
                                return clientId
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(AppConstants.TAG_FIREBASE, "Error parsing JSON", e)
            null
        }
    }

    /**
     * Lấy Web Client ID
     * Nếu không đọc được từ file, sẽ sử dụng giá trị mặc định từ google-services.json
     */
    fun getDefaultWebClientId(context: Context): String {
        getWebClientId(context)?.let { return it }
        return "418677381412-qaj4anmde4eeibigrjcmjjotajji6h3n.apps.googleusercontent.com"
    }
}

