import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runBlocking(body: suspend CoroutineScope.() -> Unit) = runBlocking { body() }