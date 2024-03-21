package fpi.paymentrun;

import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Tag(PluginsTags.PLUGINS)
@Execution(ExecutionMode.CONCURRENT)
public class BasePaymentRunSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();
    @RegisterExtension
    static opc.junit.multi.BaseSetupExtension setupExtensionMulti = new opc.junit.multi.BaseSetupExtension();

    /**
     * pluginsApp - the main one (with SMS channel)
     * pluginsAppTwo - app without SMS channel (for SMS not supported cases)
     * pluginsScaMaApp - app for Otp limits
     */

    protected static ProgrammeDetailsModel pluginsApp;
    protected static ProgrammeDetailsModel multiApplication;

    protected static ProgrammeDetailsModel pluginsAppTwo;
    protected static ProgrammeDetailsModel pluginsScaApp;
    protected static ProgrammeDetailsModel pluginsScaMaApp;

    protected static String programmeId;
    protected static String programmeName;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String zeroBalanceManagedAccountProfileId;
    protected static String linkedManagedAccountProfileId;
    protected static String prepaidCardProfileId;
    protected static String debitCardProfileId;
    protected static String innovatorName;
    protected static String innovatorToken;
    protected static String adminToken;

    protected static String programmeIdAppTwo;
    protected static String corporateProfileIdAppTwo;
    protected static String secretKeyAppTwo;
    protected static String innovatorTokenAppTwo;

    protected static String programmeIdPluginsScaApp;
    protected static String secretKeyPluginsScaApp;
    protected static String sharedKeyPluginsScaApp;
    protected static String corporateProfileIdPluginsScaApp;
    protected static String zeroBalanceManagedAccountProfileIdPluginsScaApp;
    protected static String linkedManagedAccountProfileIdPluginsScaApp;
    protected static String prepaidCardProfileIdPluginsScaApp;
    protected static String debitCardProfileIdPluginsScaApp;
    protected static String innovatorNamePluginsScaApp;
    protected static String innovatorTokenPluginsScaApp;

    protected static String programmeIdScaMa;
    protected static String programmeNameScaMa;
    protected static String secretKeyScaMa;
    protected static String sharedKeyScaMa;
    protected static String corporateProfileIdScaMa;
    protected static String consumerProfileIdScaMa;
    protected static String zeroBalanceManagedAccountProfileIdScaMa;
    protected static String linkedManagedAccountProfileIdScaMa;
    protected static String prepaidCardProfileIdScaMa;
    protected static String debitCardProfileIdScaMa;
    protected static String innovatorNameScaMa;
    protected static String innovatorTokenScaMa;

    protected static String programmeIdMultiApp;
    protected static String secretKeyMultiApp;
    protected static String corporateProfileIdMultiApp;

    @BeforeAll
    public static void GlobalSetup() {
        pluginsApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_APP);
        pluginsAppTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_APP_TWO);
        multiApplication = (ProgrammeDetailsModel) setupExtensionMulti.store.get(opc.enums.opc.InnovatorSetup.APPLICATION_ONE);
        pluginsScaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_SCA_APP);
        pluginsScaMaApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.PLUGINS_SCA_MA_APP);

        programmeId = pluginsApp.getProgrammeId();
        programmeName = pluginsApp.getProgrammeName();
        sharedKey = pluginsApp.getSharedKey();
        secretKey = pluginsApp.getSecretKey();
        corporateProfileId = pluginsApp.getCorporatesProfileId();
        consumerProfileId = pluginsApp.getConsumersProfileId();
        zeroBalanceManagedAccountProfileId = pluginsApp.getZeroBalanceManagedAccountsProfileId();
        linkedManagedAccountProfileId = pluginsApp.getLinkedAccountsProfileId();
        prepaidCardProfileId = pluginsApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileId = pluginsApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        innovatorName = pluginsApp.getInnovatorName();
        innovatorToken = InnovatorHelper.loginInnovator(pluginsApp.getInnovatorEmail(), pluginsApp.getInnovatorPassword());
        adminToken = AdminService.loginAdmin();

        programmeIdAppTwo = pluginsAppTwo.getProgrammeId();
        corporateProfileIdAppTwo = pluginsAppTwo.getCorporatesProfileId();
        secretKeyAppTwo = pluginsAppTwo.getSecretKey();
        innovatorTokenAppTwo = InnovatorHelper.loginInnovator(pluginsAppTwo.getInnovatorEmail(), pluginsAppTwo.getInnovatorPassword());

        programmeIdPluginsScaApp = pluginsScaApp.getProgrammeId();
        secretKeyPluginsScaApp = pluginsScaApp.getSecretKey();
        sharedKeyPluginsScaApp = pluginsScaApp.getSharedKey();
        corporateProfileIdPluginsScaApp = pluginsScaApp.getCorporatesProfileId();
        zeroBalanceManagedAccountProfileIdPluginsScaApp = pluginsScaApp.getZeroBalanceManagedAccountsProfileId();
        linkedManagedAccountProfileIdPluginsScaApp = pluginsScaApp.getLinkedAccountsProfileId();
        prepaidCardProfileIdPluginsScaApp = pluginsScaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileIdPluginsScaApp = pluginsScaApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        innovatorNamePluginsScaApp = pluginsScaApp.getInnovatorName();
        innovatorTokenPluginsScaApp = InnovatorHelper.loginInnovator(pluginsScaApp.getInnovatorEmail(), pluginsScaApp.getInnovatorPassword());

        programmeIdScaMa = pluginsScaMaApp.getProgrammeId();
        programmeNameScaMa = pluginsScaMaApp.getProgrammeName();
        sharedKeyScaMa = pluginsScaMaApp.getSharedKey();
        secretKeyScaMa = pluginsScaMaApp.getSecretKey();
        corporateProfileIdScaMa = pluginsScaMaApp.getCorporatesProfileId();
        consumerProfileIdScaMa = pluginsScaMaApp.getConsumersProfileId();
        zeroBalanceManagedAccountProfileIdScaMa = pluginsScaMaApp.getZeroBalanceManagedAccountsProfileId();
        linkedManagedAccountProfileIdScaMa = pluginsScaMaApp.getLinkedAccountsProfileId();
        prepaidCardProfileIdScaMa = pluginsScaMaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        debitCardProfileIdScaMa = pluginsScaMaApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        innovatorNameScaMa = pluginsScaMaApp.getInnovatorName();
        innovatorTokenScaMa = InnovatorHelper.loginInnovator(pluginsScaMaApp.getInnovatorEmail(), pluginsScaMaApp.getInnovatorPassword());

        programmeIdMultiApp = multiApplication.getProgrammeId();
        secretKeyMultiApp = multiApplication.getSecretKey();
        corporateProfileIdMultiApp = multiApplication.getCorporatesProfileId();
    }

    protected static String getBackofficeImpersonateToken(final String identityId,
                                                          final IdentityType identityType) {
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }


    protected static String getControllerRoleAuthUserToken(final String secretKey,
                                                           final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> controllerUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(controllerUser.getLeft(), secretKey, buyerToken);
        return controllerUser.getRight();
    }

    protected static Triple<String, BuyerAuthorisedUserModel, String> getControllerRoleAuthUser(final String secretKey,
                                                           final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> controllerUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignControllerRole(controllerUser.getLeft(), secretKey, buyerToken);
        return controllerUser;
    }

    protected static String getCreatorRoleAuthUserToken(final String secretKey,
                                                        final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> creatorUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignCreatorRole(creatorUser.getLeft(), secretKey, buyerToken);
        return creatorUser.getRight();
    }

    protected static Triple<String, BuyerAuthorisedUserModel, String> getCreatorRoleAuthUser(final String secretKey,
                                                                                             final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> creatorUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignCreatorRole(creatorUser.getLeft(), secretKey, buyerToken);
        return creatorUser;
    }

    protected static String getMultipleRolesAuthUserToken(final String secretKey,
                                                          final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> creatorUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(creatorUser.getLeft(), secretKey, buyerToken);
        return creatorUser.getRight();
    }

    protected static Triple<String, BuyerAuthorisedUserModel, String> getMultipleRolesAuthUser(final String secretKey,
                                                                                               final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> creatorUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        BuyerAuthorisedUserHelper.assignAllRoles(creatorUser.getLeft(), secretKey, buyerToken);
        return creatorUser;
    }

    protected static String maskDataExceptFirstOneLastThreeChars(final String text) {
        return text.charAt(0) + text.substring(1).replaceAll("\\d(?=\\d{3})", "*");
    }
}
