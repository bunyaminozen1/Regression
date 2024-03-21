package opc.junit.secure.openbanking.accountinformation;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.OwnerType;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.secure.openbanking.BaseSetup;
import opc.services.openbanking.OpenBankingSecureService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class RejectConsentTests extends BaseSetup {

    private Map<String, String> headers;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {
        corporateSetup();
        consumerSetup();
    }

    @BeforeEach
    public void Setup() throws Exception {
        headers = OpenBankingHelper.generateHeaders(clientKeyId);
    }

    @Test
    public void AuthoriseConsent_Corporate_Success() {

        final String consentId =
                OpenBankingAccountInformationHelper.createConsent(sharedKey, headers);

        final ValidatableResponse response =
                OpenBankingSecureService.rejectConsent(sharedKey, corporateAuthenticationToken, tppId, consentId)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, consentId, corporateId, OwnerType.CORPORATE);
    }

    @Test
    public void AuthoriseConsent_Consumer_Success() {

        final String consentId =
                OpenBankingAccountInformationHelper.createConsent(sharedKey, headers);

        final ValidatableResponse response =
                OpenBankingSecureService.rejectConsent(sharedKey, consumerAuthenticationToken, tppId, consentId)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, consentId, consumerId, OwnerType.CONSUMER);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final String consentId,
                                          final String identityId,
                                          final OwnerType ownerType){

        response.body("createdTimestamp", notNullValue())
                .body("expiry", notNullValue())
                .body("id", equalTo(consentId))
                .body("lastUpdated", notNullValue())
                .body("identity.id", equalTo(identityId))
                .body("identity.type", equalTo(ownerType.getValue()))
                .body("links.redirect", equalTo(String.format("https://sample.tpp/weavr?consentId=%s&consentState=REJECTED", consentId)))
                .body("state",equalTo("REJECTED"))
                .body("tppId",equalTo(tppId))
                .body("tppName",equalTo("Test"));
    }
}