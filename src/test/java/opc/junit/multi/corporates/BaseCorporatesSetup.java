package opc.junit.multi.corporates;

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
@Tag(MultiTags.CORPORATES)
public class BaseCorporatesSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel applicationThree;
    protected static ProgrammeDetailsModel nonFpsTenant;
    protected static ProgrammeDetailsModel applicationOneUk;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String managedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String debitCardProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String programmeId;
    protected static String innovatorId;
    protected static String adminToken;

    @BeforeAll
    public static void GlobalSetup(){
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
        nonFpsTenant = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.NON_FPS_ENABLED_TENANT);
        applicationOneUk = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE_UK);

        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        programmeId = applicationOne.getProgrammeId();
        innovatorId=applicationOne.getInnovatorId();


        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();
        managedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        prepaidCardProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        transfersProfileId = applicationOne.getTransfersProfileId();

        secretKey = applicationOne.getSecretKey();
    }

    protected static String getBackofficeImpersonateToken(final String email, final IdentityType identityType){
        return BackofficeHelper.impersonateIdentity(email, identityType, secretKey);
    }
}
