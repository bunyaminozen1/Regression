package opc.junit.backoffice.managedaccounts;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.backoffice.BaseSetupExtension;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.MultiBackofficeTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiBackofficeTags.MULTI_BACKOFFICE_INSTRUMENTS)
public class BaseManagedAccountsSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel nonFpsEnabledTenantDetails;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;

    protected static String adminToken;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        nonFpsEnabledTenantDetails = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();

        secretKey = applicationOne.getSecretKey();

        adminToken = AdminService.loginAdmin();
    }

    protected static List<Pair<String, CreateManagedAccountModel>> createManagedAccounts(final String managedAccountProfileId,
                                                                                         final String currency,
                                                                                         final String authenticationToken,
                                                                                         final int noOfAccounts){
        final List<Pair<String, CreateManagedAccountModel>> accounts = new ArrayList<>();

        IntStream.range(0, noOfAccounts).forEach(x ->
                accounts.add(createManagedAccount(managedAccountProfileId, currency, authenticationToken)));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(managedAccountId, createManagedAccountModel);
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

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }
}
