package com.silas.omaster.network

import android.content.Context
import android.util.Log
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

object PresetRemoteManager {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchPresets(url: String): PresetList? {
        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            val response: HttpResponse = client.get(url)
            val presets: PresetList = response.body()
            Log.d("PresetRemoteManager", "Fetched ${presets.presets.size} presets")
            presets
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to fetch presets", e)
            null
        }
    }

    suspend fun fetchAndSave(context: Context, url: String): Boolean {
        val presets = fetchPresets(url) ?: return false
        return try {
            withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "presets_remote.json")
                val jsonText = Json.encodeToString(PresetList.serializer(), presets)
                file.writeText(jsonText)
                Log.d("PresetRemoteManager", "Saved remote presets to ${file.absolutePath}")
            }
            true
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to save presets", e)
            false
        }
    }
}
