package opc.junit.multi.owt;

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
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.multi.ManagedAccountsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
public class BaseOutgoingWireTransfersSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel applicationFour;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static ProgrammeDetailsModel lowValueExemptionApp;
    protected static ProgrammeDetailsModel passcodeApp;
    protected static ProgrammeDetailsModel secondaryScaApp;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String sendsProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String secretKey;
    protected static String programmeId;
    protected static String programmeName;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String adminToken;

    protected static String lowValueExemptionAppProgrammeId;
    protected static String lowValueExemptionAppCorporateProfileId;
    protected static String lowValueExemptionAppConsumerProfileId;
    protected static String lowValueExemptionAppCorporateManagedAccountProfileId;
    protected static String lowValueExemptionAppConsumerManagedAccountProfileId;
    protected static String lowValueExemptionAppCorporatePrepaidManagedCardsProfileId;
    protected static String lowValueExemptionAppConsumerPrepaidManagedCardsProfileId;
    protected static String lowValueExemptionAppSendsProfileId;
    protected static String lowValueExemptionAppOutgoingWireTransfersProfileId;
    protected static String lowValueExemptionAppSecretKey;
    protected static String lowValueExemptionAppSharedKey;

    protected static String passcodeAppProgrammeId;
    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppCorporateManagedAccountProfileId;
    protected static String passcodeAppConsumerManagedAccountProfileId;
    protected static String passcodeAppCorporatePrepaidManagedCardsProfileId;
    protected static String passcodeAppConsumerPrepaidManagedCardsProfileId;
    protected static String passcodeAppSendsProfileId;
    protected static String passcodeAppOutgoingWireTransfersProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;

    @BeforeAll
    public static void GlobalSetup() throws InterruptedException {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        applicationFour = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_FOUR);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        lowValueExemptionApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.LOW_VALUE_EXEMPTION_APP);
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);
        secondaryScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SECONDARY_SCA_APP);

        programmeId = applicationOne.getProgrammeId();
        programmeName = applicationOne.getProgrammeName();

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        sendsProfileId = applicationOne.getSendProfileId();
        outgoingWireTransfersProfileId = applicationOne.getOwtProfileId();
        secretKey = applicationOne.getSecretKey();

        lowValueExemptionAppProgrammeId = lowValueExemptionApp.getProgrammeId();
        lowValueExemptionAppCorporateProfileId = lowValueExemptionApp.getCorporatesProfileId();
        lowValueExemptionAppConsumerProfileId = lowValueExemptionApp.getConsumersProfileId();
        lowValueExemptionAppCorporatePrepaidManagedCardsProfileId = lowValueExemptionApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        lowValueExemptionAppConsumerPrepaidManagedCardsProfileId = lowValueExemptionApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        lowValueExemptionAppCorporateManagedAccountProfileId = lowValueExemptionApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        lowValueExemptionAppConsumerManagedAccountProfileId = lowValueExemptionApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        lowValueExemptionAppSendsProfileId = lowValueExemptionApp.getSendProfileId();
        lowValueExemptionAppOutgoingWireTransfersProfileId = lowValueExemptionApp.getOwtProfileId();
        lowValueExemptionAppSecretKey = lowValueExemptionApp.getSecretKey();
        lowValueExemptionAppSharedKey = lowValueExemptionApp.getSharedKey();

        passcodeAppProgrammeId = passcodeApp.getProgrammeId();
        passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
        passcodeAppCorporatePrepaidManagedCardsProfileId = passcodeApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppConsumerPrepaidManagedCardsProfileId = passcodeApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppCorporateManagedAccountProfileId = passcodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        passcodeAppConsumerManagedAccountProfileId = passcodeApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        passcodeAppSendsProfileId = passcodeApp.getSendProfileId();
        passcodeAppOutgoingWireTransfersProfileId = passcodeApp.getOwtProfileId();
        passcodeAppSecretKey = passcodeApp.getSecretKey();
        passcodeAppSharedKey = passcodeApp.getSharedKey();

        adminToken = AdminService.loginAdmin();
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

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency, authenticationToken, secretKey)));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken){
        return createManagedAccount(managedAccountProfileId, currency, authenticationToken, secretKey);
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken,
                                                                                  final String secretKey){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        ManagedAccountsHelper.assignManagedAccountIban(managedAccountId, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
    }

    protected static int simulateManagedAccountDepositAndCheckBalance(final String managedAccountId,
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
}
