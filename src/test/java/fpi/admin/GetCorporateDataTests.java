package fpi.admin;

import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

public class GetCorporateDataTests extends BaseAdminSetup {
    private static String buyerId;
    private static CreateBuyerModel createBuyerModel;

    @BeforeAll
    public static void Setup() throws MalformedURLException, URISyntaxException {
        createBuyer();
    }

    @Test
    public void GetCorporateData_ValidBuyerId_Success() {
        AdminService.getCorporateData(buyerId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("supportedCurrencies", equalTo(createBuyerModel.getSupportedCurrencies()));
    }

    @Test
    public void GetCorporateData_InvalidBuyerId_CorporateNotFound() {
        AdminService.getCorporateData(RandomStringUtils.randomAlphabetic(18), adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CORPORATE_NOT_FOUND"));
    }

    @Test
    public void GetCorporateData_EmptyBuyerId_NotFound() {
        AdminService.getCorporateData("", adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetCorporateData_InvalidToken_Unauthorised() {
        AdminService.getCorporateData(buyerId, RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporateData_EmptyToken_Unauthorised() {
        AdminService.getCorporateData(buyerId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporateData_NullToken_Unauthorised() {
        AdminService.getCorporateData(buyerId, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    public static void createBuyer() {
        createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
    }
}
