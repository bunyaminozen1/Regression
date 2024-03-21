package opc.junit.openbanking.consents;

import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.Optional;
import opc.enums.openbanking.OpenBankingConsentState;
import opc.enums.openbanking.SignatureHeader;
import opc.enums.openbanking.TppSignatureComponent;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.openbanking.BaseSetup;
import opc.services.openbanking.AccountInformationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CreateConsentTests extends BaseSetup {

    private Map<String, String> headers;

    @BeforeEach
    public void Setup() throws Exception {
        headers = OpenBankingHelper.generateHeaders(clientKeyId);
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_Success() {

        final ValidatableResponse response =
                AccountInformationService.createConsent(sharedKey, headers)
                        .then()
                        .statusCode(SC_OK);

        response.body("createdTimestamp", notNullValue())
                .body("expiry", notNullValue())
                .body("id", notNullValue())
                .body("lastUpdated", notNullValue())
                .body("links.redirect", equalTo(String.format("https://openbanking.weavr.io/consent?programmeKey=%s&scope=ACCOUNT_INFORMATION&consentId=%s&tppId=%s",
                        URLEncoder.encode(sharedKey, StandardCharsets.UTF_8), response.extract().jsonPath().getString("id"), tppId)))
                .body("state",equalTo(OpenBankingConsentState.AWAITING_AUTHORISATION.getName()))
                .body("tppId",equalTo(tppId))
                .body("tppName",equalTo("Test"));
    }


    @Test
    public void CreateConsent_AwaitingAuthorisation_BadRequest() {
        AccountInformationService.createConsent("", headers)
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_Unauthorised() {
        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DATE);
        signatureHeaders.add(SignatureHeader.DIGEST);
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, "");

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_ExpiredDateHeader() {

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DATE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, OpenBankingHelper.getDate(
            Optional.of(60)));

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Request date not within bounds"));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_EmptyDateHeader() {
        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DATE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, "");

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Missing request headers: [date]"));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_EmptyDigestHeader() {

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DIGEST);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, "");

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Missing request headers: [digest]"));

    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_EmptyTppSignatureHeader() {

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, "");

        AccountInformationService.createConsent(sharedKey, headers)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_EmptyDateAndDigestHeaders(){

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DATE);
        signatureHeaders.add(SignatureHeader.DIGEST);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, "");

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Missing request headers: [date, digest]"));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_DateWithinBounds_SignatureNotMatch() {

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.DATE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, OpenBankingHelper.getDate(Optional.of(5)));

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo(String.format("Signature does not match for keyId %s",clientKeyId)));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_TppSignature_EmptyKeyId() {
        final String tppSignatureWithEmptyKeyId = OpenBankingHelper.generateInvalidSignatureComponent(headers, TppSignatureComponent.KEY_ID, "");

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, tppSignatureWithEmptyKeyId);

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Invalid signature header format"));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidKeyId() {
        final String invalidKeyId = OpenBankingHelper.generateRandomUUID();
        final String tppSignatureWithInvalidKeyId = OpenBankingHelper.generateInvalidSignatureComponent(headers, TppSignatureComponent.KEY_ID, invalidKeyId);

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, tppSignatureWithInvalidKeyId);

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo( String.format("Invalid keyId %s", invalidKeyId)));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_TppSignature_EmptyAlgorithm() {
        final String tppSignatureWithEmptyAlgorithm = OpenBankingHelper.generateInvalidSignatureComponent(headers, TppSignatureComponent.ALGORITHM, "");

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, tppSignatureWithEmptyAlgorithm);

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Invalid signature header format"));
    }

    @Test
    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidAlgorithm() {
        final String tppSignatureWithInvalidAlgorithm = OpenBankingHelper.generateInvalidSignatureComponent(headers, TppSignatureComponent.ALGORITHM, "Shaaa");

        final ArrayList<SignatureHeader> signatureHeaders = new ArrayList<>();
        signatureHeaders.add(SignatureHeader.TPP_SIGNATURE);

        OpenBankingHelper.replaceHeaders(headers, signatureHeaders, tppSignatureWithInvalidAlgorithm);

        AccountInformationService.createConsent(sharedKey, headers)
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .body("message", equalTo("Invalid algorithm"));
    }

//    @Test
//    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidHeaders() {
//
//    }

//    @Test
//    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidHeaders_MissingDate() {
//
//    }

//    @Test
//    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidHeaders_MissingDigest() {
//
//    }
//
//    @Test
//    public void CreateConsent_AwaitingAuthorisation_TppSignature_InvalidSignature() {
//
//    }
}
