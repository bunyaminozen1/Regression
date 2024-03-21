package opc.junit.multi.transfers;

import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.TransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.TRANSFERS)
public class BaseTransfersSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;


    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        transfersProfileId = applicationOne.getTransfersProfileId();

    }

    protected static List<Pair<String, CreateManagedAccountModel>> createManagedAccounts(final String managedAccountProfileId,
                                                                                         final String currency,
                                                                                         final String authenticationToken,
                                                                                         final int noOfAccounts){
        return createManagedAccounts(managedAccountProfileId, currency, secretKey, authenticationToken, noOfAccounts);
    }

    protected static List<Pair<String, CreateManagedAccountModel>> createManagedAccounts(final String managedAccountProfileId,
                                                                                         final String currency,
                                                                                         final String secretKey,
                                                                                         final String authenticationToken,
                                                                                         final int noOfAccounts){
        final List<Pair<String, CreateManagedAccountModel>> accounts = new ArrayList<>();

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency,
                secretKey, authenticationToken)));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken){
        return createManagedAccount(managedAccountProfileId, currency, secretKey, authenticationToken);
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String secretKey,
                                                                                  final String authenticationToken){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String corporateManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(corporateManagedAccountId, createManagedAccountModel);
    }

    protected int simulateManagedAccountDepositAndCheckBalance(final String managedAccountId,
                                                               final String currency,
                                                               final Long depositAmount,
                                                               final String secretKey,
                                                               final String authenticationToken){
        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        final int balance = (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());

        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken)
                .then()
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));

        return balance;
    }

    protected int simulateManagedCardDepositAndCheckBalance(final String managedAccountProfileId,
                                                            final String managedCardId,
                                                            final String currency,
                                                            final Long depositAmount,
                                                            final String secretKey,
                                                            final String authenticationToken){

        final String managedAccountForCardFunding =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken).getLeft();

        TestHelper.simulateManagedAccountDeposit(managedAccountForCardFunding, currency, 11000L, secretKey, authenticationToken);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, depositAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountForCardFunding, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken)
                .then()
                .body("balances.availableBalance", equalTo(depositAmount.intValue()))
                .body("balances.actualBalance", equalTo(depositAmount.intValue()));

        return depositAmount.intValue();
    }

    protected static List<Pair<String, CreateManagedCardModel>> createManagedCards(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken,
                                                                                   final int noOfAccounts){
        final List<Pair<String, CreateManagedCardModel>> cards = new ArrayList<>();

        IntStream.range(0, noOfAccounts)
                .forEach(x -> cards.add(createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken)));

        return cards;
    }

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }

    protected static Pair<String, CreateManagedCardModel> createDebitManagedCard(final String managedCardProfileId,
                                                                                 final String managedAccountId,
                                                                                 final String authenticationToken){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }

    protected static Pair<String, CreateManagedAccountModel> createPendingApprovalManagedAccount(final String managedAccountProfileId,
                                                                                                 final String authenticationToken){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                Currency.GBP)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createPendingApprovalManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    protected void fundManagedAccount(final String managedAccountId,
                                      final String currency,
                                      final Long depositAmount,
                                      final String innovatorId){
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected static void fundManagedAccount(final String managedAccountId,
                                      final String currency,
                                      final Long depositAmount){
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected void fundManagedCard(final String managedCardId,
                                   final String currency,
                                   final Long depositAmount){

        AdminHelper.fundManagedCard(innovatorId, managedCardId, currency, depositAmount);
    }
}
