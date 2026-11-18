package com.kindness.gateway.payment_gateway.domain;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EntityManager entityManager;

    private Payment newPayment(String orderId) {
        return new Payment(UUID.randomUUID(), orderId, "cust-1", 1000, "1111");
    }

    @Test
    void savesAndReadsBackAPayment() {
        Payment payment = newPayment("order-A");
        paymentRepository.save(payment);

        entityManager.flush();
        entityManager.clear();

        Payment loaded = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(loaded.getOrderId()).isEqualTo("order-A");
        assertThat(loaded.getAmountCents()).isEqualTo(1000);
        assertThat(loaded.getState()).isEqualTo(PaymentState.AUTHORIZING);
    }

    @Test
    void movedStateIsPersisted() {
        Payment payment = newPayment("order-B");
        paymentRepository.save(payment);

        payment.moveTo(PaymentState.AUTHORIZED);
        paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear();

        Payment loaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(loaded.getState()).isEqualTo(PaymentState.AUTHORIZED);
    }

    @Test
    void secondActivePaymentForSameOrderIsRejected() {
        paymentRepository.save(newPayment("order-C"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            paymentRepository.save(newPayment("order-C"));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("uq_payments_one_active_per_order");
    }
}