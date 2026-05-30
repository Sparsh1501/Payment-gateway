package com.paygateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PaymentFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fullPaymentLifecycle() throws Exception {
        String email = "merchant-" + UUID.randomUUID() + "@example.com";

        // 1) register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"businessName":"Acme Co","email":"%s","password":"supersecret"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiKey").isNotEmpty());

        // 2) login
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"supersecret"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = json(login).path("data").path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        String bearer = "Bearer " + accessToken;

        // 3) create payment intent (with idempotency key)
        String idemKey = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/payment-intents")
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":49.99,"currency":"USD","provider":"STRIPE","metadata":{"orderId":"o-1"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn();
        String intentId = json(created).path("data").path("id").asText();

        // idempotent replay returns same intent
        mockMvc.perform(post("/api/v1/payment-intents")
                        .header("Authorization", bearer)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":49.99,"currency":"USD","provider":"STRIPE"}
                                """))
                .andExpect(jsonPath("$.data.id").value(intentId));

        // 4) confirm -> SUCCESS (simulation mode)
        mockMvc.perform(post("/api/v1/payment-intents/{id}/confirm", intentId)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.providerPaymentId").isNotEmpty());

        // 5) partial refund -> SUCCESS
        mockMvc.perform(post("/api/v1/refunds")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","amount":10.00,"reason":"partial"}
                                """.formatted(intentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        // refund exceeding remaining balance is rejected
        mockMvc.perform(post("/api/v1/refunds")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIntentId":"%s","amount":100.00}
                                """.formatted(intentId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REFUND_EXCEEDS_BALANCE"));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/payment-intents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
