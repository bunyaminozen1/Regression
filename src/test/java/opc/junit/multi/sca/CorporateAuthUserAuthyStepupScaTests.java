package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAuthUserAuthyStepupScaTests extends AbstractAuthyStepupScaTests{
    private static String identityTokenScaApp;
    private static String identityManagedAccountProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityEmailScaMaApp;
    private static String identityManagedCardsProfileScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityEmailScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;
    private static String sendDestinationIdentityTokenScaMaApp;

    @BeforeAll
    public static void TestSetup() {
        //User for ScaApp
        final CreateCorporateModel createCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        final Pair<String, String> authenticatedCorporateScaApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaApp, secretKeyScaApp);

        final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaApp = UsersHelper.createAuthyEnrolledUser(modelUserScaApp, secretKeyScaApp, authenticatedCorporateScaApp.getRight());
        identityTokenScaApp = userScaApp.getRight();
        identityManagedAccountProfileScaApp = corporateManagedAccountProfileIdScaApp;

        //User for ScaMaApp
        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        final Pair<String, String> authenticatedCorporateScaMaApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp);

        final UsersModel modelUserScaMaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMaApp = UsersHelper.createAuthyEnrolledUser(modelUserScaMaApp, secretKeyScaMaApp, authenticatedCorporateScaMaApp.getRight());
        identityTokenScaMaApp = userScaMaApp.getRight();
        identityManagedAccountProfileScaMaApp = corporateManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = modelUserScaMaApp.getEmail();
        identityManagedCardsProfileScaMaApp = corporatePrepaidManagedCardProfileIdScaMaApp;

        //User for ScaMcApp
        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        final Pair<String, String> authenticatedCorporateScaMcApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp);

        final UsersModel modelUserScaMcApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMcApp = UsersHelper.createAuthyEnrolledUser(modelUserScaMcApp, secretKeyScaMcApp, authenticatedCorporateScaMcApp.getRight());
        identityTokenScaMcApp = userScaMcApp.getRight();
        identityEmailScaMcApp = modelUserScaMcApp.getEmail();
        identityPrepaidManagedCardProfileScaMcApp = corporatePrepaidManagedCardProfileIdScaMcApp;
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CORPORATE;

        final CreateCorporateModel createSendDestinationCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        sendDestinationIdentityTokenScaMaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createSendDestinationCorporateModelScaMaApp, secretKeyScaMaApp).getRight();

        authyStepUpAccepted(identityTokenScaMaApp, secretKeyScaMaApp);
        authyStepUpAccepted(identityTokenScaMcApp, secretKeyScaMcApp);
    }

    @Override
    protected String getIdentityTokenScaApp() {
        return identityTokenScaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaApp() {
        return identityManagedAccountProfileScaApp;
    }

    @Override
    protected String getIdentityTokenScaMaApp() {
        return identityTokenScaMaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMaApp() {
        return identityManagedAccountProfileScaMaApp;
    }

    @Override
    protected String getIdentityEmailScaMaApp() {
        return identityEmailScaMaApp;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return identityManagedCardsProfileScaMaApp;
    }

    @Override
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityEmailScaMcApp() {
        return identityEmailScaMcApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMcApp() {
        return identityPrepaidManagedCardProfileScaMcApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMcApp() {
        return identityCardLevelClassificationScaMcApp;
    }

    @Override
    protected String getSendDestinationIdentityTokenScaMaApp() {
        return sendDestinationIdentityTokenScaMaApp;
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthyEnrolledUser(usersModel, programme.getSecretKey(), token);

        return Pair.of(usersModel.getEmail(), user.getRight());
    }
}