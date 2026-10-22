package com.cineflow.tube.data.repository

import com.cineflow.tube.data.model.StreamingInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Dynamic Custom Instance Selector & Latency Ping Tester:
 * - Concurrently tests HTTP round-trip latency of all public & custom streaming instances
 * - Auto-routes to the fastest responsive server with zero manual configuration
 * - Validates user-entered custom instance URLs with instant ping feedback
 */
class InstanceHealthManager(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()
) {
    companion object {
        val PIPED_INSTANCES = listOf(
            StreamingInstance("https://pipedapi.kavin.rocks", "Kavin (Global)", "PIPED"),
            StreamingInstance("https://api.piped.privacy.com.de", "Privacy DE (Europe)", "PIPED"),
            StreamingInstance("https://pipedapi.tokhmi.xyz", "Tokhmi (Asia/US)", "PIPED"),
            StreamingInstance("https://api.piped.projectsegfau.lt", "Project Segfault (US)", "PIPED"),
            StreamingInstance("https://piped-api.garudalinux.org", "Garuda Linux (India/Global)", "PIPED"),
            StreamingInstance("https://pipedapi.rivo.cc", "Rivo (Europe)", "PIPED")
        )

        val INVIDIOUS_INSTANCES = listOf(
            StreamingInstance("https://invidious.nerdvpn.de", "NerdVPN (Germany)", "INVIDIOUS"),
            StreamingInstance("https://inv.tux.pizza", "Tux Pizza (US)", "INVIDIOUS"),
            StreamingInstance("https://invidious.jing.rocks", "Jing (Japan/Asia)", "INVIDIOUS"),
            StreamingInstance("https://vid.puffyan.us", "Puffyan (Global)", "INVIDIOUS"),
            StreamingInstance("https://invidious.projectsegfau.lt", "Project Segfault (US)", "INVIDIOUS")
        )
    }

    private val _pipedInstances = MutableStateFlow(PIPED_INSTANCES)
    val pipedInstances: StateFlow<List<StreamingInstance>> = _pipedInstances.asStateFlow()

    private val _invidiousInstances = MutableStateFlow(INVIDIOUS_INSTANCES)
    val invidiousInstances: StateFlow<List<StreamingInstance>> = _invidiousInstances.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    suspend fun pingInstance(instance: StreamingInstance): StreamingInstance = withContext(Dispatchers.IO) {
        val testUrl = if (instance.type == "PIPED") {
            "${instance.url.trimEnd('/')}/trending?region=US"
        } else {
            "${instance.url.trimEnd('/')}/api/v1/trending"
        }

        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(testUrl)
                .head()
                .build()

            client.newCall(request).execute().use { response ->
                val ping = System.currentTimeMillis() - start
                val healthy = response.isSuccessful || response.code in 200..399
                instance.copy(
                    pingMs = if (healthy) ping else -1,
                    isHealthy = healthy
                )
            }
        } catch (_: Exception) {
            // Fallback to quick GET if HEAD is not supported by instance reverse-proxy
            try {
                val getRequest = Request.Builder()
                    .url(testUrl)
                    .build()
                client.newCall(getRequest).execute().use { response ->
                    val ping = System.currentTimeMillis() - start
                    val healthy = response.isSuccessful
                    instance.copy(
                        pingMs = if (healthy) ping else -1,
                        isHealthy = healthy
                    )
                }
            } catch (_: Exception) {
                instance.copy(pingMs = -1, isHealthy = false)
            }
        }
    }

    suspend fun testAllInstances(): Unit = coroutineScope {
        _isTesting.value = true
        try {
            // Test Piped concurrently
            val pipedDeferred = _pipedInstances.value.map { inst ->
                async { pingInstance(inst) }
            }
            val testedPiped = pipedDeferred.awaitAll().sortedWith(
                compareBy<StreamingInstance> { !it.isHealthy }.thenBy { it.pingMs }
            )
            _pipedInstances.value = testedPiped

            // Test Invidious concurrently
            val invidiousDeferred = _invidiousInstances.value.map { inst ->
                async { pingInstance(inst) }
            }
            val testedInvidious = invidiousDeferred.awaitAll().sortedWith(
                compareBy<StreamingInstance> { !it.isHealthy }.thenBy { it.pingMs }
            )
            _invidiousInstances.value = testedInvidious
        } finally {
            _isTesting.value = false
        }
    }

    fun getFastestInstance(type: BackendType): StreamingInstance? {
        val list = if (type == BackendType.PIPED) _pipedInstances.value else _invidiousInstances.value
        return list.firstOrNull { it.isHealthy && it.pingMs > 0 }
    }

    suspend fun addCustomInstance(url: String, name: String, type: BackendType): Boolean {
        val cleanUrl = url.trim().trimEnd('/')
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) return false

        val newInstance = StreamingInstance(
            url = cleanUrl,
            name = if (name.isNotBlank()) name else "Custom Instance",
            type = type.name,
            isCustom = true
        )

        val tested = pingInstance(newInstance)
        if (type == BackendType.PIPED) {
            _pipedInstances.value = listOf(tested) + _pipedInstances.value.filter { it.url != cleanUrl }
        } else {
            _invidiousInstances.value = listOf(tested) + _invidiousInstances.value.filter { it.url != cleanUrl }
        }
        return tested.isHealthy
    }

    fun removeCustomInstance(url: String, type: BackendType) {
        if (type == BackendType.PIPED) {
            _pipedInstances.value = _pipedInstances.value.filter { !(it.url == url && it.isCustom) }
        } else {
            _invidiousInstances.value = _invidiousInstances.value.filter { !(it.url == url && it.isCustom) }
        }
    }
}
