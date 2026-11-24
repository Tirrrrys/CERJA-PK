package com.vibe.app.data.network

import com.vibe.app.data.model.ClientType
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fetches the list of available model IDs from an OpenAI-compatible or Anthropic
 * compatible endpoint (`GET {baseUrl}/v1/models`).
 *
 * Supports any custom base URL (OpenAI-compatible proxies, Ollama, LM Studio,
 * OpenRouter, one-api gateways, etc.).
 */
object ModelCatalogFetcher {

    data class FetchState(
        val isLoading: Boolean = false,
        val models: List<String> = emptyList(),
        val error: String? = null,
    )

    /**
     * Normalizes a user-entered base URL:
     * - adds `https://` when the scheme is missing
     * - trims whitespace and trailing slashes
     * - removes a trailing `/v1` so `/v1/models` is appended exactly once
     */
    fun normalizeBaseUrl(raw: String): String {
        var url = raw.trim()
        if (url.isEmpty()) return url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        while (url.endsWith("/")) {
            url = url.dropLast(1)
        }
        if (url.endsWith("/v1")) {
            url = url.dropLast(3)
        }
        return url
    }

    suspend fun fetchModels(
        rawBaseUrl: String,
        apiKey: String?,
        clientType: ClientType?,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeBaseUrl(rawBaseUrl)
            require(base.isNotBlank()) { "Base URL is empty" }
            val endpoint = "$base/v1/models"
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            if (!apiKey.isNullOrBlank()) {
                if (clientType == ClientType.ANTHROPIC || clientType == ClientType.MINIMAX) {
                    connection.setRequestProperty("x-api-key", apiKey)
                    connection.setRequestProperty("anthropic-version", "2023-06-01")
                } else {
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")
                }
            }
            val code = connection.responseCode
            val body = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            } finally {
                connection.disconnect()
            }
            check(code in 200..299) { "HTTP $code: ${body.take(160)}" }
            parseModelIds(body)
        }
    }

    /** Extracts `data[].id` (OpenAI style) or `data[].name` (Anthropic style). */
    fun parseModelIds(body: String): List<String> {
        val root = JSONObject(body)
        val data = root.optJSONArray("data") ?: return emptyList()
        val models = ArrayList<String>(data.length())
        for (i in 0 until data.length()) {
            val obj = data.optJSONObject(i) ?: continue
            val id = obj.optString("id", "").ifBlank { obj.optString("name", "") }
            if (id.isNotBlank()) models.add(id)
        }
        return models.sorted()
    }
}
