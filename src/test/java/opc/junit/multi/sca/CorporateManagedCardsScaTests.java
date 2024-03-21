package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateManagedCardsScaTests extends AbstractManagedCardsScaTests{
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
        final CreateCorporateModel createCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        identityTokenScaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaApp, secretKeyScaApp).getRight();
        identityPrepaidManagedCardProfileScaApp = corporatePrepaidManagedCardProfileIdScaApp;

        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        identityTokenScaMaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp).getRight();
        identityPrepaidManagedCardProfileScaMaApp = corporatePrepaidManagedCardProfileIdScaMaApp;
        identityCardLevelClassificationScaMaApp = CardLevelClassification.CORPORATE;

        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        identityTokenScaMcApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp).getRight();
        identityPrepaidManagedCardProfileScaMcApp = corporatePrepaidManagedCardProfileIdScaMcApp;
        identityEmailScaMcApp = createCorporateModelScaMcApp.getRootUser().getEmail();
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

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), programme.getSecretKey(), token);

        return Pair.of(createCorporateModel.getRootUser().getEmail(), token);
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        return Pair.of(createCorporateModel.getRootUser().getEmail(), token);
    }
}
