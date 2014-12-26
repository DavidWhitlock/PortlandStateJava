package edu.pdx.cs410J.grader.poa;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.eventbus.SubscriberExceptionContext;
import com.google.common.eventbus.SubscriberExceptionHandler;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UncaughtExceptionEventTest {

  @Test
  public void uncaughtExceptionInEventThreadCallsSubscriberExceptionHandler() {
    SubscriberExceptionHandler handler = mock(SubscriberExceptionHandler.class);
    EventBus bus = new EventBus(handler);

    final IllegalStateException exception = new IllegalStateException("Excepted Uncaught Exception");
    bus.register(new Object() {
      @Subscribe
      public void throwUncaughtException(TriggerUncaughtException event) {
        throw exception;
      }
    });

    bus.post(new TriggerUncaughtException());

    verify(handler).handleException(eq(exception), any(SubscriberExceptionContext.class));
  }

  private class TriggerUncaughtException {
  }

  @Test
  public void uncaughtExceptionInEventThreadFiresUncaughtExceptionEvent() {

  }
}
