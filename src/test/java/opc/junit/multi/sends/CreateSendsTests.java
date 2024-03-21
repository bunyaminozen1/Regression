package opc.junit.multi.sends;

import commons.enums.Currency;
import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwnerType;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.SendsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.admin.SetLimitModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static opc.enums.opc.ManagedInstrumentType.UNKNOWN;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag(MultiTags.SENDS)
public class CreateSendsTests extends BaseSendsSetup {

    private static String corporateAuthenticationTokenSource;
    private static String consumerAuthenticationTokenSource;
    private static String corporateCurrencySource;
    private static String consumerCurrencySource;
    private static String corporateIdSource;
    private static String consumerIdSource;
    private static String corporateNameSource;
    private static String consumerNameSource;
    private static String innovatorId;
    private static String corporateIdDestination;
    private static String corporateNameDestination;
    private static String corporateAuthenticationTokenDestination;
    private static String corporateAuthenticationTokenDestinationDifferentProgramme;

    private static String consumerIdDestination;
    private static String consumerNameDestination;
    private static String consumerAuthenticationTokenDestination;


    @BeforeAll
    public static void Setup() {

        innovatorId = applicationOne.getInnovatorId();

        corporateSetupSource();
        consumerSetupSource();

        corporateSetupDestination();
        corporateSetupDestinationDifferentProgramme();
        consumerSetupDestination();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void SendFunds_CorporateMaToMa_Success(final Currency currency) {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), currency.name(), depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), currency.name(), sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());
    }


    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void SendFunds_CorporateMaToMaUnderNonFpsEnabledTenant_Success(final Currency currency) {

        final CreateCorporateModel createCorporateModelSource =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();
        final CreateCorporateModel createCorporateModelDestination =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsTenant.getCorporatesProfileId())
                        .setBaseCurrency(currency.name())
                        .build();

        final Pair<String, String> corporateSource =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelSource,
                        nonFpsTenant.getSecretKey());

        final Pair<String, String> corporateDestination =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelDestination,
                        nonFpsTenant.getSecretKey());

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), currency.name(),
                nonFpsTenant.getSecretKey(), corporateSource.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(nonFpsTenant.getCorporatePayneticsEeaManagedAccountsProfileId(), currency.name(),
                nonFpsTenant.getSecretKey(), corporateDestination.getRight());


        fundManagedAccount(managedAccountSource.getLeft(), currency.name(), depositAmount, nonFpsTenant.getInnovatorId());

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), nonFpsTenant.getSendProfileId(),
                currency.name(), sendAmount, nonFpsTenant.getSecretKey(), corporateSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(currency.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), nonFpsTenant.getSecretKey(), corporateSource.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), nonFpsTenant.getSecretKey(), corporateDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_ConsumerMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);


        fundManagedAccount(managedAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), consumerCurrencySource, sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(consumerCurrencySource).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateUserMaToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final UsersModel usersModelSource = UsersModel.DefaultUsersModel().build();
        final UsersModel usersModelDestination = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> userSource = UsersHelper.createAuthenticatedUser(usersModelSource, secretKey, corporateAuthenticationTokenSource);
        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(usersModelDestination, secretKey, corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, userSource.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, userDestination.getRight());


        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, userSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), userSource.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), userDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(manageCardSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(manageCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(manageCardDestination.getLeft(), MANAGED_CARDS), corporateCurrencySource, sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MC_TO_MC_SEND_FEE).getAmount());

        assertManagedCardBalance(manageCardSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedCardBalance(manageCardDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ConsumerMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedCard(manageCardSource.getLeft(), consumerCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(manageCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(manageCardDestination.getLeft(), MANAGED_CARDS), consumerCurrencySource, sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(consumerCurrencySource).get(FeeType.MC_TO_MC_SEND_FEE).getAmount());

        assertManagedCardBalance(manageCardSource.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedCardBalance(manageCardDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateUserMcToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final UsersModel usersModelSource = UsersModel.DefaultUsersModel().build();
        final UsersModel usersModelDestination = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> userSource = UsersHelper.createAuthenticatedUser(usersModelSource, secretKey, corporateAuthenticationTokenSource);
        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(usersModelDestination, secretKey, corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedCardModel> managedCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, userSource.getRight());
        final Pair<String, CreateManagedCardModel> managedCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, userDestination.getRight());

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(managedCardDestination.getLeft(), MANAGED_CARDS), corporateCurrencySource, sendAmount, userSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MC_TO_MC_SEND_FEE).getAmount());

        assertManagedCardBalance(managedCardSource.getLeft(), userSource.getRight(), sourceBalance);
        assertManagedCardBalance(managedCardDestination.getLeft(), userDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MC_TO_MA_SEND_FEE).getAmount());

        assertManagedCardBalance(managedCardSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ConsumerMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), consumerCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), consumerCurrencySource, sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(consumerCurrencySource).get(FeeType.MC_TO_MA_SEND_FEE).getAmount());

        assertManagedCardBalance(managedCardSource.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateUserMcToMa_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final UsersModel usersModelSource = UsersModel.DefaultUsersModel().build();
        final UsersModel usersModelDestination = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> userSource = UsersHelper.createAuthenticatedUser(usersModelSource, secretKey, corporateAuthenticationTokenSource);
        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(usersModelDestination, secretKey, corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedCardModel> managedCard =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, userSource.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, userDestination.getRight());

        fundManagedCard(managedCard.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedCard.getLeft(), MANAGED_CARDS),
                Pair.of(managedAccount.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, userSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MC_TO_MA_SEND_FEE).getAmount());

        assertManagedCardBalance(managedCard.getLeft(), userSource.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccount.getLeft(), userDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCardDestination.getLeft(), MANAGED_CARDS), corporateCurrencySource, sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MC_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedCardBalance(managedCardDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_ConsumerMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCardDestination.getLeft(), MANAGED_CARDS), consumerCurrencySource, sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(consumerCurrencySource).get(FeeType.MA_TO_MC_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedCardBalance(managedCardDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_CorporateUserMaToMc_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final UsersModel usersModelSource = UsersModel.DefaultUsersModel().build();
        final UsersModel usersModelDestination = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> userSource = UsersHelper.createAuthenticatedUser(usersModelSource, secretKey, corporateAuthenticationTokenSource);
        final Pair<String, String> userDestination = UsersHelper.createAuthenticatedUser(usersModelDestination, secretKey, corporateAuthenticationTokenDestination);

        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, userSource.getRight());
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, userDestination.getRight());

        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedCardDestination.getLeft(), MANAGED_CARDS), corporateCurrencySource, sendAmount, userSource.getRight());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MC_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), userSource.getRight(), sourceBalance);
        assertManagedCardBalance(managedCardDestination.getLeft(), userDestination.getRight(), sendAmount.intValue());
    }

    @Test
    public void SendFunds_SourceManagedAccountLinkedToDebitCard_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountSource.getLeft(), corporateAuthenticationTokenSource);

        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, transferAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, transferAmount.intValue());
    }

    @Test
    public void SendFunds_DestinationManagedAccountLinkedToDebitCard_Success() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, transferAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - transferAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_TRANSFER_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, transferAmount.intValue());
    }

    @Test
    public void SendFunds_RequiredFieldsOnly_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);


        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_SameIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(SendsService.sendFunds(sendFundsModel, secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(SendsService.sendFunds(sendFundsModel, secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response ->

                response.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(sendsProfileId))
                        .body("tag", equalTo(sendFundsModel.getTag()))
                        .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                        .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                        .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                        .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                SendsService.getSends(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    @DisplayName("SendFunds_SameIdempotencyRefDifferentPayload_PreconditionFailed - DEV-1614 opened to return 412")
    public void SendFunds_SameIdempotencyRefDifferentPayload_PreconditionFailed() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);


        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final SendFundsModel sendFundsModel1 =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        SendsService.sendFunds(sendFundsModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_DifferentIdempotencyRefSamePayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(SendsService.sendFunds(sendFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));
        responses.add(SendsService.sendFunds(sendFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)));
        responses.add(SendsService.sendFunds(sendFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.empty()));

        responses.forEach(response ->

                response.then()
                        .statusCode(SC_OK));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                SendsService.getSends(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendFunds_DifferentIdempotencyRefDifferentPayload_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();
        final SendFundsModel sendFundsModel1 =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final Map<Response, SendFundsModel> responses = new HashMap<>();

        responses.put(SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                sendFundsModel);
        responses.put(SendsService.sendFunds(sendFundsModel1,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference1)),
                sendFundsModel1);
        responses.put(SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.empty()),
                sendFundsModel);

        responses.forEach((key, value) ->

                key.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(sendsProfileId))
                        .body("tag", equalTo(value.getTag()))
                        .body("source.type", equalTo(value.getSource().getType()))
                        .body("source.id", equalTo(value.getSource().getId()))
                        .body("destination.type", equalTo(value.getDestination().getType()))
                        .body("destination.id", equalTo(value.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(value.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(value.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo("COMPLETED"))
                        .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertNotEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertNotEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responseList.get(1).jsonPath().getString("id"), responseList.get(2).jsonPath().getString("id"));
        assertNotEquals(responseList.get(1).jsonPath().getString("creationTimestamp"), responseList.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                SendsService.getSends(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendFunds_LongIdempotencyRef_RequestTooLong() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void SendFunds_ExpiredIdempotencyRef_NewRequestSuccess() throws InterruptedException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        responses.add(SendsService.sendFunds(sendFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        TimeUnit.SECONDS.sleep(18);

        responses.add(SendsService.sendFunds(sendFundsModel,
                secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)));

        responses.forEach(response -> response.then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(sendsProfileId))
                .body("tag", equalTo(sendFundsModel.getTag()))
                .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue()));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("2",
                SendsService.getSends(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendFunds_SameIdempotencyRefDifferentPayloadInitialCallFailed_BadRequest() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel.Builder sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS));

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        SendsService.sendFunds(sendFundsModel.setProfileId("123").build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        SendsService.sendFunds(sendFundsModel.setProfileId(sendsProfileId).build(),
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_SameIdempotencyRefSamePayloadWithChange_Success() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), authenticatedCorporate.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), createCorporateModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(createCorporateModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .setScheduledTimestamp(TestHelper.generateTimestampAfter(30))
                        .build();

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Map<Response, State> responses = new HashMap<>();

        final Response initialResponse =
                SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference));

        responses.put(initialResponse, State.SCHEDULED);

        SendsHelper.cancelScheduledTransfer(secretKey, initialResponse.jsonPath().getString("id"), authenticatedCorporate.getRight());

        responses.put(SendsService.sendFunds(sendFundsModel,
                        secretKey, authenticatedCorporate.getRight(), Optional.of(idempotencyReference)),
                State.CANCELLED);

        responses.forEach((key, value) ->

                key.then()
                        .statusCode(SC_OK)
                        .body("id", notNullValue())
                        .body("profileId", equalTo(sendsProfileId))
                        .body("tag", equalTo(sendFundsModel.getTag()))
                        .body("source.type", equalTo(sendFundsModel.getSource().getType()))
                        .body("source.id", equalTo(sendFundsModel.getSource().getId()))
                        .body("destination.type", equalTo(sendFundsModel.getDestination().getType()))
                        .body("destination.id", equalTo(sendFundsModel.getDestination().getId()))
                        .body("destinationAmount.currency", equalTo(sendFundsModel.getDestinationAmount().getCurrency()))
                        .body("destinationAmount.amount", equalTo(sendFundsModel.getDestinationAmount().getAmount().intValue()))
                        .body("state", equalTo(value.name()))
                        .body("creationTimestamp", notNullValue()));

        final List<Response> responseList = new ArrayList<>(responses.keySet());

        assertEquals(responseList.get(0).jsonPath().getString("id"), responseList.get(1).jsonPath().getString("id"));
        assertEquals(responseList.get(0).jsonPath().getString("creationTimestamp"), responseList.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                SendsService.getSends(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendFunds_ManagedAccountStatement_Success() {
        final Long depositAmount = 10000L;
        final Long depositFeeAmount = TestHelper.getFees(corporateCurrencySource).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long sendAmount = 500L;
        final Long sendFeeAmount = TestHelper.getFees(corporateCurrencySource).get(FeeType.MA_TO_MA_SEND_FEE).getAmount();

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountSource.getLeft(), secretKey, corporateAuthenticationTokenSource);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountSource.getLeft(),
                corporateCurrencySource,
                depositAmount,
                secretKey,
                corporateAuthenticationTokenSource);

        assertSuccessfulSend(Pair.of(managedAccountSource.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS), corporateCurrencySource, sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (preSendBalance - sendAmount -
                        sendFeeAmount);

        assertManagedAccountBalance(managedAccountSource.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), corporateAuthenticationTokenDestination, sendAmount.intValue());

        final Response sourceManagedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedAccountSource.getLeft(), secretKey, corporateAuthenticationTokenSource, 2);

        final Response destinationManagedAccountStatement =
                ManagedAccountsHelper.getManagedAccountStatement(managedAccountDestination.getLeft(), secretKey, corporateAuthenticationTokenDestination, 2);

        sourceManagedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SEND"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrencySource))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[0].balanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrencySource))
                .body("entry[0].cardholderFee.amount", equalTo(sendFeeAmount.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.destinationIdentityType", equalTo(OwnerType.CORPORATE.getValue()))
                .body("entry[0].additionalFields.destinationIdentityId", equalTo(corporateIdDestination))
                .body("entry[0].additionalFields.destinationIdentityName", equalTo(corporateNameDestination))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrencySource))
                .body("entry[1].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(corporateCurrencySource))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(corporateCurrencySource))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo((int) (depositAmount - depositFeeAmount)))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[1].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[1].availableBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrencySource))
                .body("entry[1].cardholderFee.amount", equalTo(depositFeeAmount.intValue()))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        destinationManagedAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SEND"))
                .body("entry[0].transactionAmount.currency", equalTo(corporateCurrencySource))
                .body("entry[0].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].balanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[0].balanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(corporateCurrencySource))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.sourceIdentityType", equalTo(OwnerType.CORPORATE.getValue()))
                .body("entry[0].additionalFields.sourceIdentityId", equalTo(corporateIdSource))
                .body("entry[0].additionalFields.sourceIdentityName", equalTo(corporateNameSource))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("SEND"))
                .body("entry[1].transactionAmount.currency", equalTo(corporateCurrencySource))
                .body("entry[1].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.currency", equalTo(corporateCurrencySource))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.currency", equalTo(corporateCurrencySource))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.sourceIdentityType", equalTo(OwnerType.CORPORATE.getValue()))
                .body("entry[1].additionalFields.sourceIdentityId", equalTo(corporateIdSource))
                .body("entry[1].additionalFields.sourceIdentityName", equalTo(corporateNameSource))
                .body("entry[1].entryState", equalTo("PENDING"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void SendFunds_ManagedCardStatement_Success() {
        final Long depositAmount = 8000L;
        final Long sendAmount = 500L;
        final Long sendFeeAmount = TestHelper.getFees(consumerCurrencySource).get(FeeType.MC_TO_MC_SEND_FEE).getAmount();

        final Pair<String, CreateManagedCardModel> managedCardSource = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        final int preSendBalance = simulateManagedCardDepositAndCheckBalance(consumerManagedAccountProfileId,
                managedCardSource.getLeft(),
                consumerCurrencySource,
                depositAmount,
                secretKey,
                consumerAuthenticationTokenSource);

        assertSuccessfulSend(Pair.of(managedCardSource.getLeft(), MANAGED_CARDS),
                Pair.of(managedCardDestination.getLeft(), MANAGED_CARDS), consumerCurrencySource, sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (preSendBalance - sendAmount -
                        sendFeeAmount);

        assertManagedCardBalance(managedCardSource.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedCardBalance(managedCardDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());

        final Response sourceManagedCardStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCardSource.getLeft(), secretKey, consumerAuthenticationTokenSource, 2);

        final Response destinationCardAccountStatement =
                ManagedCardsHelper.getManagedCardStatement(managedCardDestination.getLeft(), secretKey, consumerAuthenticationTokenDestination, 2);

        sourceManagedCardStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SEND"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrencySource))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrencySource))
                .body("entry[0].balanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) (sendAmount + sendFeeAmount))))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sourceBalance))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrencySource))
                .body("entry[0].cardholderFee.amount", equalTo(sendFeeAmount.intValue()))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.destinationIdentityType", equalTo(OwnerType.CONSUMER.getValue()))
                .body("entry[0].additionalFields.destinationIdentityId", equalTo(consumerIdDestination))
                .body("entry[0].additionalFields.destinationIdentityName", equalTo(consumerNameDestination))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrencySource))
                .body("entry[1].transactionAmount.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(preSendBalance))
                .body("entry[1].availableBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrencySource))
                .body("entry[1].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrencySource))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].entryState", equalTo("COMPLETED"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));

        destinationCardAccountStatement
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", notNullValue())
                .body("entry[0].transactionId.type", equalTo("SEND"))
                .body("entry[0].transactionAmount.currency", equalTo(consumerCurrencySource))
                .body("entry[0].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].balanceAfter.currency", equalTo(consumerCurrencySource))
                .body("entry[0].balanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[0].cardholderFee.currency", equalTo(consumerCurrencySource))
                .body("entry[0].cardholderFee.amount", equalTo(0))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.sourceIdentityType", equalTo(OwnerType.CONSUMER.getValue()))
                .body("entry[0].additionalFields.sourceIdentityId", equalTo(consumerIdSource))
                .body("entry[0].additionalFields.sourceIdentityName", equalTo(consumerNameSource))
                .body("entry[0].entryState", equalTo("COMPLETED"))
                .body("entry[1].transactionId.id", notNullValue())
                .body("entry[1].transactionId.type", equalTo("SEND"))
                .body("entry[1].transactionAmount.currency", equalTo(consumerCurrencySource))
                .body("entry[1].transactionAmount.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(sendAmount.intValue()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].availableBalanceAfter.amount", equalTo(0))
                .body("entry[1].balanceAfter.currency", equalTo(consumerCurrencySource))
                .body("entry[1].balanceAfter.amount", equalTo(0))
                .body("entry[1].cardholderFee.currency", equalTo(consumerCurrencySource))
                .body("entry[1].cardholderFee.amount", equalTo(0))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.sourceIdentityType", equalTo(OwnerType.CONSUMER.getValue()))
                .body("entry[1].additionalFields.sourceIdentityId", equalTo(consumerIdSource))
                .body("entry[1].additionalFields.sourceIdentityName", equalTo(consumerNameSource))
                .body("entry[1].entryState", equalTo("PENDING"))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void SendFunds_SendToNonActiveAccount_InstrumentDeniedTransaction() {

        final Currency currency = Currency.GBP;

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationTokenSource);

        fundManagedAccount(managedAccount.getLeft(), currency.name(), 10000L);

        final Pair<String, CreateManagedAccountModel> pendingApprovalManagedAccount =
                createPendingApprovalManagedAccount(corporateManagedAccountProfileId, corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency.name(), 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(pendingApprovalManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_DENIED_TRANSACTION"));
    }

    @Test
    public void SendFunds_CorporateMaToMaDifferentProgramme_Conflict() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrencySource, 10000L);

        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(applicationFourCorporateManagedAccountProfileId, corporateCurrencySource, applicationFourSecretKey, corporateAuthenticationTokenDestinationDifferentProgramme);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_IN_SAME_PROGRAMME"));
    }

    @Test
    public void SendFunds_CorporateMaToMcDifferentProgramme_Conflict() {

        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        final Pair<String, CreateManagedCardModel> managedCardDifferentProgramme =
                createPrepaidManagedCard(applicationFourCorporatePrepaidManagedCardsProfileId, corporateCurrencySource, applicationFourSecretKey, corporateAuthenticationTokenDestinationDifferentProgramme);

        fundManagedAccount(managedAccount.getLeft(), corporateCurrencySource, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDifferentProgramme.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_IN_SAME_PROGRAMME"));
    }

    @Test
    public void SendFunds_CorporateMcToMcDifferentProgramme_Conflict() {

        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> manageCardDestination = createPrepaidManagedCard(applicationFourCorporatePrepaidManagedCardsProfileId, corporateCurrencySource, applicationFourSecretKey, corporateAuthenticationTokenDestinationDifferentProgramme);

        fundManagedCard(manageCardSource.getLeft(), corporateCurrencySource, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(manageCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_IN_SAME_PROGRAMME"));
    }

    @Test
    public void SendFunds_CorporateMcToMaDifferentProgramme_Conflict() {

        final Pair<String, CreateManagedCardModel> manageCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(applicationFourCorporateManagedAccountProfileId, corporateCurrencySource, applicationFourSecretKey, corporateAuthenticationTokenDestinationDifferentProgramme);

        fundManagedCard(manageCardSource.getLeft(), corporateCurrencySource, 10000L);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(manageCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_IN_SAME_PROGRAMME"));
    }

    @Test
    public void SendFunds_DebitManagedCardToManagedAccount_SourceCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void SendFunds_ManagedAccountToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void SendFunds_ManagedCardToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedCardModel> prepaidManagedCardSource =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountForFunding =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);
        final Pair<String, CreateManagedCardModel> debitManagedCardDestination =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFunding.getLeft(), corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(prepaidManagedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(debitManagedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void SendFunds_TransferToSameManagedAccountLinkedToDebitManagedCard_DestinationCannotBeDebitModeCard() {
        final Pair<String, CreateManagedAccountModel> managedAccountForFundingSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountForFundingDestination =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createDebitManagedCard(corporateDebitManagedCardsProfileId, managedAccountForFundingDestination.getLeft(), corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountForFundingSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_CANNOT_BE_DEBIT_MODE_CARD"));
    }

    @Test
    public void SendFunds_UnknownProfileId_NotFound() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(RandomStringUtils.randomNumeric(18))
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PROFILE_NOT_FOUND"));
    }

    @Test
    public void SendFunds_UnknownSourceInstrumentId_SourceNotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendFunds_UnknownDestinationInstrumentId_DestinationNotFound() {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendFunds_NoProfileId_Conflict(final String profileId) {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(profileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendFunds_NoSourceInstrumentId_BadRequest(final String instrumentId) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(instrumentId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendFunds_NoDestinationInstrumentId_BadRequest(final String instrumentId) {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(instrumentId, MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_UnknownSourceInstrumentType_BadRequest() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), UNKNOWN))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_UnknownDestinationInstrumentType_BadRequest() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), UNKNOWN))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_CrossIdentityAuthenticationCheck_SourceNotFound() {
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendFunds_CrossIdentityCorporateToConsumer_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenSource);

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumerAuthenticationTokenSource);

        fundManagedAccount(corporateManagedAccount.getLeft(), Currency.EUR.name(), depositAmount);


        assertSuccessfulSend(Pair.of(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(consumerManagedAccount.getLeft(), MANAGED_ACCOUNTS),
                Currency.EUR.name(), sendAmount, corporateAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(corporateManagedAccount.getLeft(), corporateAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(consumerManagedAccount.getLeft(), consumerAuthenticationTokenSource, sendAmount.intValue());
    }

    @Test
    public void SendFunds_CrossIdentityConsumerToCorporate_Success() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumerAuthenticationTokenSource);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporateAuthenticationTokenSource);


        fundManagedAccount(consumerManagedAccount.getLeft(), Currency.EUR.name(), depositAmount);

        assertSuccessfulSend(Pair.of(consumerManagedAccount.getLeft(), MANAGED_ACCOUNTS),
                Pair.of(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS),
                Currency.EUR.name(), sendAmount, consumerAuthenticationTokenSource);

        final int sourceBalance =
                (int) (depositAmount - sendAmount -
                        TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(consumerManagedAccount.getLeft(), consumerAuthenticationTokenSource, sourceBalance);
        assertManagedAccountBalance(corporateManagedAccount.getLeft(), corporateAuthenticationTokenSource, sendAmount.intValue());
    }

    @Test
    public void SendFunds_NoFunds_FundsInsufficient() {
        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendFunds_NotEnoughFunds_FundsInsufficient() {
        final Long depositAmount = 600L;
        final Long sendAmount = 550L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendFunds_DestinationManagedAccountLimitReachedAndNotEnoughFundsNoFundMovements_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumer.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount.getLeft(),
                Currency.EUR.name(),
                3000000L,
                secretKey, corporate.getRight());

        final SendFundsModel.Builder sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 1000000L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(consumerManagedAccount.getLeft(), MANAGED_ACCOUNTS));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 700000L));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> expectedSourceManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> expectedDestinationManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(consumerManagedAccount.getLeft()).get(0);

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 2000000L));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> actualSourceManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> actualDestinationManagedAccountBalances =
                ManagedAccountsDatabaseHelper.getManagedAccount(consumerManagedAccount.getLeft()).get(0);


        assertEquals(expectedSourceManagedAccountBalances.get("available"), actualSourceManagedAccountBalances.get("available"));
        assertEquals(expectedSourceManagedAccountBalances.get("pending"), actualSourceManagedAccountBalances.get("pending"));
        assertEquals(expectedDestinationManagedAccountBalances.get("available"), actualDestinationManagedAccountBalances.get("available"));
        assertEquals(expectedDestinationManagedAccountBalances.get("pending"), actualDestinationManagedAccountBalances.get("pending"));
    }

    @Test
    public void SendFunds_DestinationPrepaidCardLimitReachedAndNotEnoughFundsNoFundMovements_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedCardModel> consumerManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, Currency.EUR.name(), consumer.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount.getLeft(),
                Currency.EUR.name(),
                3000000L,
                secretKey, corporate.getRight());

        final SendFundsModel.Builder sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 1000000L))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(consumerManagedCard.getLeft(), MANAGED_CARDS));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 700000L));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> expectedSourceAccountBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> expectedDestinationCardBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(consumerManagedCard.getLeft()).get(0);

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), 2000000L));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> actualSourceAccountBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> actualDestinationCardBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(consumerManagedCard.getLeft()).get(0);

        assertEquals(expectedSourceAccountBalance.get("available"), actualSourceAccountBalance.get("available"));
        assertEquals(expectedSourceAccountBalance.get("pending"), actualSourceAccountBalance.get("pending"));
        assertEquals(expectedDestinationCardBalance.get("available"), actualDestinationCardBalance.get("available"));
        assertEquals(expectedDestinationCardBalance.get("pending"), actualDestinationCardBalance.get("pending"));
    }

    @Test
    public void SendFunds_ManagedAccountFundsInAvailableBalanceNotPending_Success() throws SQLException {

        final Long depositAmount = 2000000L;
        final Long depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long sendFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MC_SEND_FEE).getAmount();
        final Long exceedLimitSendAmount = 3000000L;
        final Long validSendAmount = 1000L;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedAccountModel> consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumer.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount.getLeft(),
                Currency.EUR.name(),
                depositAmount,
                secretKey, corporate.getRight());

        final SendFundsModel.Builder sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), exceedLimitSendAmount))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(consumerManagedAccount.getLeft(), MANAGED_ACCOUNTS));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> preTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> preTransferDestinationBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(consumerManagedAccount.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee), preTransferSourceBalance.get("available"));
        assertEquals("0", preTransferSourceBalance.get("pending"));

        assertEquals("0", preTransferDestinationBalance.get("available"));
        assertEquals("0", preTransferDestinationBalance.get("pending"));

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), validSendAmount));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> postTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> postTransferDestinationBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(consumerManagedAccount.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee - validSendAmount - sendFee), postTransferSourceBalance.get("available"));
        assertEquals("0", postTransferSourceBalance.get("pending"));

        assertEquals(String.valueOf(validSendAmount), postTransferDestinationBalance.get("available"));
        assertEquals("0", postTransferDestinationBalance.get("pending"));
    }

    @Test
    public void SendFunds_PrepaidManagedCardFundsInAvailableBalanceNotPending_Success() throws SQLException {

        final Long depositAmount = 2000000L;
        final Long depositFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.DEPOSIT_FEE).getAmount();
        final Long sendFee = TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MC_SEND_FEE).getAmount();
        final Long exceedLimitSendAmount = 3000000L;
        final Long validSendAmount = 1000L;

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency(Currency.EUR.name()).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);

        final Pair<String, CreateManagedAccountModel> corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight());

        final Pair<String, CreateManagedCardModel> consumerManagedCard =
                createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId, Currency.EUR.name(), consumer.getRight());

        setCorporateLimits(corporate.getLeft());

        simulateManagedAccountDepositAndCheckBalance(corporateManagedAccount.getLeft(),
                Currency.EUR.name(),
                depositAmount,
                secretKey, corporate.getRight());

        final SendFundsModel.Builder sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), exceedLimitSendAmount))
                        .setSource(new ManagedInstrumentTypeId(corporateManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(consumerManagedCard.getLeft(), MANAGED_CARDS));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));

        final Map<String, String> preTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> preTransferDestinationBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(consumerManagedCard.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee), preTransferSourceBalance.get("available"));
        assertEquals("0", preTransferSourceBalance.get("pending"));

        assertEquals("0", preTransferDestinationBalance.get("available"));
        assertEquals("0", preTransferDestinationBalance.get("pending"));

        sendFundsModel.setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), validSendAmount));

        SendsService.sendFunds(sendFundsModel.build(), secretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final Map<String, String> postTransferSourceBalance =
                ManagedAccountsDatabaseHelper.getManagedAccount(corporateManagedAccount.getLeft()).get(0);

        final Map<String, String> postTransferDestinationBalance =
                ManagedCardsDatabaseHelper.getPrepaidManagedCard(consumerManagedCard.getLeft()).get(0);

        assertEquals(String.valueOf(depositAmount - depositFee - validSendAmount - sendFee), postTransferSourceBalance.get("available"));
        assertEquals("0", postTransferSourceBalance.get("pending"));

        assertEquals(String.valueOf(validSendAmount), postTransferDestinationBalance.get("available"));
        assertEquals("0", postTransferDestinationBalance.get("pending"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -300L})
    public void SendFunds_InvalidAmount_AmountInvalid(final long sendAmount) {
        final Long depositAmount = 10000L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), consumerCurrencySource, depositAmount);
        fundManagedAccount(managedAccountDestination.getLeft(), consumerCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));

        assertManagedAccountBalance(managedAccountSource.getLeft(), consumerAuthenticationTokenSource, depositAmount.intValue());
        assertManagedAccountBalance(managedAccountDestination.getLeft(), consumerAuthenticationTokenDestination, depositAmount.intValue());
    }

    @Test
    public void SendFunds_SendToSelf_Conflict() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);

        fundManagedAccount(managedAccount.getLeft(), consumerCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_AND_DESTINATION_MUST_BE_DIFFERENT"));
    }

    @Test
    public void SendFunds_OtherCurrency_CurrencyMismatch() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), consumerCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency
                                .getRandomWithExcludedCurrency(Currency.valueOf(consumerCurrencySource)).name(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void SendFunds_InvalidCurrency_BadRequest() {

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount("ABCD", 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_SourceManagedCardRemoved_SourceInstrumentDestroyed() {
        final Long transferAmount = 500L;
        final List<Pair<String, CreateManagedCardModel>> managedCards =
                createManagedCards(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCards.get(0).getLeft(), corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCards.get(0).getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCards.get(1).getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendFunds_SourceManagedCardBlocked_SourceInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final Pair<String, CreateManagedCardModel> managedCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardSource.getLeft(), corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendFunds_DestinationManagedCardRemoved_DestinationInstrumentDestroyed() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final Pair<String, CreateManagedCardModel> managedCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedCardsHelper.removeManagedCard(secretKey, managedCardDestination.getLeft(), corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendFunds_DestinationManagedCardBlocked_DestinationInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;

        final Pair<String, CreateManagedCardModel> managedCardSource = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);


        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedCardsHelper.blockManagedCard(secretKey, managedCardDestination.getLeft(), corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendFunds_SourceManagedAccountRemoved_SourceInstrumentDestroyed() {
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);

        ManagedAccountsHelper.removeManagedAccount(managedAccountSource.getLeft(), secretKey, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendFunds_SourceManagedAccountBlocked_SourceInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedAccountModel> managedAccountSource =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedCardModel> managedCardDestination =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedAccount(managedAccountSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedAccountsHelper.blockManagedAccount(managedAccountSource.getLeft(), secretKey, corporateAuthenticationTokenSource);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardDestination.getLeft(), MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendFunds_DestinationManagedAccountRemoved_DestinationInstrumentDestroyed() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedAccountsHelper.removeManagedAccount(managedAccountDestination.getLeft(), secretKey, corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendFunds_DestinationManagedAccountBlocked_DestinationInstrumentBlocked() {
        final Long depositAmount = 10000L;
        final Long transferAmount = 500L;
        final Pair<String, CreateManagedCardModel> managedCardSource =
                createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrencySource, corporateAuthenticationTokenSource);
        final Pair<String, CreateManagedAccountModel> managedAccountDestination =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenDestination);

        fundManagedCard(managedCardSource.getLeft(), corporateCurrencySource, depositAmount);

        ManagedAccountsHelper.blockManagedAccount(managedAccountDestination.getLeft(), secretKey, corporateAuthenticationTokenDestination);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, transferAmount))
                        .setSource(new ManagedInstrumentTypeId(managedCardSource.getLeft(), MANAGED_CARDS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendFunds_InvalidApiKey_Unauthorised() {
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, "abc", consumerAuthenticationTokenSource, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendFunds_NoApiKey_BadRequest() {
        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, "", consumerAuthenticationTokenSource, Optional.empty())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendFunds_DifferentInnovatorApiKey_Forbidden() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumerAuthenticationTokenSource, Optional.empty())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendFunds_RootUserLoggedOut_Unauthorised() {
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(RandomStringUtils.randomNumeric(18), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, token, Optional.empty())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendFunds_BackofficeCorporateImpersonator_Forbidden() {

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();


        SendsService.sendFunds(sendFundsModel, secretKey, getBackofficeImpersonateToken(corporateIdSource, IdentityType.CORPORATE), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendFunds_BackofficeConsumerImpersonator_Forbidden() {

        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(consumerManagedAccountProfileId, consumerCurrencySource, consumerAuthenticationTokenSource, 2);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(consumerCurrencySource, 10L))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();


        SendsService.sendFunds(sendFundsModel, secretKey, getBackofficeImpersonateToken(consumerIdSource, IdentityType.CONSUMER), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendFunds_KycLevel1_StepupRequired() {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 5001L;

        final Pair<String, CreateManagedAccountModel> managedAccount1 = createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumer.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccount2 = createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumerAuthenticationTokenDestination);

        fundManagedAccount(managedAccount1.getLeft(), Currency.EUR.name(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccount1.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccount2.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("IDENTITY_KYC_LEVEL_STEPUP_REQUIRED"));

        assertManagedAccountBalance(managedAccount1.getLeft(), consumer.getRight(), depositAmount.intValue());
        assertManagedAccountBalance(managedAccount2.getLeft(), consumerAuthenticationTokenDestination, 0);
    }

    @Test
    public void SendFunds_KycLevel1_SourceStepupRequired() {

        final CreateConsumerModel sourceConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final CreateConsumerModel destinationConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> sourceConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Pair<String, String> destinationConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 5001L;

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency(), sourceConsumer.getRight());

        final Pair<String, CreateManagedAccountModel> destinationManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency(), destinationConsumer.getRight());

        fundManagedAccount(sourceManagedAccount.getLeft(), sourceConsumerModel.getBaseCurrency(), depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sourceConsumerModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("IDENTITY_KYC_LEVEL_STEPUP_REQUIRED"));

        assertManagedAccountBalance(sourceManagedAccount.getLeft(), sourceConsumer.getRight(), depositAmount.intValue());
        assertManagedAccountBalance(destinationManagedAccount.getLeft(), destinationConsumer.getRight(), 0);
    }

    @Test
    public void SendFunds_KycLevel1DestinationLimitCheck_Success() {

        final CreateConsumerModel sourceConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final CreateConsumerModel destinationConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> sourceConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Pair<String, String> destinationConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 20000L;
        final Long sendAmount = 15001L;

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency(), sourceConsumer.getRight());

        final Pair<String, CreateManagedAccountModel> destinationManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency(), destinationConsumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(sourceManagedAccount.getLeft(), secretKey, sourceConsumer.getRight());

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(sourceManagedAccount.getLeft(),
                sourceConsumerModel.getBaseCurrency(),
                depositAmount,
                secretKey,
                sourceConsumer.getRight());

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sourceConsumerModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        final int transactionAmount = (int) (sendAmount +
                TestHelper.getFees(sourceConsumerModel.getBaseCurrency()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());
        final int sourceBalance = preSendBalance - transactionAmount;

        ManagedAccountsService.getManagedAccount(secretKey, sourceManagedAccount.getLeft(), sourceConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(sourceBalance))
                .body("balances.actualBalance", equalTo(sourceBalance));

        ManagedAccountsService.getManagedAccount(secretKey, destinationManagedAccount.getLeft(), destinationConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(sendAmount.intValue()));
    }

    @Test
    public void SendFunds_KycLevel1LimitCountCheck_Success() {

        final CreateConsumerModel sourceConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final CreateConsumerModel destinationConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> sourceConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(sourceConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Pair<String, String> destinationConsumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(destinationConsumerModel, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 15000L;
        final Long sendAmount = 5000L;

        final Pair<String, CreateManagedAccountModel> sourceManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, sourceConsumerModel.getBaseCurrency(), sourceConsumer.getRight());

        ManagedAccountsHelper.assignManagedAccountIban(sourceManagedAccount.getLeft(), secretKey, sourceConsumer.getRight());

        final Pair<String, CreateManagedAccountModel> destinationManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, destinationConsumerModel.getBaseCurrency(), destinationConsumer.getRight());

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(sourceManagedAccount.getLeft(),
                sourceConsumerModel.getBaseCurrency(),
                depositAmount,
                secretKey,
                sourceConsumer.getRight());

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(sourceConsumerModel.getBaseCurrency(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(sourceManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(destinationManagedAccount.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("COMPLETED"));

        SendsService.sendFunds(sendFundsModel, secretKey, sourceConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("PENDING"));

        final int transactionAmount = (int) (sendAmount +
                TestHelper.getFees(sourceConsumerModel.getBaseCurrency()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());
        final int sourceBalance = (preSendBalance - transactionAmount - transactionAmount);

        ManagedAccountsService.getManagedAccount(secretKey, sourceManagedAccount.getLeft(), sourceConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(sourceBalance))
                .body("balances.actualBalance", equalTo(sourceBalance));

        ManagedAccountsService.getManagedAccount(secretKey, destinationManagedAccount.getLeft(), destinationConsumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(sendAmount.intValue()))
                .body("balances.actualBalance", equalTo(sendAmount.intValue() * 2));
    }

    @Test
    public void SendFunds_KycLevel1StepupDone_Success() {

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, KycLevel.KYC_LEVEL_1, secretKey);

        final Long depositAmount = 10000L;
        final Long sendAmount = 5001L;

        final Pair<String, CreateManagedAccountModel> managedAccountSource = createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumer.getRight());
        final Pair<String, CreateManagedAccountModel> managedAccountDestination = createManagedAccount(consumerManagedAccountProfileId, Currency.EUR.name(), consumerAuthenticationTokenDestination);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountSource.getLeft(), secretKey, consumer.getRight());

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountSource.getLeft(),
                Currency.EUR.name(),
                depositAmount,
                secretKey,
                consumer.getRight());

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountSource.getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccountDestination.getLeft(), MANAGED_ACCOUNTS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("IDENTITY_KYC_LEVEL_STEPUP_REQUIRED"));

        ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_2, secretKey, consumer.getRight());
        ConsumersHelper.verifyKyc(secretKey, consumer.getLeft());

        SendsService.sendFunds(sendFundsModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final int sourceBalance =
                (int) (preSendBalance - sendAmount -
                        TestHelper.getFees(Currency.EUR.name()).get(FeeType.MA_TO_MA_SEND_FEE).getAmount());

        assertManagedAccountBalance(managedAccountSource.getLeft(), consumer.getRight(), sourceBalance);
        assertManagedAccountBalance(managedAccountDestination.getLeft(), consumerAuthenticationTokenDestination, sendAmount.intValue());
    }

    @Test
    public void SendFunds_SendsOnSameIdentity_Conflict() {
        final Long depositAmount = 10000L;
        final Long sendAmount = 500L;
        final List<Pair<String, CreateManagedAccountModel>> managedAccounts =
                createManagedAccounts(corporateManagedAccountProfileId, corporateCurrencySource, corporateAuthenticationTokenSource, 2);

        fundManagedAccount(managedAccounts.get(0).getLeft(), corporateCurrencySource, depositAmount);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(corporateCurrencySource, sendAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccounts.get(0).getLeft(), MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedAccounts.get(1).getLeft(), MANAGED_ACCOUNTS))
                        .build();
        SendsService.sendFunds(sendFundsModel, secretKey, corporateAuthenticationTokenSource, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DESTINATION_BELONGS_TO_SAME_IDENTITY"));
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
                        .setDescription(RandomStringUtils.randomAlphabetic(10))
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
                .body("creationTimestamp", notNullValue())
                .body("description", notNullValue());
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance) {
        assertManagedAccountBalance(managedAccountId, secretKey, token, balance);
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String secretKey, final String token, final int balance) {
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


    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerIdSource = authenticatedConsumer.getLeft();
        consumerNameSource = String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname());
        consumerAuthenticationTokenSource = authenticatedConsumer.getRight();
        consumerCurrencySource = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerIdSource);
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateIdSource = authenticatedCorporate.getLeft();
        corporateNameSource = createCorporateModel.getCompany().getName();
        corporateAuthenticationTokenSource = authenticatedCorporate.getRight();
        corporateCurrencySource = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateIdSource);
    }

    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerIdDestination = authenticatedConsumer.getLeft();
        consumerNameDestination = String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname());
        consumerAuthenticationTokenDestination = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerIdDestination);
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateIdDestination = authenticatedCorporate.getLeft();
        corporateNameDestination = createCorporateModel.getCompany().getName();
        corporateAuthenticationTokenDestination = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateIdDestination);
    }


    private static void corporateSetupDestinationDifferentProgramme() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(applicationFourCorporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, applicationFourSecretKey);
        String corporateIdDestinationDifferentProgramme = authenticatedCorporate.getLeft();
        corporateAuthenticationTokenDestinationDifferentProgramme = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(applicationFourSecretKey,
                corporateIdDestinationDifferentProgramme);
    }

    private void setCorporateLimits(final String corporateId) {

        final String adminToken = AdminService.loginAdmin();
        final String token = AdminService.impersonateTenant(innovatorId, adminToken);

        final SetLimitModel corporateVelocityLimits = SetLimitModel.builder()
                .setLimitType("CORPORATE_VELOCITY_AGGREGATE")
                .setBaseLimit(new CurrencyAmount(Currency.EUR.name(), 500000000L))
                .setCurrencyLimit(Arrays.asList(new CurrencyAmount(Currency.USD.name(), 10000000L),
                        new CurrencyAmount(Currency.GBP.name(), 10000000L)))
                .build();

        AdminService
                .setCorporateLimit(corporateVelocityLimits, token, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final SetLimitModel corporateFundsLimit = SetLimitModel.builder()
                .setLimitType("CORPORATE_FUNDS_SOURCE_AGGREGATE")
                .setBaseLimit(new CurrencyAmount(Currency.EUR.name(), 500000000L))
                .setCurrencyLimit(Arrays.asList(new CurrencyAmount(Currency.USD.name(), 10000000L),
                        new CurrencyAmount(Currency.GBP.name(), 10000000L)))
                .build();

        AdminService
                .setCorporateLimit(corporateFundsLimit, token, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

}
