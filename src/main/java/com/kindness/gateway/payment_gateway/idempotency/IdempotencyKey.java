package com.kindness.gateway.payment_gateway.idempotency;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", length = 255)
    private String key;

    @Column(nullable = false, length = 20)
    private String operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String requestHash;

    @Column(nullable = false, length = 15)
    private String status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String key, String operation, String requestHash) {
        this.key = key;
        this.operation = operation;
        this.requestHash = requestHash;
        this.status = "IN_PROGRESS";
    }

    public void complete(int responseStatus, String responseBody) {
        this.status = "COMPLETED";
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.completedAt = Instant.now();
    }

    public String getKey() {
        return key;
    }

    public String getOperation() {
        return operation;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getStatus() {
        return status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}