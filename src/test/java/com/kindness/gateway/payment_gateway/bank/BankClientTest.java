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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankClientTest {

    private WireMockServer wireMock;
    private BankClient client;

    private final BankAuthorizeRequest request =
            new BankAuthorizeRequest("4111111111111111", "123", 12, 2030, 1000);

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        factory.setReadTimeout(Duration.ofMillis(500));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .requestFactory(factory)
                .build();

        client = new BankClient(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void successReturnsAuthorizationId() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(okJson("{\"authorization_id\":\"auth-1\",\"expires_at\":\"2026-12-01T10:00:00Z\"}")));

        BankAuthorizeResponse response = client.authorize(request, "authorize-123");

        assertThat(response.authorizationId()).isEqualTo("auth-1");
        wireMock.verify(postRequestedFor(urlEqualTo("/api/v1/authorizations"))
                .withHeader("Idempotency-Key", equalTo("authorize-123")));
    }

    @Test
    void insufficientFundsIsPermanent() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(aResponse().withStatus(402)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"insufficient_funds\",\"message\":\"Not enough funds\"}")));

        assertThatThrownBy(() -> client.authorize(request, "authorize-123"))
                .isInstanceOfSatisfying(BankPermanentException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(402);
                    assertThat(e.getErrorCode()).isEqualTo("insufficient_funds");
                });
    }

    @Test
    void serverErrorIsTransient() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal_error\",\"message\":\"Random failure injection\"}")));

        assertThatThrownBy(() -> client.authorize(request, "authorize-123"))
                .isInstanceOf(BankTransientException.class);
    }

    @Test
    void timeoutIsTransient() {
        wireMock.stubFor(post(urlEqualTo("/api/v1/authorizations"))
                .willReturn(okJson("{\"authorization_id\":\"auth-1\",\"expires_at\":\"2026-12-01T10:00:00Z\"}")
                        .withFixedDelay(1500)));

        assertThatThrownBy(() -> client.authorize(request, "authorize-123"))
                .isInstanceOf(BankTransientException.class)
                .hasCauseInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    }
}