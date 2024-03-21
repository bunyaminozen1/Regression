package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAuthUserManagedCardsScaTests extends AbstractManagedCardsScaTests{
    private static String identityTokenScaApp;
    private static String identityPrepaidManagedCardProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityPrepaidManagedCardProfileScaMaApp;
    private static CardLevelClassification identityCardLevelClassificationScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static String identityEmailScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;

    @BeforeAll
    public static void TestSetup() {
        //User for ScaApp
        final CreateCorporateModel createCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        final Pair<String, String> authenticatedCorporateScaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaApp, secretKeyScaApp);

        final UsersModel modelUserScaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaApp, secretKeyScaApp, authenticatedCorporateScaApp.getRight());
        identityTokenScaApp = userScaApp.getRight();
        identityPrepaidManagedCardProfileScaApp = corporatePrepaidManagedCardProfileIdScaApp;

        //User for ScaMaApp
        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        final Pair<String, String> authenticatedCorporateScaMaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp);

        final UsersModel modelUserScaMaApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMaApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaMaApp, secretKeyScaMaApp, authenticatedCorporateScaMaApp.getRight());
        identityTokenScaMaApp = userScaMaApp.getRight();
        identityPrepaidManagedCardProfileScaMaApp = corporatePrepaidManagedCardProfileIdScaMaApp;
        identityCardLevelClassificationScaMaApp = CardLevelClassification.CORPORATE;

        //User for ScaMcApp
        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        final Pair<String, String> authenticatedCorporateScaMcApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp);

        final UsersModel modelUserScaMcApp = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> userScaMcApp = UsersHelper.createEnrolledAuthenticatedUser(modelUserScaMcApp, secretKeyScaMcApp, authenticatedCorporateScaMcApp.getRight());
        identityTokenScaMcApp = userScaMcApp.getRight();
        identityPrepaidManagedCardProfileScaMcApp = corporatePrepaidManagedCardProfileIdScaMcApp;
        identityEmailScaMcApp = modelUserScaMcApp.getEmail();
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CORPORATE;

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identityTokenScaMaApp);
        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identityTokenScaMcApp);
    }

    @Override
    protected String getIdentityTokenScaApp() {
        return identityTokenScaApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaApp() {
        return identityPrepaidManagedCardProfileScaApp;
    }

    @Override
    protected String getIdentityTokenScaMaApp() {
        return identityTokenScaMaApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMaApp() {
        return identityPrepaidManagedCardProfileScaMaApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMaApp() {
        return identityCardLevelClassificationScaMaApp;
    }

    @Override
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityPrepaidManagedCardProfileScaMcApp() {
        return identityPrepaidManagedCardProfileScaMcApp;
    }

    @Override
    protected String getIdentityEmailScaMcApp() {
        return identityEmailScaMcApp;
    }

    @Override
    protected CardLevelClassification getIdentityCardLevelClassificationScaMcApp() {
        return identityCardLevelClassificationScaMcApp;
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), token);

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), programme.getSecretKey(), user.getRight());

        return Pair.of(usersModel.getEmail(), user.getRight());
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), token);

        return Pair.of(usersModel.getEmail(), user.getRight());
    }
}
