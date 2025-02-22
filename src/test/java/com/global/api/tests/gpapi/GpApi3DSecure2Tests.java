package com.global.api.tests.gpapi;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.BrowserData;
import com.global.api.entities.StoredCredential;
import com.global.api.entities.ThreeDSecure;
import com.global.api.entities.enums.*;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.entities.exceptions.GatewayException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.serviceConfigs.GpApiConfig;
import com.global.api.services.Secure3dService;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.global.api.tests.gpapi.BaseGpApiTest.GpApi3DSTestCards.CARD_AUTH_SUCCESSFUL_V2_2;
import static com.global.api.tests.gpapi.BaseGpApiTest.GpApi3DSTestCards.CARD_CHALLENGE_REQUIRED_V2_2;
import static org.junit.Assert.*;

public class GpApi3DSecure2Tests extends BaseGpApiTest {

    private final static String AVAILABLE = "AVAILABLE";
    private final static String CHALLENGE_REQUIRED = "CHALLENGE_REQUIRED";
    private final static String ENROLLED = "ENROLLED";
    private final static String SUCCESS_AUTHENTICATED = "SUCCESS_AUTHENTICATED";

    private CreditCardData card;
    private final Address shippingAddress;
    private final BrowserData browserData;

    private final BigDecimal amount = new BigDecimal("10.01");
    private final String currency = "GBP";

    public GpApi3DSecure2Tests() throws ConfigurationException {
        GpApiConfig config = new GpApiConfig();

        // GP-API settings
        config.setAppId(APP_ID);
        config.setAppKey(APP_KEY);
        config.setCountry("GB");
        config.setChallengeNotificationUrl("https://ensi808o85za.x.pipedream.net/");
        config.setMethodNotificationUrl("https://ensi808o85za.x.pipedream.net/");
        config.setMerchantContactUrl("https://enp4qhvjseljg.x.pipedream.net/");

        config.setEnableLogging(true);

        ServicesContainer.configureService(config, GP_API_CONFIG_NAME);

        // Create card data
        card = new CreditCardData();
        card.setNumber(CARD_CHALLENGE_REQUIRED_V2_2.cardNumber);
        card.setExpMonth(expMonth);
        card.setExpYear(expYear);
        card.setCardHolderName("John Smith");

        // Shipping address
        shippingAddress = new Address();

        shippingAddress.setStreetAddress1("Apartment 852");
        shippingAddress.setStreetAddress2("Complex 741");
        shippingAddress.setStreetAddress3("no");
        shippingAddress.setCity("Chicago");
        shippingAddress.setPostalCode("5001");
        shippingAddress.setState("IL");
        shippingAddress.setCountryCode("840");

        // Browser data
        browserData = new BrowserData();
        browserData.setAcceptHeader("text/html,application/xhtml+xml,application/xml;q=9,image/webp,img/apng,*/*;q=0.8");
        browserData.setColorDepth(ColorDepth.TwentyFourBit);
        browserData.setIpAddress("123.123.123.123");
        browserData.setJavaEnabled(true);
        browserData.setLanguage("en");
        browserData.setScreenHeight(1080);
        browserData.setScreenWidth(1920);
        browserData.setChallengeWindowSize(ChallengeWindowSize.Windowed_600x400);
        browserData.setTimezone("0");
        browserData.setUserAgent("Mozilla/5.0 (Windows NT 6.1; Win64, x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v22() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_WithIdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .checkEnrollment(card)
                    .withCurrency(currency)
                    .withAmount(amount)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_WithTokenizedCard() throws ApiException {
        CreditCardData tokenizedCard = new CreditCardData();
        tokenizedCard.setToken(card.tokenize(GP_API_CONFIG_NAME));

        assertNotNull(tokenizedCard.getToken());

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(tokenizedCard)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_AllPreferenceValues() throws ApiException {
        for (ChallengeRequestIndicator challengeRequestIndicator : ChallengeRequestIndicator.values()) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .withChallengeRequestIndicator(challengeRequestIndicator)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
            assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
//            assertNull(secureEcom.getPreference());
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_StoredCredentials() throws ApiException {
        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setInitiator(StoredCredentialInitiator.Merchant);
        storedCredential.setType(StoredCredentialType.Subscription);
        storedCredential.setSequence(StoredCredentialSequence.Subsequent);
        storedCredential.setReason(StoredCredentialReason.Incremental);

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .withStoredCredential(storedCredential)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_AllSources() throws ApiException {
        for (AuthenticationSource source : AuthenticationSource.values()) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .withAuthenticationSource(source)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
            assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_WithNullPaymentMethod() throws ApiException {
        card = new CreditCardData();

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .checkEnrollment(card)
                    .withCurrency(currency)
                    .withAmount(amount)
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("INVALID_REQUEST_DATA", ex.getResponseCode());
            assertEquals("40007", ex.getResponseText());
            assertEquals("Status Code: 400 - Request expects the following conditionally mandatory fields number,expiry_month,expiry_year.", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_WithIdempotencyKey() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);
        String idempotencyKey = UUID.randomUUID().toString();

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .checkEnrollment(card)
                    .withCurrency(currency)
                    .withAmount(amount)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_WithTokenizedCard() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        CreditCardData tokenizedCard = new CreditCardData();
        tokenizedCard.setToken(card.tokenize(GP_API_CONFIG_NAME));

        assertNotNull(tokenizedCard.getToken());

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(tokenizedCard)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_AllPreferenceValues() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        for (ChallengeRequestIndicator challengeRequestIndicator : ChallengeRequestIndicator.values()) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .withChallengeRequestIndicator(challengeRequestIndicator)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
            assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
        }
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_StoredCredentials() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        StoredCredential storedCredential = new StoredCredential();
        storedCredential.setInitiator(StoredCredentialInitiator.Merchant);
        storedCredential.setType(StoredCredentialType.Subscription);
        storedCredential.setSequence(StoredCredentialSequence.Subsequent);
        storedCredential.setReason(StoredCredentialReason.Incremental);

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .withStoredCredential(storedCredential)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
    }

    /**
     * Tests for 3DS v2 Card Enrolled - Initiate Auth
     */
    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_With_IdempotencyKey() throws ApiException {
        String idempotencyKey = UUID.randomUUID().toString();

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .withIdempotencyKey(idempotencyKey)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .initiateAuthentication(card, secureEcom)
                    .withAmount(amount)
                    .withCurrency(currency)
                    .withAuthenticationSource(AuthenticationSource.Browser)
                    .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                    .withOrderCreateDate(DateTime.now())
                    .withAddress(shippingAddress, AddressType.Shipping)
                    .withBrowserData(browserData)
                    .withIdempotencyKey(idempotencyKey)
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("DUPLICATE_ACTION", ex.getResponseCode());
            assertEquals("40039", ex.getResponseText());
            assertTrue(ex.getMessage().startsWith("Status Code: 409 - Idempotency Key seen before:"));
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_TokenizedCard() throws ApiException {
        CreditCardData tokenizedCard = new CreditCardData();
        tokenizedCard.setToken(card.tokenize(GP_API_CONFIG_NAME));

        assertNotNull(tokenizedCard.getToken());

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(tokenizedCard)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(tokenizedCard, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_MethodUrlSetNo() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.No)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_MethodUrlSetUnavailable() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Unavailable)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_Without_ShippingAddress() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        // Initiate authentication
        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Unavailable)
                        .withOrderCreateDate(DateTime.now())
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_Without_PaymentMethod() throws ApiException {
        ThreeDSecure secureEcom = new ThreeDSecure();
        secureEcom.setServerTransactionId("AUT_" + UUID.randomUUID().toString().replace("-", ""));
        card = new CreditCardData();

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .initiateAuthentication(card, secureEcom)
                    .withAmount(amount)
                    .withCurrency(currency)
                    .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                    .withOrderCreateDate(DateTime.now())
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("MANDATORY_DATA_MISSING", ex.getResponseCode());
            assertEquals("40005", ex.getResponseText());
            assertEquals("Status Code: 400 - Request expects the following fields number", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_NonExistentId() throws ApiException {
        String transId = UUID.randomUUID().toString();

        ThreeDSecure secureEcom = new ThreeDSecure();
        secureEcom.setServerTransactionId(transId);

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .initiateAuthentication(card, secureEcom)
                    .withAmount(amount)
                    .withCurrency(currency)
                    .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                    .withOrderCreateDate(DateTime.now())
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("RESOURCE_NOT_FOUND", ex.getResponseCode());
            assertEquals("40118", ex.getResponseText());
            assertEquals("Status Code: 404 - Authentication " + transId + " not found at this location.", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_AcsNotPerformed() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .withShippingMethod(ShippingMethod.DigitalGoods)
                        .withShippingNameMatchesCardHolderName(true)
                        .withShippingAddressCreateDate(DateTime.now())
                        .withShippingAddressUsageIndicator(AgeIndicator.LessThanThirtyDays)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());

        ThreeDSecure getAuthData =
                Secure3dService
                    .getAuthenticationData()
                    .withServerTransactionId(initAuth.getServerTransactionId())
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertEquals(CHALLENGE_REQUIRED, getAuthData.getStatus());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_AllSources() throws ApiException {

        // TODO: Add other sources:
        //  + AuthenticationSource.MerchantInitiated
        //  + AuthenticationSource.MobileSDK
        AuthenticationSource[] sources = {AuthenticationSource.Browser};

        for (AuthenticationSource source : sources) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);

            ThreeDSecure initAuth =
                    Secure3dService
                            .initiateAuthentication(card, secureEcom)
                            .withAmount(amount)
                            .withCurrency(currency)
                            .withAuthenticationSource(source)
                            .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                            .withOrderCreateDate(DateTime.now())
                            .withAddress(shippingAddress, AddressType.Shipping)
                            .withBrowserData(browserData)
//                            // TODO: Add merchant_initiated_request_type to be sent into the request
//                            .withMerchantInitiatedRequestType(MerchantInitiatedRequestType.RecurringTransaction)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
            assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_WithGiftCard() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .withGiftCardCount(1)
                        .withGiftCardAmount(new BigDecimal("2"))
                        .withGiftCardCurrency("GBP")
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_WithDeliveryEmail() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .withDeliveryEmail("gptest@gp.test")
                        .withDeliveryTimeFrame(DeliveryTimeFrame.ElectronicDelivery)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_WithShippingMethod() throws ApiException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .withShippingMethod(ShippingMethod.DigitalGoods)
                        .withShippingNameMatchesCardHolderName(true)
                        .withShippingAddressCreateDate(DateTime.now())
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);
        assertTrue(initAuth.isChallengeMandated());
//        assertEquals("05",secureEcom.getEci());
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_Initiate() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertNotNull(initAuth);
        assertEquals(ENROLLED, initAuth.getEnrolledStatus());
        assertEquals(Secure3dVersion.TWO, initAuth.getVersion());
        assertEquals(SUCCESS_AUTHENTICATED, initAuth.getStatus());
        assertEquals("YES", initAuth.getLiabilityShift());
        assertFalse(initAuth.isChallengeMandated());
        assertNotNull(initAuth.getIssuerAcsUrl());
        assertNotNull(initAuth.getPayerAuthenticationRequest());
        assertNotNull(initAuth.getChallengeReturnUrl());
        assertNotNull(initAuth.getMessageType());
        assertNotNull(initAuth.getSessionDataFieldName());
        assertEquals("05", initAuth.getEci());
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_Initiate_AllPreferenceValues() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        ChallengeRequestIndicator[] preferences = {
                ChallengeRequestIndicator.NoPreference,
                ChallengeRequestIndicator.NoChallengeRequested,
                ChallengeRequestIndicator.NoChallengeRequestedTransactionRiskAnalysisPerformed,
                ChallengeRequestIndicator.NoChallengeRequestedScaAlreadyPerformed,
                ChallengeRequestIndicator.NoChallengeRequestedWhiteList
        };

        for (ChallengeRequestIndicator preference : preferences) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .withChallengeRequestIndicator(preference)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
            assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
            assertFalse(secureEcom.isChallengeMandated());

            ThreeDSecure initAuth =
                    Secure3dService
                            .initiateAuthentication(card, secureEcom)
                            .withAmount(amount)
                            .withCurrency(currency)
                            .withChallengeRequestIndicator(preference)
                            .withOrderCreateDate(DateTime.now())
                            .withAddress(shippingAddress, AddressType.Shipping)
                            .withBrowserData(browserData)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertNotNull(initAuth);
            assertEquals(ENROLLED, initAuth.getEnrolledStatus());
            assertEquals(Secure3dVersion.TWO, initAuth.getVersion());
            assertNotNull(initAuth.getStatus());
            assertNotNull(initAuth.isChallengeMandated());
            assertNotNull(initAuth.getIssuerAcsUrl());
            assertNotNull(initAuth.getPayerAuthenticationRequest());
            assertNotNull(initAuth.getChallengeReturnUrl());
            assertNotNull(initAuth.getMessageType());
            assertNotNull(initAuth.getSessionDataFieldName());
            // assertEquals("05",secureEcom.getEci());
        }
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_Initiate_AllPreferenceValues_ChallengeRequired() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        ChallengeRequestIndicator[] preferences = {
                ChallengeRequestIndicator.ChallengePreferred,
                ChallengeRequestIndicator.ChallengeMandated,
                ChallengeRequestIndicator.ChallengeRequestedPromptForWhiteList
        };

        for (ChallengeRequestIndicator preference : preferences) {
            ThreeDSecure secureEcom =
                    Secure3dService
                            .checkEnrollment(card)
                            .withCurrency(currency)
                            .withAmount(amount)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);
            assertFalse(secureEcom.isChallengeMandated());

            ThreeDSecure initAuth =
                    Secure3dService
                            .initiateAuthentication(card, secureEcom)
                            .withAmount(amount)
                            .withCurrency(currency)
                            .withChallengeRequestIndicator(preference)
                            .withOrderCreateDate(DateTime.now())
                            .withAddress(shippingAddress, AddressType.Shipping)
                            .withBrowserData(browserData)
                            .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

            assertNotNull(initAuth);
            assertEquals(ENROLLED, initAuth.getEnrolledStatus());
            assertEquals(Secure3dVersion.TWO, initAuth.getVersion());
            assertNotNull(CHALLENGE_REQUIRED, initAuth.getStatus());
            assertNotNull(initAuth.isChallengeMandated());
            assertNotNull(initAuth.getIssuerAcsUrl());
            assertNotNull(initAuth.getPayerAuthenticationRequest());
            assertNotNull(initAuth.getChallengeReturnUrl());
            assertNotNull(initAuth.getMessageType());
            assertNotNull(initAuth.getSessionDataFieldName());
        }
    }

    @Test
    public void CardHolderEnrolled_Frictionless_v2_Initiate_DuplicateInitiate() throws ApiException {
        card.setNumber(CARD_AUTH_SUCCESSFUL_V2_2.cardNumber);

        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertNotNull(secureEcom);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
        assertEquals(Secure3dVersion.TWO, secureEcom.getVersion());
        assertEquals(SUCCESS_AUTHENTICATED, secureEcom.getStatus());
        assertEquals("YES", initAuth.getLiabilityShift());
        assertNotNull(secureEcom.getIssuerAcsUrl());
        assertNotNull(secureEcom.getPayerAuthenticationRequest());
        assertNotNull(secureEcom.getChallengeReturnUrl());
        assertNotNull(secureEcom.getMessageType());
        assertNotNull(secureEcom.getSessionDataFieldName());

        boolean exceptionCaught = false;
        try {
            Secure3dService
                    .initiateAuthentication(card, secureEcom)
                    .withAmount(amount)
                    .withCurrency(currency)
                    .withAuthenticationSource(AuthenticationSource.Browser)
                    .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                    .withOrderCreateDate(DateTime.now())
                    .withAddress(shippingAddress, AddressType.Shipping)
                    .withBrowserData(browserData)
                    .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);
        } catch (GatewayException ex) {
            exceptionCaught = true;
            assertEquals("SYSTEM_ERROR_DOWNSTREAM", ex.getResponseCode());
            assertEquals("50139", ex.getResponseText());
            assertEquals("Status Code: 502 - The Authentication Response is invalid, indicates an error occurred or no response was returned. The request should be considered as not authenticated.", ex.getMessage());
        } finally {
            assertTrue(exceptionCaught);
        }
    }

    @Test
    public void CardHolderEnrolled_ChallengeRequired_v2_Initiate_PostResultFailed() throws ApiException, InterruptedException {
        ThreeDSecure secureEcom =
                Secure3dService
                        .checkEnrollment(card)
                        .withCurrency(currency)
                        .withAmount(amount)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(secureEcom, AVAILABLE, Secure3dVersion.TWO);

        ThreeDSecure initAuth =
                Secure3dService
                        .initiateAuthentication(card, secureEcom)
                        .withAmount(amount)
                        .withCurrency(currency)
                        .withAuthenticationSource(AuthenticationSource.Browser)
                        .withMethodUrlCompletion(MethodUrlCompletion.Yes)
                        .withOrderCreateDate(DateTime.now())
                        .withAddress(shippingAddress, AddressType.Shipping)
                        .withBrowserData(browserData)
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertThreeDSResponse(initAuth, CHALLENGE_REQUIRED, Secure3dVersion.TWO);

        GpApi3DSecureTests.GpApi3DSecureAcsClient acsClient = new GpApi3DSecureTests.GpApi3DSecureAcsClient(initAuth.getIssuerAcsUrl());
        String authResponse = acsClient.authenticate_v2(initAuth);
        assertEquals("{\"success\":true}", authResponse);

        // Get authentication data
        secureEcom =
                Secure3dService
                        .getAuthenticationData()
                        .withServerTransactionId(initAuth.getServerTransactionId())
                        .execute(Secure3dVersion.TWO, GP_API_CONFIG_NAME);

        assertNotNull(secureEcom);
    }

    public void assertThreeDSResponse(ThreeDSecure secureEcom, String status, Secure3dVersion secure3dVersion) {
        assertNotNull(secureEcom);
        assertEquals(ENROLLED, secureEcom.getEnrolledStatus());
        assertEquals(secure3dVersion, secureEcom.getVersion());
        assertEquals(status, secureEcom.getStatus());
        assertNotNull(secureEcom.getIssuerAcsUrl());
        assertNotNull(secureEcom.getPayerAuthenticationRequest());
        assertNotNull(secureEcom.getChallengeReturnUrl());
        assertNotNull(secureEcom.getMessageType());
        assertNotNull(secureEcom.getSessionDataFieldName());
        assertNull(secureEcom.getEci());
    }

}