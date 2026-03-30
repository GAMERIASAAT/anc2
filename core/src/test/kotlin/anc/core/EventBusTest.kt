package anc.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class EventBusTest : FunSpec({

    test("tryEmit and collect ConsoleOutput event") {
        val bus = EventBus()
        val received = mutableListOf<EventBus.Event>()

        val job = launch {
            bus.events.collect { received.add(it) }
        }

        delay(10) // let collector subscribe before emitting
        bus.tryEmit(EventBus.Event.ConsoleOutput("hello"))
        delay(50)
        job.cancel()

        received.any { it is EventBus.Event.ConsoleOutput && (it as EventBus.Event.ConsoleOutput).line == "hello" } shouldBe true
    }

    test("emit and first collect FrameworkStatus event") {
        val bus = EventBus()

        val result = runBlocking {
            val deferred = async {
                bus.events.first { it is EventBus.Event.FrameworkStatus }
            }
            delay(10)
            bus.emit(EventBus.Event.FrameworkStatus("ready"))
            withTimeout(1000L) { deferred.await() }
        }

        result.shouldBeInstanceOf<EventBus.Event.FrameworkStatus>()
        (result as EventBus.Event.FrameworkStatus).message shouldBe "ready"
    }

    test("ModuleOutput event carries module name and line") {
        val bus = EventBus()
        val event = EventBus.Event.ModuleOutput("exploits/test", "[+] Found open port")
        event.fullName shouldBe "exploits/test"
        event.line shouldBe "[+] Found open port"
    }

    test("SessionOpened event carries session info") {
        val event = EventBus.Event.SessionOpened(1, "shell", "192.168.1.100:4444")
        event.sessionId shouldBe 1
        event.type shouldBe "shell"
        event.remote shouldBe "192.168.1.100:4444"
    }

    test("SessionClosed event carries session id") {
        val event = EventBus.Event.SessionClosed(42)
        event.sessionId shouldBe 42
    }

    test("tryEmit does not throw when buffer has space") {
        val bus = EventBus()
        repeat(10) {
            bus.tryEmit(EventBus.Event.ConsoleOutput("line $it"))
        }
    }

    test("ConsoleOutput isError defaults to false") {
        val event = EventBus.Event.ConsoleOutput("msg")
        event.isError shouldBe false
    }

    test("ConsoleOutput isError can be set to true") {
        val event = EventBus.Event.ConsoleOutput("error msg", isError = true)
        event.isError shouldBe true
    }
})
