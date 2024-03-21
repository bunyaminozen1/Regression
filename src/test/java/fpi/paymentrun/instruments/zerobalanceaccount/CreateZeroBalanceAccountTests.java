package fpi.paymentrun.instruments.zerobalanceaccount;

import commons.enums.Roles;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.ManagedAccountsService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Tag(PluginsTags.PAYMENT_RUN_ZBA)
public class CreateZeroBalanceAccountTests extends BasePaymentRunSetup {
    private String buyerId;
    private String buyerToken;
    private String buyerEmail;
    private String buyerPassword;
    private final static String VERIFICATION_CODE = "123456";
    private String buyerCurrency;

    @BeforeEach
    public void Setup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
        buyerEmail = createBuyerModel.getAdminUser().getEmail();
        buyerPassword = TestHelper.getDefaultPassword(secretKey);
        buyerCurrency = createBuyerModel.getBaseCurrency();
    }

    @Test
    public void CreateZBA_ZBACreated_Success(){
        //Do full verification of buyer
        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());

        BuyersHelper.verifyKyb(secretKey, buyerId);
        verifyEnrolmentDeviceByOtp(buyerToken);

        AuthenticationService.loginWithPassword(new LoginModel(buyerEmail, new PasswordModel(buyerPassword)) , secretKey)
                .then()
                .statusCode(SC_OK);

        verifySuccessfulStepup(buyerToken);

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("accounts[0].currency", equalTo(buyerCurrency))
                .body("accounts[0].id", notNullValue())
                .body("accounts[0].state.blockedReason", equalTo("SYSTEM"))
                .body("accounts[0].state.state", equalTo("BLOCKED"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.id", equalTo(buyerId))
                .body("adminUser.roles[0]", equalTo(Roles.ADMIN.name()));
    }

    @Test
    public void CreateZBA_KybNotApproved_NoZBACreatedSuccess(){
        //Do full verification of buyer, but do not approve a KYB
        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());

        verifyEnrolmentDeviceByOtp(buyerToken);

        AuthenticationService.loginWithPassword(new LoginModel(buyerEmail, new PasswordModel(buyerPassword)), secretKey)
                .then()
                .statusCode(SC_OK);
        verifySuccessfulStepup(buyerToken);

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.id", equalTo(buyerId))
                .body("adminUser.roles[0]", equalTo("ADMIN"));
    }

    @Test
    public void CreateZBA_StepupFailed_NoZBACreatedSuccess(){
        //Do full verification of buyer, but do not verify the step up
        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());

        BuyersHelper.verifyKyb(secretKey, buyerId);
        verifyEnrolmentDeviceByOtp(buyerToken);

        AuthenticationService.loginWithPassword(new LoginModel(buyerEmail, new PasswordModel(buyerPassword)), secretKey)
                .then()
                .statusCode(SC_OK);

        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.getManagedAccounts(secretKey, Optional.empty(), buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        BuyersService.getBuyer(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.id", equalTo(buyerId))
                .body("adminUser.roles[0]", equalTo("ADMIN"));
    }

    private void verifyEnrolmentDeviceByOtp(final String token) {
        AuthenticationHelper.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, token);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, buyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private void verifySuccessfulStepup(final String token) {
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
