package com.kindness.gateway.payment_gateway.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentStateTest {

    @ParameterizedTest
    @CsvSource({
            "AUTHORIZING, AUTHORIZED",
            "AUTHORIZING, DECLINED",
            "AUTHORIZING, NEEDS_REVIEW",
            "AUTHORIZED, CAPTURING",
            "AUTHORIZED, VOIDING",
            "AUTHORIZED, EXPIRED",
            "CAPTURING, CAPTURED",
            "CAPTURING, AUTHORIZED",
            "CAPTURING, NEEDS_REVIEW",
            "VOIDING, VOIDED",
            "VOIDING, AUTHORIZED",
            "VOIDING, NEEDS_REVIEW",
            "CAPTURED, REFUNDING",
            "REFUNDING, REFUNDED",
            "REFUNDING, CAPTURED",
            "REFUNDING, NEEDS_REVIEW"
    })
    void allowedMoves(PaymentState from, PaymentState to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "CAPTURED, VOIDING",
            "CAPTURED, AUTHORIZED",
            "CAPTURED, CAPTURING",
            "AUTHORIZED, REFUNDING",
            "AUTHORIZED, CAPTURED",
            "AUTHORIZED, NEEDS_REVIEW",
            "AUTHORIZING, CAPTURING",
            "DECLINED, AUTHORIZING",
            "VOIDED, CAPTURING",
            "REFUNDED, REFUNDING",
            "EXPIRED, CAPTURING",
            "NEEDS_REVIEW, AUTHORIZED"
    })
    void forbiddenMoves(PaymentState from, PaymentState to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void forbiddenMoveThrowsWithFromAndTo() {
        assertThatThrownBy(() -> PaymentState.CAPTURED.transitionTo(PaymentState.VOIDING))
                .isInstanceOfSatisfying(InvalidTransitionException.class, e -> {
                    assertThat(e.getFrom()).isEqualTo(PaymentState.CAPTURED);
                    assertThat(e.getTo()).isEqualTo(PaymentState.VOIDING);
                });
    }

    @Test
    void allowedMoveReturnsNewState() {
        assertThat(PaymentState.AUTHORIZED.transitionTo(PaymentState.CAPTURING))
                .isEqualTo(PaymentState.CAPTURING);
    }

    @Test
    void everyStateHasAnEntryInTheTable() {
        for (PaymentState state : PaymentState.values()) {
            assertThat(state.canTransitionTo(PaymentState.NEEDS_REVIEW)).isNotNull();
        }
    }
}