package opc.junit.multi.directdebits;

import opc.enums.opc.CompanyType;
import commons.enums.Currency;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.DirectDebitsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class CancelMandateTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount;
    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;

    @BeforeAll
    public static void Setup(){

        corporateSetup();
        consumerSetup();

        corporateManagedAccount = createFundedManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount = createFundedManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
    }

    @Test
    public void CancelMandate_Corporate_Success() {

        final String mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken).getKey();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CancelMandate_Consumer_Success() {

        final String mandate = createMandate(consumerManagedAccount, consumerAuthenticationToken).getKey();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CancelMandate_WithCollection_Success() {

        final Map.Entry<Pair<String, SimulateCreateMandateModel>, List<Pair<String, String>>> mandate =
                createMandateWithCollection(corporateManagedAccount, corporateCurrency, 101L, corporateAuthenticationToken)
                        .entrySet().stream().findFirst().orElseThrow();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate.getKey().getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CancelMandate_AlreadyCancelledInternally_MandateNotActive() {

        final Pair<String, SimulateCreateMandateModel> mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken);
        DirectDebitsHelper.cancelMandateInternally(mandate.getLeft(), secretKey, corporateAuthenticationToken);

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANDATE_NOT_ACTIVE"));
    }

    @Test
    public void CancelMandate_AlreadyCancelledExternally_MandateNotActive() {

        final Pair<String, SimulateCreateMandateModel> mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken);
        DirectDebitsHelper.cancelMandateExternally(mandate.getLeft(), mandate.getRight().getDdiId(), secretKey, corporateAuthenticationToken);

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANDATE_NOT_ACTIVE"));
    }

    @Test
    public void CancelMandate_Expired_MandateNotActive() {

        final Pair<String, SimulateCreateMandateModel> mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken);
        DirectDebitsHelper.expireMandate(mandate.getLeft(), mandate.getRight().getDdiId(), secretKey, corporateAuthenticationToken);

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MANDATE_NOT_ACTIVE"));
    }

    @Test
    public void CancelMandate_UnknownMandate_NotFound() {

        createMandate(corporateManagedAccount, corporateAuthenticationToken);

        DirectDebitsService.cancelDirectDebitMandate(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CancelMandate_InvalidMandate_BadRequest(){

        DirectDebitsService.cancelDirectDebitMandate(secretKey, RandomStringUtils.randomAlphanumeric(6), corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CancelMandate_InvalidApiKey_Unauthorised(){
        DirectDebitsService.cancelDirectDebitMandate("abc", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CancelMandate_NoApiKey_BadRequest(){
        DirectDebitsService.cancelDirectDebitMandate("", RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CancelMandate_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken).getKey();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate, corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CancelMandate_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CancelMandate_CrossIdentityChecks_NotFound() {

        final String mandate = createMandate(corporateManagedAccount, corporateAuthenticationToken).getKey();

        DirectDebitsService.cancelDirectDebitMandate(secretKey, mandate, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.GBP.name()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
