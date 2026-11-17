package com.kindness.gateway.payment_gateway.bank;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryingBankClientTest {

    private WireMockServer wireMock;
    private RetryingBankClient client;

    private final BankAuthorizeRequest request =
            new BankAuthorizeRequest("4111111111111111", "123", 12, 2030, 1000);

    private final BankCaptureRequest captureRequest =
            new BankCaptureRequest("auth-1", 1000);
    private final BankVoidRequest voidRequest =
            new BankVoidRequest("auth-1");
    private final BankRefundRequest refundRequest =
            new BankRefundRequest("cap-1", 1000);

    private static final String CAPTURE_OK =
            "{\"capture_id\":\"cap-1\",\"authorization_id\":\"auth-1\","
                    + "\"amount\":1000,\"captured_at\":\"2026-11-17T11:49:52.991Z\"}";

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(500));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .requestFactory(factory)
                .build();


        client = new RetryingBankClient(new BankClient(restClient));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void retriesTransientFailureThenSucceeds() {

        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .inScenario("retry").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_error\"}"))
                .willSetStateTo("second"));

        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .inScenario("retry").whenScenarioStateIs("second")
                .willReturn(okJson("{\"authorization_id\":\"auth-1\",\"expires_at\":\"2026-12-01T10:00:00Z\"}")));

        BankAuthorizeResponse response = client.authorize(request, "authorize-123");

        assertThat(response.authorizationId()).isEqualTo("auth-1");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/api/v1/authorizations")));
    }

    @Test
    void doesNotRetryPermanentFailure() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(aResponse().withStatus(402)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"insufficient_funds\",\"message\":\"Not enough funds\"}")));

        assertThatThrownBy(() -> client.authorize(request, "authorize-123"))
                .isInstanceOf(BankPermanentException.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/authorizations")));
    }

    @Test
    void givesUpAfterThreeAttempts() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_error\"}")));

        assertThatThrownBy(() -> client.authorize(request, "authorize-123"))
                .isInstanceOf(BankTransientException.class);

        wireMock.verify(3, postRequestedFor(urlEqualTo("/api/v1/authorizations")));
    }

    @Test
    void captureDoesNotRetryPermanentFailure() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/captures"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"authorization_already_used\",\"message\":\"Already used\"}")));

        assertThatThrownBy(() -> client.capture(captureRequest, "capture-123"))
                .isInstanceOfSatisfying(BankPermanentException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("authorization_already_used"));

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/captures")));
    }

    @Test
    void captureRetriesTransientFailureWithSameKey() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/captures"))
                .inScenario("capture-retry").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_error\"}"))
                .willSetStateTo("second"));

        wireMock.stubFor(post(urlEqualTo("/api/v1/captures"))
                .inScenario("capture-retry").whenScenarioStateIs("second")
                .willReturn(okJson(CAPTURE_OK)));

        BankCaptureResponse response = client.capture(captureRequest, "capture-123");

        assertThat(response.captureId()).isEqualTo("cap-1");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/api/v1/captures"))
                .withHeader("Idempotency-Key", equalTo("capture-123")));
    }

    @Test
    void voidDoesNotRetryPermanentFailure() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/voids"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"authorization_already_used\",\"message\":\"Already used\"}")));

        assertThatThrownBy(() -> client.voidAuthorization(voidRequest, "void-123"))
                .isInstanceOfSatisfying(BankPermanentException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("authorization_already_used"));

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/voids")));
    }

    @Test
    void refundDoesNotRetryPermanentFailure() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/refunds"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"amount_mismatch\"," +
                                "\"message\":\"Different Amounts used\"}")));

        assertThatThrownBy(() -> client.refund(refundRequest, "refund-123"))
                .isInstanceOfSatisfying(BankPermanentException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(
                                "amount_mismatch"));

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/refunds")));
    }
}