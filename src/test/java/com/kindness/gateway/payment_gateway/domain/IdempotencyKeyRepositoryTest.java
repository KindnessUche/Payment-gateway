package com.kindness.gateway.payment_gateway.domain;

import com.kindness.gateway.payment_gateway.idempotency.IdempotencyKey;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class IdempotencyKeyRepositoryTest {

    private static final String HASH = "a".repeat(64);

    @Autowired
    EntityManager entityManager;

    @Test
    void sameKeyTwiceIsRejectedByThePrimaryKey() {
        entityManager.persist(new IdempotencyKey("auth-123", "AUTHORIZE", HASH));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> {
            entityManager.persist(new IdempotencyKey("auth-123", "AUTHORIZE", HASH));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void completingAKeyStoresTheResponse() {
        IdempotencyKey key = new IdempotencyKey("auth-456", "AUTHORIZE", HASH);
        entityManager.persist(key);
        key.complete(201, "{\"ok\":true}");
        entityManager.flush();
        entityManager.clear();

        IdempotencyKey loaded = entityManager.find(IdempotencyKey.class, "auth-456");
        assertThat(loaded.getStatus()).isEqualTo("COMPLETED");
        assertThat(loaded.getResponseStatus()).isEqualTo(201);
    }
}