import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

@OptIn(DelicateCoroutinesApi::class)
actual fun runBlocking(body: suspend CoroutineScope.() -> Unit) {
    GlobalScope.promise { body() }
    return
}
