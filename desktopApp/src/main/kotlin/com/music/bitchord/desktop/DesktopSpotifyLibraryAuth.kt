package com.music.bitchord.desktop

import com.music.bitchord.data.Http
import com.music.bitchord.data.spotify.SpotifyLibrary
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import netscape.javascript.JSObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.nio.file.Files
import java.util.Base64
import java.util.UUID

/**
 * Spotify's mobile v1.8.1 token flow, adapted to the desktop's JavaFX WebEngine.
 *
 * Do not mint a token by reproducing Spotify's TOTP request here. The mobile release found that
 * such a token can be returned successfully but then rejected by Spotify's downstream endpoints.
 * Instead, let the actual web player mint its own token and capture the /api/token response.
 */
internal object DesktopSpotifyLibraryAuth {
    private const val BRIDGE_NAME = "BitChordSpotifyTokenBridge"
    private const val TOKEN_TIMEOUT_MS = 20_000L
    private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L
    private const val CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val harvestMutex = Mutex()

    @Volatile private var cachedCookie: String? = null
    @Volatile private var cachedAccessToken: String? = null
    @Volatile private var accessTokenExpiresAtMs = 0L
    @Volatile private var cachedClientId: String? = null
    @Volatile private var cachedClientToken: String? = null
    @Volatile private var clientTokenExpiresAtMs = 0L
    @Volatile private var cachedSession: SessionInfo? = null

    private data class SessionInfo(val clientVersion: String, val deviceId: String)
    private data class HarvestedToken(val token: String, val expiresAt: Long, val clientId: String?)

    suspend fun tokens(): SpotifyLibrary.Tokens? = withContext(Dispatchers.IO) {
        val cookie = DesktopSpotifyToken.cookie().ifBlank { return@withContext null }
        useCookie(cookie)
        val bearer = accessToken(cookie) ?: return@withContext null
        SpotifyLibrary.Tokens(bearer, clientToken())
    }

    /** Cached companion header for the synchronous HTTP helpers in the Canvas provider. */
    fun cachedClientToken(): String? =
        cachedClientToken?.takeIf { System.currentTimeMillis() < clientTokenExpiresAtMs - 30_000 }

    private fun useCookie(cookie: String) {
        if (cachedCookie == cookie) return
        synchronized(this) {
            if (cachedCookie == cookie) return
            cachedCookie = cookie
            cachedAccessToken = null
            accessTokenExpiresAtMs = 0L
            cachedClientId = null
            cachedClientToken = null
            clientTokenExpiresAtMs = 0L
            cachedSession = null
        }
    }

    private suspend fun accessToken(cookie: String): String? {
        val now = System.currentTimeMillis()
        cachedAccessToken?.let { if (now < accessTokenExpiresAtMs - 30_000) return it }

        return harvestMutex.withLock {
            val stillNow = System.currentTimeMillis()
            cachedAccessToken?.let { if (stillNow < accessTokenExpiresAtMs - 30_000) return@withLock it }

            val harvested = harvestViaWebEngine(cookie)
            if (harvested == null) {
                DesktopTrackLog.log("spotify: web-player token harvest failed or timed out")
                return@withLock null
            }
            if (cachedCookie != cookie) return@withLock null
            cachedAccessToken = harvested.token
            accessTokenExpiresAtMs = harvested.expiresAt
            harvested.clientId?.let { cachedClientId = it }
            DesktopTrackLog.log("spotify: web-player token harvested")
            harvested.token
        }
    }

    /**
     * Opens a fresh web-player profile, applies the user's saved session cookie, and observes the
     * real token response. The page may mint an anonymous token first; only an authenticated token
     * is accepted, matching the mobile implementation.
     */
    private suspend fun harvestViaWebEngine(cookie: String): HarvestedToken? {
        val result = CompletableDeferred<HarvestedToken?>()
        val engineReady = CompletableDeferred<Unit>()
        val profile = runCatching { Files.createTempDirectory("bitchord-spotify-token-") }.getOrNull()
            ?: return null
        val engineRef = arrayOfNulls<WebEngine>(1)

        try {
            Platform.runLater {
                try {
                    DesktopWebCookies.put(
                        URI("https://open.spotify.com/"),
                        mapOf(
                            "Set-Cookie" to listOf(
                                "sp_dc=" + cookie + "; Domain=.spotify.com; Path=/; Secure; HttpOnly",
                            ),
                        ),
                    )

                    val engine = WebEngine()
                    engineRef[0] = engine
                    engine.userAgent = CANVAS_UA
                    engine.userDataDirectory = profile.toFile()

                    val bridge = DesktopSpotifyTokenBridge { payload ->
                        acceptTokenPayload(payload, result)
                    }
                    var reloadAttempted = false

                    fun installHook() {
                        runCatching {
                            val window = engine.executeScript("window") as JSObject
                            window.setMember(BRIDGE_NAME, bridge)
                            engine.executeScript(HOOK_SCRIPT)
                        }
                    }

                    engine.loadWorker.stateProperty().addListener { _, _, state ->
                        when (state) {
                            Worker.State.SCHEDULED, Worker.State.RUNNING -> {
                                // JavaFX has no Android-style onPageStarted callback. Inject at the
                                // earliest load-worker transitions and reassert after document load.
                                installHook()
                            }
                            Worker.State.SUCCEEDED -> {
                                installHook()
                                if (!result.isCompleted && !reloadAttempted) {
                                    // The first navigation can finish before JavaFX exposes the
                                    // page context to the listener. Its profile is new, so clearing
                                    // storage and reloading gives the hook another chance to observe
                                    // the web player's authenticated token mint.
                                    reloadAttempted = true
                                    runCatching {
                                        engine.executeScript(
                                            "try { localStorage.clear(); sessionStorage.clear(); } catch (e) {}",
                                        )
                                    }
                                    Platform.runLater { runCatching { engine.reload() } }
                                }
                            }
                            Worker.State.FAILED, Worker.State.CANCELLED -> {
                                if (!result.isCompleted) result.complete(null)
                            }
                            else -> Unit
                        }
                    }

                    engine.load("https://open.spotify.com/")
                    engineReady.complete(Unit)
                } catch (failure: Throwable) {
                    engineReady.completeExceptionally(failure)
                    result.completeExceptionally(failure)
                }
            }

            return withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
                engineReady.await()
                result.await()
            }
        } catch (failure: Throwable) {
            DesktopTrackLog.log("spotify: token WebEngine failed: " + (failure.message ?: failure.javaClass.simpleName))
            return null
        } finally {
            runCatching {
                Platform.runLater {
                    engineRef[0]?.let { engine ->
                        runCatching { engine.load("about:blank") }
                    }
                }
            }
            withContext(Dispatchers.IO) {
                runCatching {
                    if (!profile.toFile().deleteRecursively()) {
                        Runtime.getRuntime().addShutdownHook(Thread { profile.toFile().deleteRecursively() })
                    }
                }
            }
        }
    }

    /** Receives raw /api/token response bodies from the Spotify page. */
    private fun acceptTokenPayload(payload: String?, result: CompletableDeferred<HarvestedToken?>) {
        if (payload.isNullOrBlank() || result.isCompleted) return
        runCatching {
            val root = json.parseToJsonElement(payload).jsonObject
            val token = root["accessToken"]?.jsonPrimitive?.contentOrNull
            val anonymous = root["isAnonymous"]?.jsonPrimitive?.contentOrNull
                ?.toBooleanStrictOrNull() ?: false
            if (token.isNullOrBlank() || anonymous) return
            val expiresAt = root["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.contentOrNull
                ?.toLongOrNull()?.takeIf { it > System.currentTimeMillis() }
                ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
            val clientId = root["clientId"]?.jsonPrimitive?.contentOrNull
            result.complete(HarvestedToken(token, expiresAt, clientId))
        }
    }

    private val HOOK_SCRIPT = """
        (function () {
          if (window.__bitchordTokenHook) return;
          window.__bitchordTokenHook = true;
          var report = function (body) {
            try { window.BitChordSpotifyTokenBridge.onTokenPayload(String(body)); } catch (e) {}
          };
          var isToken = function (u) {
            try { return String(u).indexOf('/api/token') !== -1; } catch (e) { return false; }
          };
          var originalFetch = window.fetch;
          if (originalFetch) {
            window.fetch = function (input, init) {
              var url = (input && input.url) ? input.url : input;
              var response = originalFetch.apply(this, arguments);
              if (isToken(url)) {
                try {
                  response.then(function (res) {
                    res.clone().text().then(report).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              }
              return response;
            };
          }
          var originalOpen = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__bitchordUrl = url;
            return originalOpen.apply(this, arguments);
          };
          var originalSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.send = function () {
            var xhr = this;
            try {
              xhr.addEventListener('load', function () {
                if (isToken(xhr.__bitchordUrl)) {
                  try { report(xhr.responseText); } catch (e) {}
                }
              });
            } catch (e) {}
            return originalSend.apply(this, arguments);
          };
        })();
    """.trimIndent()

    @Synchronized
    private fun clientToken(): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.let { if (now < clientTokenExpiresAtMs - 30_000) return it }
        val clientId = cachedClientId ?: return null
        val session = session() ?: return null
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val platform = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "macos"
            else -> "linux"
        }
        val payload = buildJsonObject {
            putJsonObject("client_data") {
                put("client_version", session.clientVersion)
                put("client_id", clientId)
                putJsonObject("js_sdk_data") {
                    put("device_brand", "unknown")
                    put("device_model", "unknown")
                    put("os", platform)
                    put("os_version", System.getProperty("os.version").orEmpty())
                    put("device_id", session.deviceId)
                    put("device_type", "computer")
                }
            }
        }
        val request = Request.Builder()
            .url(CLIENT_TOKEN_URL)
            .post(payload.toString().toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("User-Agent", CANVAS_UA)
            .build()
        val body = runCatching {
            Http.client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull() ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["response_type"]?.jsonPrimitive?.contentOrNull != "RESPONSE_GRANTED_TOKEN_RESPONSE") return null
        val granted = root["granted_token"]?.jsonObject ?: return null
        val token = granted["token"]?.jsonPrimitive?.contentOrNull ?: return null
        val ttlSeconds = granted["expires_after_seconds"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L
        cachedClientToken = token
        clientTokenExpiresAtMs = now + ttlSeconds * 1000
        return token
    }

    /** The current web-player version and its per-session device id, as the mobile release does. */
    private fun session(): SessionInfo? {
        cachedSession?.let { return it }
        val request = Request.Builder()
            .url("https://open.spotify.com")
            .header("User-Agent", CANVAS_UA)
            .build()
        val response = runCatching {
            Http.client.newCall(request).execute().use { result ->
                val html = result.body?.string()
                val spT = result.headers("Set-Cookie").firstNotNullOfOrNull { header ->
                    Regex("""sp_t=([^;]+)""").find(header)?.groupValues?.get(1)
                }
                html to spT
            }
        }.getOrNull() ?: return null
        val html = response.first ?: return null
        val configB64 = Regex("""<script id="appServerConfig" type="text/plain">([^<]+)</script>""")
            .find(html)?.groupValues?.get(1) ?: return null
        val clientVersion = runCatching {
            val config = String(Base64.getDecoder().decode(configB64), Charsets.UTF_8)
            json.parseToJsonElement(config).jsonObject["clientVersion"]?.jsonPrimitive?.contentOrNull
        }.getOrNull() ?: return null
        return SessionInfo(clientVersion, response.second ?: UUID.randomUUID().toString()).also {
            cachedSession = it
        }
    }

}


/** Public JVM bridge surface required by JavaFX's JavaScript-to-Java bridge. */
internal class DesktopSpotifyTokenBridge(private val handler: (String?) -> Unit) {
    fun onTokenPayload(payload: String?) = handler(payload)
}
