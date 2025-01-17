package com.intellij.ide.starter.tests.unit

import com.intellij.ide.starter.bus.Signal
import com.intellij.ide.starter.bus.StarterBus
import com.intellij.ide.starter.bus.StarterListener
import com.intellij.ide.starter.bus.subscribe
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

class EventBusTest {
  private var isEventHappened: AtomicBoolean = AtomicBoolean(false)

  private fun checkIsEventFired(shouldEventBeFired: Boolean, isEventFiredGetter: () -> Boolean) {
    val shouldNotMessage = if (!shouldEventBeFired) "NOT" else ""

    withClue("Event should $shouldNotMessage be fired") {
      runBlocking {
        try {
          withTimeout(timeout = Duration.seconds(10)) {
            while (shouldEventBeFired != isEventFiredGetter()) {
              delay(Duration.milliseconds(500))
            }
          }
        }
        catch (_: Exception) {
        }
      }

      isEventFiredGetter().shouldBe(shouldEventBeFired)
    }
  }

  @Before
  fun beforeEach() {
    isEventHappened.set(false)
  }

  @After
  fun afterEach() {
    StarterListener.unsubscribe()
  }

  @Test
  fun filteringEventsByTypeIsWorking() {
    StarterListener.subscribe { event: Signal ->
      isEventHappened.set(true)
    }

    StarterBus.post(2)
    checkIsEventFired(false) { isEventHappened.get() }

    StarterBus.post(Signal())
    checkIsEventFired(true) { isEventHappened.get() }
  }
}