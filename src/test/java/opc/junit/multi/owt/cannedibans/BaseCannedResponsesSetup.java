package opc.junit.multi.owt.cannedibans;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.OWT_CANNED)
public class BaseCannedResponsesSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;

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
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;

    @BeforeAll
    public static void GlobalSetup() throws InterruptedException {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        programmeId = applicationOne.getProgrammeId();

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

    protected static void fundManagedAccount(final String managedAccountId,
                                             final String currency,
                                             final Long depositAmount){
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }
}
