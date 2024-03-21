package opc.junit.multi.multipleapps;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class OutgoingWireTransfersTests extends BaseApplicationsSetup {

    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateProfileId;
    private static String consumerProfileId;
    private static String corporateManagedAccountsProfileId;
    private static String consumerManagedAccountsProfileId;
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String outgoingWireTransfersProfileId;
    private static String secretKey;

    @BeforeAll
    public static void TestSetup() {
        corporateProfileId = applicationFour.getCorporatesProfileId();
        consumerProfileId = applicationFour.getConsumersProfileId();
        corporateManagedAccountsProfileId = applicationFour.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationFour.getConsumerPayneticsEeaManagedAccountsProfileId();
        outgoingWireTransfersProfileId = applicationFour.getOwtProfileId();
        secretKey = applicationFour.getSecretKey();

        corporateSetup();
        consumerSetup();
    }

    @Test
    public void SendOwt_OtherApplicationProfile_Forbidden() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken, secretKey);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(applicationTwo.getOwtProfileId(),
                        managedAccount.getLeft(),
                        corporateCurrency, 200L, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendOwt_OtherApplicationManagedAccount_Forbidden() {

        final CreateConsumerModel otherApplicationConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId())
                        .setBaseCurrency(Currency.EUR.name()).build();

        final Pair<String, String> otherApplicationConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(otherApplicationConsumerModel, applicationTwo.getSecretKey());

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(applicationTwo.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        otherApplicationConsumerModel.getBaseCurrency(), otherApplicationConsumer.getRight(),
                        applicationTwo.getSecretKey());

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrency, 200L, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendOwt_OtherApplicationSecretKey_Forbidden() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken, secretKey);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrency, 200L, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, applicationTwo.getSecretKey(),
                consumerAuthenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendOwt_OtherApplicationIdentity_Forbidden() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken, secretKey);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        consumerCurrency, 200L, OwtType.SEPA).build();

        final Pair<String, String> otherApplicationConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(applicationTwo.getConsumersProfileId(), applicationTwo.getSecretKey());

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey,
                otherApplicationConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendOwt_StartOtherApplicationVerification_NotFound() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(), corporateCurrency, 1000L,
                secretKey,
                corporateAuthenticationToken);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrency, 200L, OwtType.SEPA).build();

        final String id =
                OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then().statusCode(SC_OK).extract().jsonPath().getString("id");

        final Pair<String, String> otherApplicationCorporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey());

        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, EnrolmentChannel.SMS.name(), applicationTwo.getSecretKey(),
                otherApplicationCorporate.getRight())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendOwt_VerifyOtherApplicationVerification_NotFound() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(), corporateCurrency, 1000L,
                secretKey,
                corporateAuthenticationToken);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrency, 200L, OwtType.SEPA).build();

        final String id =
                OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then().statusCode(SC_OK).extract().jsonPath().getString("id");

        AuthenticationFactorsHelper.enrolAndVerifyOtp("123456", EnrolmentChannel.SMS.name(), secretKey, corporateAuthenticationToken);
        OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, EnrolmentChannel.SMS.name(), secretKey,
                corporateAuthenticationToken)
                .then().statusCode(SC_NO_CONTENT);

        final Pair<String, String> otherApplicationCorporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey());

        OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel("123456"), id,
                EnrolmentChannel.SMS.name(), applicationTwo.getSecretKey(), otherApplicationCorporate.getRight())
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetOwt_GetOtherApplicationOwt_NotFound() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(), corporateCurrency, 1000L,
                secretKey,
                corporateAuthenticationToken);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrency, 200L, OwtType.SEPA).build();

        final String id =
                OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                        .then().statusCode(SC_OK).extract().jsonPath().getString("id");

        final Pair<String, String> otherApplicationCorporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey());
        OutgoingWireTransfersService.getOutgoingWireTransfer(applicationTwo.getSecretKey(), id, otherApplicationCorporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetOwts_GetOtherApplicationOwts_NoEntries() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken, secretKey);

        simulateManagedAccountDepositAndCheckBalance(managedAccount.getLeft(), corporateCurrency, 1000L,
                secretKey,
                corporateAuthenticationToken);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        corporateCurrency, 200L, OwtType.SEPA).build();

        OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationToken, Optional.empty())
                .then().statusCode(SC_OK);

        final Pair<String, String> otherApplicationCorporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(applicationTwo.getCorporatesProfileId(), applicationTwo.getSecretKey());
        OutgoingWireTransfersService.getOutgoingWireTransfers(applicationTwo.getSecretKey(), Optional.empty(), otherApplicationCorporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    private static void corporateSetup() {
        corporateCurrency = Currency.EUR.name();
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(corporateCurrency).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
    }

    private static void consumerSetup() {
        consumerCurrency = Currency.EUR.name();
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(consumerCurrency).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }
}
