package gizz.tapes.storage

import io.ktor.client.HttpClient
import kotlin.jvm.JvmInline

// distinct from the app's HttpClient binding - needs its own HttpTimeout config since the
// app's client is tuned for small JSON calls, not multi-minute audio file downloads.
@JvmInline
value class DownloadHttpClient(val client: HttpClient)
