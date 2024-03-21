package opc.junit.openbanking.consents;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.openbanking.BaseSetup;
import opc.services.openbanking.AccountInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetConsentsTests extends BaseSetup {

  private static Map<String, String> headers;
  private static List<String> listOfConsents;

  @BeforeAll
  public static void setupConsent() throws Exception {
    headers = OpenBankingHelper.generateHeaders(clientKeyId);
    listOfConsents = OpenBankingAccountInformationHelper.createConsents(sharedKey, clientKeyId, headers, 10);
    Collections.reverse(listOfConsents);
  }

  @BeforeEach
  public void setupHeaders() throws Exception {
    headers = OpenBankingHelper.generateHeaders(clientKeyId);
  }

  @Test
  public void GetConsents_Success() {
    final JsonPath response =
        AccountInformationService.getConsents(sharedKey, headers, Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("consents[0].id",equalTo(""))
            .extract().jsonPath();
    Assertions.assertEquals(response.getString("consents[0].createdTimestamp"), response.getString("consents[0].expiary"));


    //Add response assertions
  }

  @Test
  public void GetConsents_Filters_Limit() {
    final Integer limitFilter = listOfConsents.size() / 2;
    final Map<String, Object> filters = new HashMap<>();
    filters.put("limit", limitFilter);

    final ValidatableResponse response =
        AccountInformationService.getConsents(sharedKey, headers, Optional.of(filters))
            .then()
            .statusCode(SC_OK);

    //Add response assertions
  }

  @Test
  public void GetConsents_Filters_Offset() {
    final Integer offsetFilter = listOfConsents.size() / 2;
    final Map<String, Object> filters = new HashMap<>();
    filters.put("offset", offsetFilter);

    final ValidatableResponse response =
        AccountInformationService.getConsents(sharedKey, headers, Optional.of(filters))
            .then()
            .statusCode(SC_OK);

    //Add response assertions
  }

  //Add limit tests
  //Add offset tests
  //Add invalid headers tests
}
