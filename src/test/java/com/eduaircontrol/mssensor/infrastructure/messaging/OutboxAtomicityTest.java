package com.eduaircontrol.mssensor.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Verifica la garantia que justifica el patron: el evento se escribe en la misma
 * transaccion que el cambio de negocio.
 *
 * <p>Se usa {@link TransactionTemplate} en vez de un servicio de prueba porque lo
 * que importa comprobar es la semantica de la transaccion compartida, no una clase
 * concreta.
 */
class OutboxAtomicityTest extends OutboxTestBase {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void eventSurvivesWhenTheBusinessTransactionCommits() {
        UUID eventId = transactionTemplate.execute(status ->
                appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        assertThat(repository.findById(eventId)).isPresent();
    }

    /**
     * Esta es la garantia central de ADR-007: si la transaccion de negocio falla,
     * el evento tampoco queda. Es lo que impide el estado intermedio "instalacion
     * guardada pero evento perdido".
     */
    @Test
    void eventDisappearsWhenTheBusinessTransactionRollsBack() {
        transactionTemplate.execute(status -> {
            appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            status.setRollbackOnly();
            return null;
        });

        assertThat(repository.count()).isZero();
    }

    @Test
    void rolledBackEventIsNeverRelayed() {
        transactionTemplate.execute(status -> {
            appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
            status.setRollbackOnly();
            return null;
        });

        relay.publishPending();

        assertThat(bodyOf()).isNull();
    }

    @Test
    void registersEveryAppendAsPending() {
        appendInstalled(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(repository.countByPublishedAtIsNull()).isEqualTo(1);
    }
}