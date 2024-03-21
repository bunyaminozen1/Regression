package opc.junit.smoke;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.FeeType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.SpendLimitModel;
import opc.models.multi.managedcards.SpendLimitResponseModel;
import opc.models.multi.managedcards.SpendRulesModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import commons.models.MobileNumberModel;
import opc.models.shared.VerificationModel;
import opc.models.testmodels.BalanceModel;
import opc.models.testmodels.ManagedCardDetails;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.multi.SendsService;
import opc.services.multi.TransfersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateTransactionsTests extends BaseSmokeSetup {
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String existingCorporateCurrency;
    private static String existingConsumerCurrency;
    private static String innovatorId;
    private static String existingConsumerAuthenticationToken;
    private static String existingCorporateAuthenticationToken;
    private static final String OTP_CHANNEL = EnrolmentChannel.SMS.name();
    private static final String PUSH_CHANNEL = EnrolmentChannel.AUTHY.name();
    private static final String VERIFICATION_CODE = "123456";

    @BeforeAll
    public static void Setup() {

        innovatorId = applicationOne.getInnovatorId();

        corporateSetup();
        consumerSetup();

        existingConsumerAuthenticationToken = getExistingConsumerDetails().getLeft();
        existingCorporateAuthenticationToken = getExistingCorporateDetails().getLeft();
        existingCorporateCurrency = getExistingCorporateDetails().getRight();
        existingConsumerCurrency = getExistingConsumerDetails().getRight();
    }

    private static Stream<Arguments> owtSuccessfulArgs() {
        return Stream.of(Arguments.of(Currency.EUR, OwtType.SEPA, FeeType.SEPA_OWT_FEE),
                Arguments.of(Currency.GBP, OwtType.FASTER_PAYMENTS, FeeType.FASTER_PAYMENTS_OWT_FEE));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void TransferFunds_CorporateMaToMa_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Map<String, CreateManagedAccountModel> managedAccounts =
                ManagedAccountsHelper.createManagedAccounts(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken, secretKey, 2);

        AdminHelper.fundManagedAccount(innovatorId, managedAccounts.keySet().toArray()[0].toString(), currency.name(), depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.keySet().toArray()[0].toString(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.keySet().toArray()[1].toString(), MANAGED_ACCOUNTS), currency.name(), sendAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.keySet().toArray()[0].toString(), corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccounts.keySet().toArray()[1].toString(), corporateAuthenticationToken, sendAmount.intValue());
    }

    @Test
    public void TransferFunds_ExistingCorporateUserMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, existingCorporateAuthenticationToken);

        final Map<String, CreateManagedAccountModel> managedAccounts =
                ManagedAccountsHelper.createManagedAccounts(corporateManagedAccountProfileId, existingCorporateCurrency, user.getRight(), secretKey, 2);

        AdminHelper.fundManagedAccount(innovatorId, managedAccounts.keySet().toArray()[0].toString(), existingCorporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccounts.keySet().toArray()[0].toString(), MANAGED_ACCOUNTS),
                Pair.of(managedAccounts.keySet().toArray()[1].toString(), MANAGED_ACCOUNTS), existingCorporateCurrency, transferAmount, user.getRight());

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(existingCorporateCurrency).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccounts.keySet().toArray()[0].toString(), user.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccounts.keySet().toArray()[1].toString(), user.getRight(), transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);

        AdminHelper.fundManagedCard(innovatorId, managedCardId, corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCardId, MANAGED_CARDS),
                Pair.of(managedAccountId, MANAGED_ACCOUNTS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MC_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_ExistingConsumerMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrency, secretKey, consumerAuthenticationToken);
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, secretKey, consumerAuthenticationToken);

        AdminHelper.fundManagedCard(innovatorId, managedCardId, consumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCardId, MANAGED_CARDS),
                Pair.of(managedAccountId, MANAGED_ACCOUNTS), consumerCurrency, transferAmount, consumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(consumerCurrency).get(FeeType.MC_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCardId, consumerAuthenticationToken, sourceBalance);
        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_CorporateMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount
                        (corporateManagedAccountProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);
        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard
                        (corporatePrepaidManagedCardsProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, corporateCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedAccountId, MANAGED_ACCOUNTS),
                Pair.of(managedCardId, MANAGED_CARDS), corporateCurrency, transferAmount, corporateAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrency).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCardId, corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void TransferFunds_ExistingConsumerMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final List<ManagedCardDetails> managedCards =
                ManagedCardsHelper.createPhysicalPrepaidManagedCards(consumerPrepaidManagedCardsProfileId, existingConsumerCurrency, secretKey, existingConsumerAuthenticationToken, 2);

        AdminHelper.fundManagedCard(innovatorId, managedCards.get(0).getManagedCardId(), existingConsumerCurrency, depositAmount);

        assertSuccessfulTransfer(Pair.of(managedCards.get(0).getManagedCardId(), MANAGED_CARDS),
                Pair.of(managedCards.get(1).getManagedCardId(), MANAGED_CARDS), existingConsumerCurrency, transferAmount, existingConsumerAuthenticationToken);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(existingConsumerCurrency).get(FeeType.MC_TO_MC_TRANSFER_FEE).getAmount());

        assertManagedCardBalance(managedCards.get(0).getManagedCardId(), existingConsumerAuthenticationToken, sourceBalance);
        assertManagedCardBalance(managedCards.get(1).getManagedCardId(), existingConsumerAuthenticationToken, transferAmount.intValue());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void SendFunds_CorporateMaToMa_Success(final Currency currency) {
        final String corporateAuthenticationTokenSource = corporateAuthenticationToken;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final String manageAccountSourceId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationTokenSource);
        final String manageAccountDestinationId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationTokenDestination);

        AdminHelper.fundManagedAccount(innovatorId, manageAccountSourceId, currency.name(), depositAmount);

        assertSuccessfulSend(Pair.of(manageAccountSourceId, MANAGED_ACCOUNTS),
                Pair.of(manageAccountDestinationId, MANAGED_ACCOUNTS), currency.name(), sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(manageAccountSourceId, corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(manageAccountDestinationId, corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ToExistingCorporateUserMaToMa_Success() {
        final String corporateAuthenticationTokenSource = corporateAuthenticationToken;
        final String corporateCurrencySource = corporateCurrency;
        final String existingCorporateAuthenticationTokenDestination = existingCorporateAuthenticationToken;

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final UsersModel usersModelSource = UsersModel.DefaultUsersModel().build();
        final UsersModel usersModelDestination = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> userSource = UsersHelper.createAuthenticatedUser(usersModelSource, secretKey, corporateAuthenticationTokenSource);
        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(usersModelDestination, secretKey, existingCorporateAuthenticationTokenDestination);

        final String manageAccountSourceId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, userSource.getRight());
        final String manageAccountDestinationId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, userDestination.getRight());


        AdminHelper.fundManagedAccount(innovatorId, manageAccountSourceId, corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(manageAccountSourceId, MANAGED_ACCOUNTS),
                Pair.of(manageAccountDestinationId, MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, userSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(manageAccountSourceId, userSource.getRight(), sourceBalance);
        assertManagedAccountBalance(manageAccountDestinationId, userDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateMcToMa_Success() {
        final String corporateAuthenticationTokenSource = corporateAuthenticationToken;
        final String corporateCurrencySource = corporateCurrency;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final String managedCardSource =
                ManagedCardsHelper.createPrepaidManagedCard
                        (corporatePrepaidManagedCardsProfileId, corporateCurrencySource, secretKey, corporateAuthenticationTokenSource);
        final String managedAccountDestination =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, corporateAuthenticationTokenDestination);

        AdminHelper.fundManagedCard(innovatorId, managedCardSource, corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedCardSource, MANAGED_CARDS),
                Pair.of(managedAccountDestination, MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MC_TO_MA_SEND_FEE).getAmount());

        assertManagedCardBalance(managedCardSource, corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination, corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ExistingSourceManagedAccountLinkedToDebitCard_Success() {
        final String existingCorporateAuthenticationTokenSource = existingCorporateAuthenticationToken;
        final String corporateCurrencySource = existingCorporateCurrency;

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final String manageAccountSource = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, existingCorporateAuthenticationTokenSource);
        final String manageAccountDestination = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);

        ManagedCardsHelper.createDebitManagedCard(corporateDebitManagedCardsProfileId, manageAccountSource, secretKey, existingCorporateAuthenticationTokenSource);

        AdminHelper.fundManagedAccount(innovatorId, manageAccountSource, corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(manageAccountSource, MANAGED_ACCOUNTS),
                Pair.of(manageAccountDestination, MANAGED_ACCOUNTS), corporateCurrencySource, transferAmount, existingCorporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(manageAccountSource, existingCorporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(manageAccountDestination, corporateAuthenticationToken, transferAmount.intValue());
    }

    @Test
    public void SendFunds_DestinationManagedAccountLinkedToDebitCard_Success() {
        final String corporateAuthenticationTokenSource = corporateAuthenticationToken;
        final String corporateCurrencySource = corporateCurrency;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final String manageAccountSource = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, corporateAuthenticationTokenSource);
        final String manageAccountDestination = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, secretKey, corporateAuthenticationTokenDestination);

        ManagedCardsHelper.createDebitManagedCard(corporateDebitManagedCardsProfileId, manageAccountDestination, secretKey, corporateAuthenticationTokenDestination);

        AdminHelper.fundManagedAccount(innovatorId, manageAccountSource, corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(manageAccountSource, MANAGED_ACCOUNTS),
                Pair.of(manageAccountDestination, MANAGED_ACCOUNTS), corporateCurrencySource, transferAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(manageAccountSource, corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(manageAccountDestination, corporateAuthenticationTokenDestination, transferAmount.intValue());
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_CorporateOtp_Success(final Currency currency, final OwtType type, final FeeType feeType) {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenOtp = authenticatedCorporate.getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationTokenOtp);

        AdminHelper.fundManagedAccount(innovatorId, managedAccount, currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                                managedAccount,
                                currency.name(), sendAmount, type)
                        .setDescription(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        assertSuccessfulResponse(OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenOtp, Optional.empty())
                        .then()
                        .statusCode(SC_OK), outgoingWireTransfersModel, type, corporateAuthenticationTokenOtp,
                OTP_CHANNEL);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount, corporateAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_CorporateAuthy_Success(final Currency currency, final OwtType type, final FeeType feeType) {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateAuthenticationTokenPush = authenticatedCorporate.getRight();

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationTokenPush);

        AdminHelper.fundManagedAccount(innovatorId, managedAccount, currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount,
                        currency.name(), sendAmount, type).build();

        assertSuccessfulResponse(OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, corporateAuthenticationTokenPush, Optional.empty())
                        .then()
                        .statusCode(SC_OK), outgoingWireTransfersModel, type, corporateAuthenticationTokenPush,
                PUSH_CHANNEL);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount, corporateAuthenticationTokenPush, currentBalance);
    }

    @Test
    public void SendOwt_CorporateEurUnderNonFpsEnabledTenantOtp_Success() {
        final Currency currency = Currency.EUR;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsTenant.getSecretKey());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, nonFpsTenant.getSecretKey(), corporate.getRight());

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount
                        (nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), currency.name(), nonFpsTenant.getSecretKey(), corporate.getRight());

        AdminHelper.fundManagedAccount(nonFpsTenant.getInnovatorId(), managedAccount, currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(nonFpsTenant.getOwtProfileId(),
                        managedAccount,
                        currency.name(), sendAmount, OwtType.SEPA).build();

        assertSuccessfulResponse(OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, nonFpsTenant.getSecretKey(), corporate.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK), outgoingWireTransfersModel, OwtType.SEPA, corporate.getRight(), nonFpsTenant.getSecretKey(),
                OTP_CHANNEL);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount, corporate.getRight(), nonFpsTenant.getSecretKey(), currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_ExistingConsumer_Success(final Currency currency) {
        final String existingConsumerAuthenticationTokenOtp = existingConsumerAuthenticationToken;

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, currency.name(), secretKey, existingConsumerAuthenticationTokenOtp);

        AdminHelper.fundManagedAccount(innovatorId, managedAccount, currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount,
                        currency.name(), sendAmount, OwtType.SEPA).build();

        assertSuccessfulResponse(OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, existingConsumerAuthenticationTokenOtp, Optional.empty())
                        .then()
                        .statusCode(SC_OK), outgoingWireTransfersModel, OwtType.SEPA, existingConsumerAuthenticationTokenOtp,
                OTP_CHANNEL);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount, existingConsumerAuthenticationTokenOtp, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = {"EUR"})
    public void SendOwt_ExistingCorporateUser_Success(final Currency currency) {
        final String existingCorporateAuthenticationTokenOtp = existingCorporateAuthenticationToken;

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(UsersModel.DefaultUsersModel().build(), secretKey, existingCorporateAuthenticationTokenOtp);

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, OTP_CHANNEL, secretKey, user.getRight());

        final String managedAccount =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, user.getRight());

        AdminHelper.fundManagedAccount(innovatorId, managedAccount, currency.name(), depositAmount);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount,
                        currency.name(), sendAmount, OwtType.SEPA).build();

        assertSuccessfulResponse(OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, user.getRight(), Optional.empty())
                        .then()
                        .statusCode(SC_OK), outgoingWireTransfersModel, OwtType.SEPA, user.getRight(),
                OTP_CHANNEL);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount, user.getRight(), currentBalance);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_DebitCorporate_Success(final InstrumentType instrumentType) {
        final Long depositAmount = 10000L;
        final Long availableToSpend = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken, depositAmount);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(corporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                corporateAuthenticationToken);

        final String purchaseCode = SimulatorHelper.simulateCardPurchaseById
                (secretKey, managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, corporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_ExistingCorporateMultipleSpendLimitIntervals_Success() {
        final Long depositAmount = 10000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(existingCorporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, existingCorporateCurrency, existingCorporateAuthenticationToken, depositAmount);

        final List<SpendLimitModel> spendLimits =
                Arrays.asList(new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 10000L), LimitInterval.ALWAYS),
                        new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 5000L), LimitInterval.YEARLY),
                        new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 2500L), LimitInterval.QUARTERLY),
                        new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 1250L), LimitInterval.MONTHLY),
                        new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 750L), LimitInterval.WEEKLY),
                        new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, 300L), LimitInterval.DAILY));

        setSpendLimit(managedCard.getManagedCardId(),
                spendLimits,
                existingCorporateAuthenticationToken);

        final String purchaseCode = SimulatorHelper.simulateCardPurchaseById
                (secretKey, managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, purchaseAmount));

        final List<SpendLimitModel> updatedSpendLimits = new ArrayList<>();
        spendLimits.forEach(limit -> {
            final Long amount = limit.getValue().getAmount() - purchaseAmount - purchaseFee;
            updatedSpendLimits
                    .add(new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, amount),
                            LimitInterval.valueOf(limit.getInterval())));
        });

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final List<SpendLimitResponseModel> remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpendList(secretKey, managedCard.getManagedCardId(), existingCorporateAuthenticationToken, updatedSpendLimits);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, existingCorporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        updatedSpendLimits.forEach(expectedLimit -> {
            final SpendLimitResponseModel actualOriginalLimit =
                    remainingAvailableToSpend.stream().filter(x -> x.getInterval().equals(expectedLimit.getInterval())).collect(Collectors.toList()).get(0);

            assertEquals(expectedLimit.getValue().getAmount().toString(), actualOriginalLimit.getValue().get("amount"));
            assertEquals(expectedLimit.getInterval(), actualOriginalLimit.getInterval());
        });
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void CardPurchase_PrepaidCorporate_Success(final InstrumentType instrumentType) {
        final Long depositAmount = 1000L;
        final Long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(corporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createPrepaidManagedCardPurchases(corporatePrepaidManagedCardsProfileId, corporateCurrency, corporateAuthenticationToken);

        transferFundsToCard(corporateAuthenticationToken, corporateManagedAccountsProfileId, managedCard.getManagedCardId(),
                corporateCurrency, depositAmount);

        if (instrumentType.equals(InstrumentType.PHYSICAL)) {
            ManagedCardsHelper.upgradeAndActivateManagedCardToPhysical(secretKey, managedCard.getManagedCardId(), corporateAuthenticationToken);
        }

        final String purchaseCode = SimulatorHelper.simulateCardPurchaseById
                (secretKey, managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int managedCardExpectedBalance =
                (int) (depositAmount - purchaseAmount - purchaseFee);

        final BalanceModel managedAccountBalance =
                ManagedCardsHelper.getManagedCardBalance(managedCard.getManagedCardId(),
                        secretKey, corporateAuthenticationToken, managedCardExpectedBalance);

        Assertions.assertEquals("APPROVED", purchaseCode);
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedCardExpectedBalance, managedAccountBalance.getActualBalance());
    }

    @Test
    public void CardPurchase_ExistingCorporateManagedAccountStatementChecks_Success() {
        final Long depositAmount = 10000L;
        final long availableToSpend = 1000L;
        final long purchaseAmount = 100L;
        final Long purchaseFee = TestHelper.getFees(existingCorporateCurrency).get(FeeType.PURCHASE_FEE).getAmount();

        final ManagedCardDetails managedCard =
                createFundedManagedAccountAndDebitCard(corporateManagedAccountsProfileId, corporateDebitManagedCardsProfileId, existingCorporateCurrency, existingCorporateAuthenticationToken, depositAmount);

        setSpendLimit(managedCard.getManagedCardId(),
                Collections.singletonList(new SpendLimitModel(new CurrencyAmount(existingCorporateCurrency, availableToSpend), LimitInterval.ALWAYS)),
                existingCorporateAuthenticationToken);

        SimulatorHelper.simulateCardPurchaseById
                (secretKey, managedCard.getManagedCardId(), new CurrencyAmount(corporateCurrency, purchaseAmount));

        final int expectedAvailableToSpend = (int) (availableToSpend - purchaseAmount - purchaseFee);

        final int managedAccountExpectedBalance =
                (int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee);

        final int remainingAvailableToSpend =
                ManagedCardsHelper.getAvailableToSpend(secretKey, managedCard.getManagedCardId(), existingCorporateAuthenticationToken, expectedAvailableToSpend);

        final BalanceModel managedAccountBalance =
                ManagedAccountsHelper.getManagedAccountBalance(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, existingCorporateAuthenticationToken, managedAccountExpectedBalance);

        Assertions.assertEquals(expectedAvailableToSpend, remainingAvailableToSpend);
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getAvailableBalance());
        Assertions.assertEquals(managedAccountExpectedBalance, managedAccountBalance.getActualBalance());

        final Response managedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedCard.getManagedCardModel().getParentManagedAccountId(),
                        secretKey, existingCorporateAuthenticationToken, 3);

        managedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SETTLEMENT"))
                .body("entry[0].transactionAmount.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[0].balanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].balanceAfter.amount", equalTo(managedAccountExpectedBalance))
                .body("entry[0].cardholderFee.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].cardholderFee.amount", equalTo(purchaseFee.intValue()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].availableBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact(purchaseFee.intValue())))
                .body("entry[0].actualBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].actualBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount - purchaseFee)))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (purchaseAmount + purchaseFee))))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[0].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[0].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[0].additionalFields.merchantTransactionType", equalTo("SALE_PURCHASE"))
                .body("entry[0].additionalFields.authorisationCode", notNullValue())
                .body("entry[0].additionalFields.authorisationRelatedId", equalTo(managedAccountStatement.jsonPath().get("entry[1].transactionId.id")))
                .body("entry[0].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("AUTHORISATION"))
                .body("entry[1].transactionAmount.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].balanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].cardholderFee.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].availableBalanceAfter.amount", equalTo((int) (managedCard.getInitialManagedAccountBalance() - purchaseAmount)))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) purchaseAmount)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.merchantName", equalTo("Amazon IT"))
                .body("entry[1].additionalFields.merchantCategoryCode", equalTo("5399"))
                .body("entry[1].additionalFields.merchantTerminalCountry", equalTo("MT"))
                .body("entry[1].additionalFields.forexPaddingCurrency", equalTo(existingCorporateCurrency))
                .body("entry[1].additionalFields.forexPaddingAmount", equalTo("0"))
                .body("entry[1].additionalFields.authorisationCode", notNullValue())
                .body("entry[1].additionalFields.authorisationState", equalTo("COMPLETED"))
                .body("entry[1].additionalFields.relatedCardId", equalTo(managedCard.getManagedCardId()))
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionAmount.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].transactionAmount.amount", equalTo(managedCard.getInitialDepositAmount()))
                .body("entry[2].balanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].balanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].cardholderFee.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(existingCorporateCurrency).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].availableBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].availableBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].availableBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].availableBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAfter.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].actualBalanceAfter.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].actualBalanceAdjustment.currency", equalTo(existingCorporateCurrency))
                .body("entry[2].actualBalanceAdjustment.amount", equalTo(managedCard.getInitialManagedAccountBalance()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("entry[2].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[2].additionalFields.sender", equalTo("Sender Test"))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }


    private void assertSuccessfulSend(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                      final Pair<String, ManagedInstrumentType> destinationInstrument,
                                      final String currency,
                                      final Long sendAmount,
                                      final String token) {
        assertSuccessfulSend(sourceInstrument, destinationInstrument, sendsProfileId, currency, sendAmount, secretKey, token);
    }

    private void assertSuccessfulSend(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                      final Pair<String, ManagedInstrumentType> destinationInstrument,
                                      final String profileId,
                                      final String currency,
                                      final Long sendAmount,
                                      final String secretKey,
                                      final String token) {
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(profileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                        .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                        .build();


        SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(profileId))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                          final OwtType owtType,
                                          final String token,
                                          final String enrolmentChannel) {
        assertSuccessfulResponse(response, outgoingWireTransfersModel, owtType, token, secretKey, enrolmentChannel);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                          final OwtType owtType,
                                          final String token,
                                          final String secretKey,
                                          final String enrolmentChannel) {
        response.body("id", notNullValue())
                .body("profileId", equalTo(outgoingWireTransfersModel.getProfileId()))
                .body("tag", equalTo(outgoingWireTransfersModel.getTag()))
                .body("sourceInstrument.type", equalTo(outgoingWireTransfersModel.getSourceInstrument().getType()))
                .body("sourceInstrument.id", equalTo(outgoingWireTransfersModel.getSourceInstrument().getId()))
                .body("transferAmount.currency", equalTo(outgoingWireTransfersModel.getTransferAmount().getCurrency()))
                .body("transferAmount.amount", equalTo(outgoingWireTransfersModel.getTransferAmount().getAmount().intValue()))
                .body("description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("destination.name", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("destination.address", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getAddress()))
                .body("destination.bankName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankName()))
                .body("destination.bankAddress", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankAddress()))
                .body("destination.bankCountry", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankCountry()))
                .body("state", equalTo("PENDING_CHALLENGE"))
                .body("creationTimestamp", notNullValue());

        if (EnrolmentChannel.SMS.name().equals(enrolmentChannel)) {
            verifyOwtOtp(response.extract().jsonPath().getString("id"), secretKey, token);
        } else {
            verifyOwtPush(response.extract().jsonPath().getString("id"), token);
        }

        OutgoingWireTransfersHelper
                .checkOwtStateByAccountId(outgoingWireTransfersModel.getSourceInstrument().getId(), "COMPLETED");

        assertBankAccountDetails(response, outgoingWireTransfersModel, owtType);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel,
                                          final OwtType owtType) {
        switch (owtType) {
            case SEPA:
                final SepaBankDetailsModel sepaBankDetails =
                        (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("type", equalTo(owtType.name()))
                        .body("destination.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                        .body("destination.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()));
                break;
            case FASTER_PAYMENTS:
                final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
                        (FasterPaymentsBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("type", equalTo(owtType.name()))
                        .body("destination.bankAccountDetails.accountNumber", equalTo(fasterPaymentsBankDetails.getAccountNumber()))
                        .body("destination.bankAccountDetails.sortCode", equalTo(fasterPaymentsBankDetails.getSortCode()));
                break;
            default:
                throw new IllegalArgumentException("OWT type not supported.");
        }
    }

    private void assertSuccessfulTransfer(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                          final Pair<String, ManagedInstrumentType> destinationInstrument,
                                          final String currency,
                                          final Long transferAmount,
                                          final String token) {
        assertSuccessfulTransfer(sourceInstrument, destinationInstrument, transfersProfileId, currency, transferAmount, secretKey, token);
    }

    private void assertSuccessfulTransfer(final Pair<String, ManagedInstrumentType> sourceInstrument,
                                          final Pair<String, ManagedInstrumentType> destinationInstrument,
                                          final String profile,
                                          final String currency,
                                          final Long transferAmount,
                                          final String secretKey,
                                          final String token) {
        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(profile)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                        .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                        .build();


        TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(profile))
                .body("tag", equalTo(transferFundsModel.getTag()))
                .body("source.type", equalTo(transferFundsModel.getSource().getType()))
                .body("source.id", equalTo(transferFundsModel.getSource().getId()))
                .body("destination.type", equalTo(transferFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(transferFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(transferFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(transferFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance) {
        assertManagedAccountBalance(managedAccountId, token, secretKey, balance);
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final String secretKey, final int balance) {
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private void assertManagedCardBalance(final String managedCardId, final String token, final int balance) {
        ManagedCardsService.getManagedCard(secretKey, managedCardId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private void verifyOwtOtp(final String id,
                              final String secretKey,
                              final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, OTP_CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                        OTP_CHANNEL, secretKey, token),
                SC_NO_CONTENT);
    }

    private void verifyOwtPush(final String id,
                               final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferPushVerification(id, PUSH_CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        SimulatorHelper.acceptAuthyOwt(secretKey, id);
    }

    private static String createManagedAccount(final String managedAccountsProfileId, final String currency, final String token) {
        return ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(managedAccountsProfileId, currency).build(),
                        secretKey, token);
    }


    private static ManagedCardDetails createFundedManagedAccountAndDebitCard(final String managedAccountProfileId,
                                                                             final String managedCardProfileId,
                                                                             final String currency,
                                                                             final String authenticationToken,
                                                                             final Long depositAmount) {
        final String managedAccountId =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken);

        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        final int balance = (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());

        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(DEBIT_MODE)
                .setInstrumentType(VIRTUAL)
                .setInitialManagedAccountBalance(balance)
                .setInitialDepositAmount(depositAmount.intValue())
                .build();
    }

    private static Pair<String, CreateManagedAccountModel> transferFundsToCard(final String token,
                                                                               final String corporateManagedAccountsProfileId,
                                                                               final String managedCardId,
                                                                               final String currency,
                                                                               final Long depositAmount) {

        return TestHelper
                .simulateManagedAccountDepositAndTransferToCard(corporateManagedAccountsProfileId,
                        transfersProfileId, managedCardId, currency, depositAmount, secretKey, token);
    }

    private static ManagedCardDetails createPrepaidManagedCardPurchases(final String managedCardProfileId,
                                                                        final String currency,
                                                                        final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return ManagedCardDetails.builder()
                .setManagedCardId(managedCardId)
                .setManagedCardModel(createManagedCardModel)
                .setManagedCardMode(PREPAID_MODE)
                .setInstrumentType(VIRTUAL)
                .build();
    }

    private void setSpendLimit(final String managedCardId,
                               final List<SpendLimitModel> spendLimit,
                               final String authenticationToken) {
        final SpendRulesModel spendRulesModel = getDefaultSpendRulesModel(spendLimit)
                .build();

        ManagedCardsHelper.setSpendLimit(spendRulesModel, secretKey, managedCardId, authenticationToken);
    }

    private SpendRulesModel.Builder getDefaultSpendRulesModel(final List<SpendLimitModel> spendLimit) {
        return SpendRulesModel
                .builder()
                .setAllowedMerchantCategories(new ArrayList<>())
                .setBlockedMerchantCategories(Arrays.asList("7995", "1234"))
                .setAllowedMerchantIds(new ArrayList<>())
                .setBlockedMerchantIds(new ArrayList<>())
                .setAllowContactless(true)
                .setAllowAtm(true)
                .setAllowECommerce(true)
                .setAllowCashback(true)
                .setAllowCreditAuthorisations(true)
                .setSpendLimit(spendLimit);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }

}
