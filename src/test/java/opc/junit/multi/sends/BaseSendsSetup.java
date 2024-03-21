package opc.junit.multi.sends;

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
public class BaseSendsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationFour;
    protected static ProgrammeDetailsModel scaSendsApp;
    protected static ProgrammeDetailsModel lowValueExemptionApp;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static ProgrammeDetailsModel secondaryScaApp;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String sendsProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;

    protected static String corporateProfileIdScaSendsApp;
    protected static String consumerProfileIdScaSendsApp;
    protected static String corporateManagedAccountProfileIdScaSendsApp;
    protected static String consumerManagedAccountProfileIdScaSendsApp;
    protected static String corporatePrepaidManagedCardsProfileIdScaSendsApp;
    protected static String consumerPrepaidManagedCardsProfileIdScaSendsApp;
    protected static String corporateDebitManagedCardsProfileIdScaSendsApp;
    protected static String consumerDebitManagedCardsProfileIdScaSendsApp;

    protected static String sendsProfileIdScaSendsApp;
    protected static String secretKeyScaSendsApp;
    protected static String sharedKeyScaSendsApp;
    protected static String programmeIdScaSendsApp;
    protected static String programmeIdAppOne;
    protected static String programmeNameScaSendsApp;

    protected static String innovatorEmailScaApp;
    protected static String innovatorPasswordScaApp;

    protected static String programmeIdLowValueExemptionApp;
    protected static String corporateProfileIdLowValueExemptionApp;
    protected static String consumerProfileIdLowValueExemptionApp;
    protected static String corporateManagedAccountProfileIdLowValueExemptionApp;
    protected static String consumerManagedAccountProfileIdLowValueExemptionApp;
    protected static String corporatePrepaidManagedCardsProfileIdLowValueExemptionApp;
    protected static String consumerPrepaidManagedCardsProfileIdLowValueExemptionApp;
    protected static String corporateDebitManagedCardsProfileIdLowValueExemptionApp;
    protected static String consumerDebitManagedCardsProfileIdLowValueExemptionApp;
    protected static String sendsProfileIdLowValueExemptionApp;
    protected static String secretKeyLowValueExemptionApp;

    protected static String applicationFourProgrammeId;
    protected static String applicationFourCorporateProfileId;
    protected static String applicationFourConsumerProfileId;
    protected static String applicationFourCorporateManagedAccountProfileId;
    protected static String applicationFourConsumerManagedAccountProfileId;
    protected static String applicationFourCorporatePrepaidManagedCardsProfileId;
    protected static String applicationFourConsumerPrepaidManagedCardsProfileId;
    protected static String applicationFourCorporateDebitManagedCardsProfileId;
    protected static String applicationFourConsumerDebitManagedCardsProfileId;
    protected static String applicationFourSendsProfileId;
    protected static String applicationFourSecretKey;
    protected static String outgoingWireTransfersProfileIdSca;


    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationFour = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_FOUR);
        scaSendsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_SENDS_APP);
        lowValueExemptionApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.LOW_VALUE_EXEMPTION_APP);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        secondaryScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SECONDARY_SCA_APP);


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
        sendsProfileId = applicationOne.getSendProfileId();
        transfersProfileId = applicationOne.getTransfersProfileId();

        corporateProfileIdScaSendsApp = scaSendsApp.getCorporatesProfileId();
        consumerProfileIdScaSendsApp = scaSendsApp.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileIdScaSendsApp = scaSendsApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileIdScaSendsApp = scaSendsApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileIdScaSendsApp = scaSendsApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileIdScaSendsApp = scaSendsApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountProfileIdScaSendsApp = scaSendsApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdScaSendsApp = scaSendsApp.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKeyScaSendsApp = scaSendsApp.getSecretKey();
        sharedKeyScaSendsApp = scaSendsApp.getSharedKey();
        sendsProfileIdScaSendsApp = scaSendsApp.getSendProfileId();

        programmeIdScaSendsApp = scaSendsApp.getProgrammeId();
        programmeNameScaSendsApp = scaSendsApp.getProgrammeName();

        programmeIdAppOne = applicationOne.getProgrammeId();

        innovatorEmailScaApp = scaSendsApp.getInnovatorEmail();
        innovatorPasswordScaApp = scaSendsApp.getInnovatorPassword();
        outgoingWireTransfersProfileIdSca = scaSendsApp.getOwtProfileId();

        programmeIdLowValueExemptionApp = lowValueExemptionApp.getProgrammeId();
        corporateProfileIdLowValueExemptionApp = lowValueExemptionApp.getCorporatesProfileId();
        consumerProfileIdLowValueExemptionApp = lowValueExemptionApp.getConsumersProfileId();
        corporateManagedAccountProfileIdLowValueExemptionApp = lowValueExemptionApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileIdLowValueExemptionApp = lowValueExemptionApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporatePrepaidManagedCardsProfileIdLowValueExemptionApp = lowValueExemptionApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileIdLowValueExemptionApp = lowValueExemptionApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileIdLowValueExemptionApp = lowValueExemptionApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileIdLowValueExemptionApp = lowValueExemptionApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        sendsProfileIdLowValueExemptionApp = lowValueExemptionApp.getSendProfileId();
        secretKeyLowValueExemptionApp = lowValueExemptionApp.getSecretKey();

        applicationFourProgrammeId = applicationFour.getProgrammeId();
        applicationFourCorporateProfileId = applicationFour.getCorporatesProfileId();
        applicationFourConsumerProfileId = applicationFour.getConsumersProfileId();
        applicationFourCorporateManagedAccountProfileId = applicationFour.getCorporatePayneticsEeaManagedAccountsProfileId();
        applicationFourConsumerManagedAccountProfileId = applicationFour.getConsumerPayneticsEeaManagedAccountsProfileId();
        applicationFourCorporatePrepaidManagedCardsProfileId = applicationFour.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        applicationFourConsumerPrepaidManagedCardsProfileId = applicationFour.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        applicationFourCorporateDebitManagedCardsProfileId = applicationFour.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        applicationFourConsumerDebitManagedCardsProfileId = applicationFour.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        applicationFourSendsProfileId = applicationFour.getSendProfileId();
        applicationFourSecretKey = applicationFour.getSecretKey();

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

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency, secretKey, authenticationToken)));

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

    protected static int simulateManagedAccountDepositAndCheckBalance(final String managedAccountId,
                                                               final String currency,
                                                               final Long depositAmount,
                                                               final String secretKey,
                                                               final String authenticationToken){
        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        return (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());
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

    protected int simulateManagedCardDepositAndCheckBalance(final String managedAccountProfileId,
                                                            final String managedCardId,
                                                            final String currency,
                                                            final Long depositAmount,
                                                            final String secretKey,
                                                            final String authenticationToken) {

        final String managedAccountForCardFunding =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken).getLeft();

        fundManagedAccount(managedAccountForCardFunding, currency, 20000L);

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

    protected void fundManagedCard(final String managedCardId,
                                   final String currency,
                                   final Long depositAmount){

        AdminHelper.fundManagedCard(innovatorId, managedCardId, currency, depositAmount);
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

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String secretKey,
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
}