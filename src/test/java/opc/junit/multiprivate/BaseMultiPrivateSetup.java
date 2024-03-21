package opc.junit.multiprivate;

import opc.enums.opc.FeeType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
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
@Tag(MultiTags.MULTI_PRIVATE)
public class BaseMultiPrivateSetup {
    @RegisterExtension
    static BaseSetupExtension multiSetupExtension = new BaseSetupExtension();

    @RegisterExtension
    static fpi.paymentrun.BaseSetupExtension pluginsSetupExtension = new fpi.paymentrun.BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel pluginsApplication;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String outgoingWireTransfersProfileId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorId;
    protected static String innovatorToken;
    protected static String corporatePrepaidManagedCardsProfileId;


    protected static String pluginCorporateProfileId;
    protected static String pluginSecretKey;
    protected static String pluginInnovatorId;
    protected static String pluginInnovatorPassword;
    protected static String pluginInnovatorToken;
    protected static String pluginCorporateLinkedManagedAccountProfileId;
    protected static String pluginZeroBalanceAccountProfileId;
    protected static String pluginProgrammeId;
    protected static String fpiKey;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) multiSetupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        pluginsApplication = (ProgrammeDetailsModel) pluginsSetupExtension.store.get(fpi.paymentrun.InnovatorSetup.PLUGINS_APP);
        nonFpsTenant = (ProgrammeDetailsModel) multiSetupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        secretKey = applicationOne.getSecretKey();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        outgoingWireTransfersProfileId = applicationOne.getOwtProfileId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();
        innovatorId = applicationOne.getInnovatorId();
        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();

        pluginCorporateProfileId = pluginsApplication.getCorporatesProfileId();
        pluginSecretKey = pluginsApplication.getSecretKey();
        pluginCorporateLinkedManagedAccountProfileId = pluginsApplication.getLinkedAccountsProfileId();
        pluginZeroBalanceAccountProfileId = pluginsApplication.getZeroBalanceManagedAccountsProfileId();
        pluginInnovatorPassword = pluginsApplication.getInnovatorPassword();
        pluginInnovatorId = pluginsApplication.getInnovatorId();
        pluginInnovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        pluginProgrammeId = pluginsApplication.getProgrammeId();
        fpiKey = pluginsApplication.getFpiKey();
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
}
