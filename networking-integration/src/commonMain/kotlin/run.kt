import kotlinx.coroutines.CoroutineScope

expect fun runBlocking(body: suspend CoroutineScope.() -> Unit)
