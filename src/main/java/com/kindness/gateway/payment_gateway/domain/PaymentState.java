package com.kindness.gateway.payment_gateway.domain;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

public enum PaymentState {
    AUTHORIZING, AUTHORIZED, DECLINED, CAPTURING, CAPTURED, VOIDING, VOIDED,
    REFUNDING, REFUNDED, EXPIRED, NEEDS_REVIEW;

    private static final Map<PaymentState, Set<PaymentState>> ALLOWED = Map.ofEntries(
            entry(AUTHORIZING, Set.of(AUTHORIZED, DECLINED, NEEDS_REVIEW)),
            entry(AUTHORIZED,  Set.of(CAPTURING, VOIDING, EXPIRED)),
            entry(CAPTURING,   Set.of(CAPTURED, AUTHORIZED, NEEDS_REVIEW)),
            entry(VOIDING,     Set.of(VOIDED, AUTHORIZED, NEEDS_REVIEW)),
            entry(CAPTURED,    Set.of(REFUNDING)),
            entry(REFUNDING,   Set.of(REFUNDED, CAPTURED, NEEDS_REVIEW)),
            entry(DECLINED,     Set.of()),
            entry(VOIDED,       Set.of()),
            entry(REFUNDED,     Set.of()),
            entry(EXPIRED,      Set.of()),
            entry(NEEDS_REVIEW, Set.of())
    );

    public boolean canTransitionTo(PaymentState next){
        return ALLOWED.get(this).contains(next);
    }

    public PaymentState transitionTo(PaymentState next) {
        if (!canTransitionTo(next)) {
            throw new InvalidTransitionException(this, next);
        }
        return next;
    }
}
