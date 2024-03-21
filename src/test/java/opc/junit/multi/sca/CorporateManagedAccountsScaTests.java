package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateManagedAccountsScaTests extends AbstractManagedAccountsScaTests {

    private static String identityTokenScaApp;
    private static String identityManagedAccountProfileScaApp;

    private static String identityTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityEmailScaMaApp;

    private static String identityTokenScaMcApp;
    private static String identityManagedAccountProfileScaMcApp;

    @BeforeAll
    public static void TestSetup() {
        final CreateCorporateModel createCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        identityTokenScaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaApp, secretKeyScaApp).getRight();
        identityManagedAccountProfileScaApp = corporateManagedAccountProfileIdScaApp;

        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        identityTokenScaMaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp).getRight();
        identityManagedAccountProfileScaMaApp = corporateManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = createCorporateModelScaMaApp.getRootUser().getEmail();

        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        identityTokenScaMcApp = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp).getRight();
        identityManagedAccountProfileScaMcApp = corporateManagedAccountProfileIdScaMcApp;

        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMaApp, identityTokenScaMaApp);
        AuthenticationHelper.startAndVerifyStepup(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaMcApp, identityTokenScaMcApp);
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
    protected String getIdentityTokenScaMcApp() {
        return identityTokenScaMcApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMcApp() {
        return identityManagedAccountProfileScaMcApp;
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
