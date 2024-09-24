import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runBlocking(body: suspend CoroutineScope.() -> Unit): Unit {
    GlobalScope.promise { body() }
    return Unit
}
