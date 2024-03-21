package fpi.paymentrun.paymentruns;

import com.fasterxml.jackson.core.JsonProcessingException;
import commons.enums.Currency;
import commons.enums.State;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.helpers.InstrumentsHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.models.PaymentAmountModel;
import fpi.paymentrun.models.PaymentsModel;
import fpi.paymentrun.models.PaymentsSupplierModel;
import fpi.paymentrun.services.PaymentRunsService;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.beneficiaries.BankAccountDetailsModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN)
public class CreatePaymentRunTests extends BasePaymentRunSetup {

    protected static final String VERIFICATION_CODE = "123456";

    private String buyerId;
    private String buyerToken;

    private static Pair<String, String> accountNumberAndSortCode;

    /**
     * Required user role: CREATOR
     */

    @BeforeEach
    public void SourceSetup() {
        creatorBuyerSetup();
    }

    @BeforeAll
    public static void TestSetup() {
        accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
    }

    @Test
    public void CreatePaymentRun_ValidRoleBuyerFasterPaymentsDetails_Success() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                        .then()
                        .statusCode(SC_CREATED);
        assertSuccessfulFasterPaymentsResponse(createPaymentRunModel, response, buyerId);
    }

    @Test
    public void CreatePaymentRun_ValidRoleAuthUser_Success() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = getCreatorRoleAuthUser(secretKey, buyerToken);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, authUser.getRight())
                        .then()
                        .statusCode(SC_CREATED);
        assertSuccessfulFasterPaymentsResponse(createPaymentRunModel, response, authUser.getLeft());
    }

    @Test
    public void CreatePaymentRun_MultipleRolesAuthUser_Success() {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = getMultipleRolesAuthUser(secretKey, buyerToken);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, authUser.getRight())
                        .then()
                        .statusCode(SC_CREATED);
        assertSuccessfulFasterPaymentsResponse(createPaymentRunModel, response, authUser.getLeft());
    }

    @Test
    public void CreatePaymentRun_AdminRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String adminBuyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, adminBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePaymentRun_IncorrectRoleBuyer_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(createBuyerModel, secretKey).getRight();
        BuyersHelper.assignControllerRole(secretKey, buyerToken);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePaymentRun_IncorrectRoleAuthUser_Forbidden() {
        final String authUserToken = getControllerRoleAuthUserToken(secretKey, buyerToken);

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, authUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePaymentRun_TagNotRequired_Success() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .tag(null)
                        .build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                        .then()
                        .statusCode(SC_CREATED);
        assertSuccessfulFasterPaymentsResponse(createPaymentRunModel, response, buyerId);
    }

    @Test
    public void CreatePaymentRun_RequiredOnlyPaymentDetails_Success() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.builder()
                                .paymentAmount(PaymentAmountModel.builder()
                                        .currency(Currency.GBP.name())
                                        .amount(100)
                                        .build())
                                .reference(String.format("Ref%s", RandomStringUtils.randomAlphabetic(5)))
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(5))
                                        .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                        .then()
                        .statusCode(SC_CREATED);

        assertSuccessfulFasterPaymentsResponse(createPaymentRunModel, response, buyerId);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"Name_test", "Nameżółć"})
    public void CreatePaymentRun_InvalidSupplierName_BadRequest(final String supplierName) {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.defaultFasterPaymentsBankAccountSupplerModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                        .name(supplierName)
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, mode = EnumSource.Mode.EXCLUDE, names = {"GBP"})
    public void CreatePaymentRun_NotAllowedCurrencyChecks_BadRequest(final Currency currency) {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .paymentAmount(PaymentAmountModel.builder()
                                        .currency(currency.name())
                                        .amount(100)
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "paymentAmount")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("currency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"ABCD", "", "ABC"})
    public void CreatePaymentRun_InvalidCurrency_BadRequest(final String currency) {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .paymentAmount(PaymentAmountModel.builder()
                                        .currency(currency)
                                        .amount(100)
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "paymentAmount")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("currency"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"0", "-1"})
    public void CreatePaymentRun_InvalidAmount_BadRequest(final int amount) {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .paymentAmount(PaymentAmountModel.builder()
                                        .currency("GBP")
                                        .amount(amount)
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "paymentAmount")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("amount"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_LEAST"));
    }

    @Test
    public void CreatePaymentRun_NullReference_BadRequest() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .reference(null)
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("reference"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreatePaymentRun_EmptyReference_BadRequest() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .reference("")
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("reference"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("payments", "0")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("reference"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test
    public void CreatePaymentRun_NullSupplierName_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(null)
                                        .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreatePaymentRun_EmptySupplierName_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name("")
                                        .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_InvalidBankCountry_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(7))
                                        .bankCountry(RandomStringUtils.randomAlphabetic(3))
                                        .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("bankCountry"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("bankCountry"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"))
                .body("syntaxErrors.invalidFields.size()", equalTo(2));
    }

    @Test
    public void CreatePaymentRun_InvalidBankAddressAndAddressTooLong_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(7))
                                        .bankCountry("MT")
                                        .bankAddress(RandomStringUtils.randomAlphabetic(151))
                                        .address(RandomStringUtils.randomAlphabetic(151))
                                        .bankAccountDetails(BankAccountDetailsModel.FasterPaymentsBankAccountDetails(
                                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("address"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("payments", "0", "supplier")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("bankAddress"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"))
                .body("syntaxErrors.invalidFields.size()", equalTo(2));
    }

    @Test
    public void CreatePaymentRun_NullSupplierBankDetails_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(5))
                                        .bankAccountDetails(null)
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments","0","supplier","bankAccountDetails","payments","0","supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("bankAccountDetails"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_InvalidFasterPaymentsDetails_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(5))
                                        .bankAccountDetails(BankAccountDetailsModel.builder()
                                                .setAccountNumber(RandomStringUtils.randomNumeric(10))
                                                .setSortCode(RandomStringUtils.randomNumeric(8))
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments","0","supplier","bankAccountDetails","payments","0","supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("bankAccountDetails"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_FasterPaymentsNoAccountNumber_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(5))
                                        .bankAccountDetails(BankAccountDetailsModel.builder()
                                                .setAccountNumber(null)
                                                .setSortCode(accountNumberAndSortCode.getRight())
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments","0","supplier","bankAccountDetails","payments","0","supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("bankAccountDetails"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_FasterPaymentsNoSortCode_BadRequest() {
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(PaymentsSupplierModel.builder()
                                        .name(RandomStringUtils.randomAlphabetic(5))
                                        .bankAccountDetails(BankAccountDetailsModel.builder()
                                                .setAccountNumber(accountNumberAndSortCode.getLeft())
                                                .setSortCode(null)
                                                .build())
                                        .build())
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments","0","supplier","bankAccountDetails","payments","0","supplier")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("bankAccountDetails"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_NullSupplier_BadRequest() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(List.of(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(
                                        accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                                .supplier(null)
                                .build()))
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("payments","0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("supplier"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_NullPaymentsDetails_BadRequest() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.builder()
                        .payments(null)
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payments"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", ""})
    public void CreatePaymentRun_InvalidApiKey_Unauthorised(final String apiKey) {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, apiKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePaymentRun_NoApiKey_BadRequest() throws JsonProcessingException {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRunNoApiKey(createPaymentRunModel, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"))
                .body("syntaxErrors.invalidFields.size()", equalTo(1));
    }

    @Test
    public void CreatePaymentRun_InvalidToken_Unauthorised() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreatePaymentRun_NoToken_Unauthorised(final String token) {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePaymentRun_DifferentInnovatorApiKey_Unauthorised() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePaymentRun_BuyerLoggedOut_Unauthorised() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledVerifiedBuyer(secretKey);

        AuthenticationHelper.logout(secretKey, buyer.getRight());

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePaymentRun_BackofficeImpersonator_Unauthorised() {

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(
                                accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey,
                        getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    //    tests for payments limits break regression with 503 error. this should be fixed: https://weavr-payments.atlassian.net/browse/FPI-453
    @Disabled
    @Test
    public void CreatePaymentRun_PaymentsMoreThanLimit_BadRequest() {
        List<PaymentsModel> payments = new ArrayList<>();
//Limit: 1000 payments in request body
        for (int i = 0; i < 1001; i++) {
            payments.add(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build());
        }

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .payments(payments)
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("payments"));
    }

    @Disabled
    @Test
    public void CreatePaymentRun_MaxPaymentsLimit_Success() {
        List<PaymentsModel> payments = new ArrayList<>();
//Limit: 1000 payments in request body
        for (int i = 0; i < 1000; i++) {
            payments.add(PaymentsModel.defaultFasterPaymentsBankAccountPaymentsModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build());
        }

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                        .payments(payments)
                        .build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CREATED);
    }

    //    SEPA is in postMVP plans
//    TODO create tests to validate SEPA bank account details (iban, bankIdentifierCode)
    @Disabled
    @Test
    public void CreatePaymentRun_SEPADetails_Success() {
        final Pair<String, String> ibanAndBankIdentifierCodes = ModelHelper.generateRandomValidSEPABankDetails();

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunSEPABankAccountModel(ibanAndBankIdentifierCodes.getLeft(), ibanAndBankIdentifierCodes.getRight()).build();

        final ValidatableResponse response =
                PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                        .then()
                        .statusCode(SC_CREATED);
        assertSuccessfulSEPAResponse(createPaymentRunModel, response);
    }

    @Test
    public void CreatePaymentRun_BankDetailsFromZbaAccount_Conflict() {
        final String accountId = ManagedAccountsHelper.getManagedAccounts(secretKey, buyerToken)
                .jsonPath().get("accounts[0].id");

        ManagedAccountsHelper.ensureIbanState(secretKey, accountId, buyerToken, "ALLOCATED");

        final HashMap<String, String> accountDetails = AdminService.getManagedAccount(accountId, adminToken)
                .then()
                .extract()
                .jsonPath()
                .get("bankAccountDetails[0]");

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountDetails.get("accountNumber"), accountDetails.get("sortCode")).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BANK_ACCOUNT_CANNOT_BE_ZERO_BALANCE_ACCOUNT"));
    }

    @Test
    public void CreatePaymentRun_BankDetailsFromLinkedAccount_Conflict() throws MalformedURLException, URISyntaxException {
        final String buyerToken = BuyersHelper.createEnrolledVerifiedBuyer(secretKey).getRight();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        final String linkedAccountId = InstrumentsHelper.createLinkedAccountGetId(buyerToken, secretKey, sharedKey);
        final HashMap<String, String> accountDetails = InstrumentsHelper.getLinkedAccountById(linkedAccountId, secretKey, buyerToken)
                .extract()
                .jsonPath()
                .get("accountIdentification");

        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountDetails.get("accountNumber"), accountDetails.get("sortCode")).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BANK_ACCOUNT_CANNOT_BE_LINKED_ACCOUNT"));
    }

    private void assertSuccessfulFasterPaymentsResponse(final CreatePaymentRunModel createPaymentRunModel,
                                                        final ValidatableResponse response,
                                                        final String userId) {
        response.body("id", notNullValue())
                .body("status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("payments[0].id", notNullValue())
                .body("payments[0].status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("payments[0].paymentAmount.currency", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("payments[0].paymentAmount.amount", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("payments[0].paymentRef", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentRef()))
                .body("payments[0].supplier.name", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getName()))
                .body("payments[0].supplier.address", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getAddress()))
                .body("payments[0].supplier.bankName", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getBankName()))
                .body("payments[0].supplier.bankAddress", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getBankAddress()))
                .body("payments[0].supplier.bankCountry", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getBankCountry()))
                .body("payments[0].supplier.bankAccountDetails.accountNumber", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getBankAccountDetails().getAccountNumber()))
                .body("payments[0].supplier.bankAccountDetails.sortCode", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getBankAccountDetails().getSortCode()))
                .body("payments[0].reference", equalTo(createPaymentRunModel.getPayments().get(0).getReference()))
                .body("payments[0].externalRef", equalTo(createPaymentRunModel.getPayments().get(0).getExternalRef()))
                .body("payments[0].paymentRef", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentRef()))
                .body("createdBy", equalTo(userId))
                .body("createdAt", notNullValue())
                .body("paymentRunRef", equalTo(createPaymentRunModel.getPaymentRunRef()))
                .body("description", equalTo(createPaymentRunModel.getDescription()))
                .body("tag", equalTo(createPaymentRunModel.getTag()));
    }

    private void assertSuccessfulSEPAResponse(final CreatePaymentRunModel createPaymentRunModel,
                                              final ValidatableResponse response) {
        response.body("id", notNullValue())
                .body("status", equalTo(State.PENDING_CONFIRMATION.name()))
                .body("createdBy", equalTo(buyerId))
                .body("payments[0].id", notNullValue())
                .body("payments[0].status", equalTo(State.PENDING_CHALLENGE.name()))
                .body("payments[0].paymentAmount.currency", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getCurrency()))
                .body("payments[0].paymentAmount.amount", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentAmount().getAmount()))
                .body("payments[0].paymentRef", equalTo(createPaymentRunModel.getPayments().get(0).getPaymentRef()))
//                .body("payments[0].supplier.supplierId", equalTo(createPaymentRunModel.getPayments().get(0).getSupplier().getSupplierId()))
                .body("payments[0].reference", equalTo(createPaymentRunModel.getPayments().get(0).getReference()))
                .body("payments[0].externalRef", equalTo(createPaymentRunModel.getPayments().get(0).getExternalRef()))
                .body("createdAt", notNullValue())
                .body("paymentRunRef", equalTo(createPaymentRunModel.getPaymentRunRef()))
                .body("description", equalTo(createPaymentRunModel.getDescription()))
                .body("tag", equalTo(createPaymentRunModel.getTag()));
    }

    private void creatorBuyerSetup() {
        final Pair<String, String> authenticatedBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        buyerId = authenticatedBuyer.getLeft();
        buyerToken = authenticatedBuyer.getRight();
        BuyersHelper.assignCreatorRole(secretKey, buyerToken);
    }
}