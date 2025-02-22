package com.global.api.tests.realex;

import com.global.api.ServicesContainer;
import com.global.api.entities.TransactionSummary;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.serviceConfigs.GatewayConfig;
import com.global.api.services.ReportingService;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RealexReportingTests {
    public RealexReportingTests() throws ApiException {
        GatewayConfig config = new GatewayConfig();
        config.setMerchantId("heartlandgpsandbox");
        config.setAccountId("api");
        config.setSharedSecret("secret");
        config.setRefundPassword("refund");
        config.setServiceUrl("https://api.sandbox.realexpayments.com/epage-remote.cgi");
        config.setEnableLogging(true);

        ServicesContainer.configureService(config);
    }

    @Test
    public void GetTransactionDetail() throws ApiException {
        String transactionId = "u2RjrtEmaU2f0pB-aGw4Eg";

        TransactionSummary response =
                ReportingService
                        .transactionDetail(transactionId)
                        .execute();

        assertNotNull(response);
        assertEquals(transactionId, response.getOrderId());
    }

    @Test
    public void GetTransactionDetail_WithRandomId() throws ApiException {
        String orderId = UUID.randomUUID().toString();

        try {
            ReportingService
                    .transactionDetail(orderId)
                    .execute();
        } catch (GatewayException ex) {
            assertEquals("508", ex.getResponseCode());
            assertEquals("Original transaction not found.", ex.getResponseText());
        }
    }

}