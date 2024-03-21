package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAuthyStepupScaTests extends AbstractAuthyStepupScaTests{
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
        final CreateCorporateModel createCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        identityTokenScaApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaApp, secretKeyScaApp).getRight();
        identityManagedAccountProfileScaApp = corporateManagedAccountProfileIdScaApp;

        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        identityTokenScaMaApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp).getRight();
        identityManagedAccountProfileScaMaApp = corporateManagedAccountProfileIdScaMaApp;
        identityEmailScaMaApp = createCorporateModelScaMaApp.getRootUser().getEmail();
        identityManagedCardsProfileScaMaApp = corporatePrepaidManagedCardProfileIdScaMaApp;

        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        identityTokenScaMcApp = CorporatesHelper.createStepupAuthenticatedVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp).getRight();
        identityEmailScaMcApp = createCorporateModelScaMcApp.getRootUser().getEmail();
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
        return Pair.of(createCorporateModel.getRootUser().getEmail(), token);
    }
}