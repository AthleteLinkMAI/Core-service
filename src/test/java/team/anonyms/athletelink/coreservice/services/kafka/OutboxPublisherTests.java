package team.anonyms.athletelink.coreservice.services.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTests {

    @Mock OutboxDeliveryService delivery;

    @Test
    void stopsWhenThereAreNoMorePendingEvents() {
        when(delivery.publishNext()).thenReturn(true, false);

        new OutboxPublisher(delivery).publishPending();

        verify(delivery, times(2)).publishNext();
    }

    @Test
    void limitsOneScheduledRunToOneHundredEvents() {
        when(delivery.publishNext()).thenReturn(true);

        new OutboxPublisher(delivery).publishPending();

        verify(delivery, times(100)).publishNext();
    }

    @Test
    void stopsAfterDeliveryFailureSoNextRunCanRetry() {
        when(delivery.publishNext()).thenReturn(true).thenThrow(new IllegalStateException("broker unavailable"));

        assertDoesNotThrow(() -> new OutboxPublisher(delivery).publishPending());

        verify(delivery, times(2)).publishNext();
    }
}
