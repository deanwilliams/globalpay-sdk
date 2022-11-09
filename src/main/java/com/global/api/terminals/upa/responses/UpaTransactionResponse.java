package com.global.api.terminals.upa.responses;

import com.global.api.entities.enums.ApplicationCryptogramType;
import com.global.api.entities.enums.CardType;
import com.global.api.terminals.TerminalResponse;
import com.global.api.utils.JsonDoc;

public class UpaTransactionResponse extends TerminalResponse {
    public UpaTransactionResponse(JsonDoc responseData) {
        JsonDoc cmdResult = responseData.get("cmdResult");
        if (cmdResult != null) {
            status = cmdResult.getString("result");
            deviceResponseCode = cmdResult.getString("errorCode");
            deviceResponseText = cmdResult.getString("errorMessage");
        }

        transactionType = responseData.getString("response");

        JsonDoc host = responseData.get("data").get("host");
        if (host != null) {
            amountDue = host.getDecimal("balanceDue");
            approvalCode = host.getString("approvalCode");
            avsResponseCode = host.getString("AvsResultCode");
            avsResponseText = host.getString("AvsResultText");
            balanceAmount = host.getDecimal("availableBalance");
            cardBrandTransactionId = host.getString("cardBrandTransId");
            responseCode = host.getString("responseCode");
            responseText = host.getString("responseText");
            terminalRefNumber = host.getString("tranNo");
            token = host.getString("tokenValue");
            transactionId = host.getString("referenceNumber");
            transactionAmount = host.getDecimal("totalAmount");
        }

        JsonDoc payment = responseData.get("data").get("payment");
        if (payment != null) { // is null on decline response
            cardHolderName = payment.getString("cardHolderName");

            if (payment.getString("cardType") != null) {
                switch (payment.getString("cardType").toUpperCase()) {
                    case "VISA":
                        cardType = CardType.VISA;
                        break;
                    case "MASTERCARD":
                        cardType = CardType.MC;
                        break;
                    case "DISCOVER":
                        cardType = CardType.DISC;
                        break;
                    case "AMERICAN EXPRESS":
                        cardType = CardType.AMEX;
                        break;
                    default:
                        break;
                }
            }

            entryMethod = payment.getString("cardAcquisition");
            maskedCardNumber = payment.getString("maskedPan");
            paymentType = payment.getString("cardGroup");    
        }

        JsonDoc transaction = responseData.get("data").get("transaction");
        if (transaction != null) {
            if (transaction.getDecimal("totalAmount") != null) {
                transactionAmount = transaction.getDecimal("totalAmount");
            }

            if (transaction.getDecimal("tipAmount") != null) {
                tipAmount = transaction.getDecimal("tipAmount");
            }
        }

        JsonDoc emv = responseData.get("data").get("emv");
        if (emv != null) {
            applicationCryptogram = emv.getString("9F26");

            if (emv.getString("9F27") != null) {
                switch (emv.getString("9F27")) {
                    case "0":
                        applicationCryptogramType = ApplicationCryptogramType.AAC;
                        break;
                    case "40":
                        applicationCryptogramType = ApplicationCryptogramType.TC;
                        break;
                    case "80":
                        applicationCryptogramType = ApplicationCryptogramType.ARQC;
                        break;
                    default:
                        break;
                }
            }

            applicationId = emv.getString("9F06");
            applicationLabel = emv.getString("50");
            applicationPreferredName = emv.getString("9F12");
        }
    }
}
