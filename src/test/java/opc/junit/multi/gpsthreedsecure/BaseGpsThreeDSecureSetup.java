package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.tags.MultiTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
public class BaseGpsThreeDSecureSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel threeDSApp;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String threeDSCorporateProfileId;
    protected static String threeDSConsumerProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String programmeId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;

    protected static String innovatorToken;

    @BeforeAll
    public static void GlobalSetup() {
        threeDSApp = (ProgrammeDetailsModel) setupExtension.store.get(
                InnovatorSetup.THREE_DS_APP);

        corporateProfileId = threeDSApp.getCorporatesProfileId();
        consumerProfileId = threeDSApp.getConsumersProfileId();
        threeDSCorporateProfileId = threeDSApp.getThreeDSCorporatesProfileId();
        threeDSConsumerProfileId = threeDSApp.getThreeDSConsumersProfileId();
        secretKey = threeDSApp.getSecretKey();
        sharedKey = threeDSApp.getSharedKey();
        programmeId = threeDSApp.getProgrammeId();

        corporatePrepaidManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = threeDSApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = threeDSApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = threeDSApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileId = threeDSApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = threeDSApp.getConsumerPayneticsEeaManagedAccountsProfileId();

        innovatorToken = InnovatorHelper.loginInnovator(threeDSApp.getInnovatorEmail(),
                threeDSApp.getInnovatorPassword());
    }

    protected static String createManagedAccount(final String managedAccountsProfileId, final String currency, final String token) {
        return ManagedAccountsHelper
                .createManagedAccount(CreateManagedAccountModel
                                .DefaultCreateManagedAccountModel(managedAccountsProfileId, currency).build(),
                        secretKey, token);
    }
}
