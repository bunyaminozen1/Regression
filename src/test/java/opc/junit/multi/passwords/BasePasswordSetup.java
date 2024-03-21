package opc.junit.multi.passwords;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.tags.MultiTags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.PASSWORDS)
public class BasePasswordSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel passcodeApp;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;

    protected static String passcodeAppProgrammeId;
    protected static String passcodeAppCorporateProfileId;
    protected static String passcodeAppConsumerProfileId;
    protected static String passcodeAppCorporateManagedAccountProfileId;
    protected static String passcodeAppConsumerManagedAccountProfileId;
    protected static String passcodeAppCorporatePrepaidManagedCardsProfileId;
    protected static String passcodeAppConsumerPrepaidManagedCardsProfileId;
    protected static String passcodeAppOutgoingWireTransfersProfileId;
    protected static String passcodeAppSecretKey;
    protected static String passcodeAppSharedKey;

    @BeforeAll
    public static void GlobalSetup(){

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PASSCODE_APP);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        secretKey = applicationOne.getSecretKey();

        passcodeAppProgrammeId = passcodeApp.getProgrammeId();
        passcodeAppCorporateProfileId = passcodeApp.getCorporatesProfileId();
        passcodeAppConsumerProfileId = passcodeApp.getConsumersProfileId();
        passcodeAppCorporatePrepaidManagedCardsProfileId = passcodeApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppConsumerPrepaidManagedCardsProfileId = passcodeApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        passcodeAppCorporateManagedAccountProfileId = passcodeApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        passcodeAppConsumerManagedAccountProfileId = passcodeApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        passcodeAppOutgoingWireTransfersProfileId = passcodeApp.getOwtProfileId();
        passcodeAppSecretKey = passcodeApp.getSecretKey();
        passcodeAppSharedKey = passcodeApp.getSharedKey();

    }

    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }
}
