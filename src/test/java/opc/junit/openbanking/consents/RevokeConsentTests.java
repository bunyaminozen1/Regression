package opc.junit.openbanking.consents;

import io.restassured.response.ValidatableResponse;
import opc.enums.openbanking.OpenBankingConsentState;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.openbanking.BaseSetup;
import opc.services.openbanking.AccountInformationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;


public class RevokeConsentTests extends BaseSetup {

  private static Map<String, String> headers;
  private static String consentId;

  @BeforeAll
  public static void setupConsent() throws Exception {
    headers = OpenBankingHelper.generateHeaders(clientKeyId);
    consentId = OpenBankingAccountInformationHelper.createConsent(sharedKey, headers);
  }

  @BeforeEach
  public void setupHeaders() throws Exception {
    headers = OpenBankingHelper.generateHeaders(clientKeyId);
  }

  @Test
  public void RevokeConsent_Success() {
    final ValidatableResponse response =
        AccountInformationService.revokeConsent(sharedKey, headers, consentId)
            .then()
            .statusCode(SC_OK);

    response.body("createdTimestamp", notNullValue())
        .body("expiry", notNullValue())
        .body("id", notNullValue())
        .body("lastUpdated", notNullValue())
        .body("state",equalTo(OpenBankingConsentState.REVOKED.getName()))
        .body("tppId",equalTo(tppId))
        .body("tppName",equalTo("Test"));
  }

  @Test
  public void RevokeConsent_EmptyConsentId_BedRequest() {
    AccountInformationService.getConsent(sharedKey, headers, "")
        .then()
        .statusCode(SC_BAD_REQUEST);

    //Add response assertions
  }

  @Test
  public void RevokeConsent_InvalidConsentId_BedRequest() {
    AccountInformationService.getConsent(sharedKey, headers, OpenBankingHelper.generateRandomUUID())
        .then()
        .statusCode(SC_BAD_REQUEST);

    //Add response assertions
  }
}
