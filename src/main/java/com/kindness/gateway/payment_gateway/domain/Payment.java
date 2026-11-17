package com.kindness.gateway.payment_gateway.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentState state;

    @Column(name = "card_last4", nullable = false, length = 4)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cardLast4;

    @Column(name = "bank_authorization_id")
    private String bankAuthorizationId;
    @Column(name = "bank_capture_id")
    private String bankCaptureId;
    @Column(name = "bank_void_id")
    private String bankVoidId;
    @Column(name = "bank_refund_id")
    private String bankRefundId;

    @Column(name = "authorization_expires_at")
    private Instant authorizationExpiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Column(name = "authorized_at")
    private Instant authorizedAt;
    @Column(name = "captured_at")
    private Instant capturedAt;
    @Column(name = "voided_at")
    private Instant voidedAt;
    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected Payment() {}

    public Payment(UUID id, String orderId, String customerId,
                   long amountCents, String cardLast4) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amountCents = amountCents;
        this.cardLast4 = cardLast4;
        this.state = PaymentState.AUTHORIZING;
    }


    public void moveTo(PaymentState next) {
        this.state = this.state.transitionTo(next);
        this.updatedAt = Instant.now();
    }

    // getters and setters: generate with IntelliJ (Alt+Insert)

}