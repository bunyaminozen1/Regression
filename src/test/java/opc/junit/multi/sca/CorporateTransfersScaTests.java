package opc.junit.multi.sca;

import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateTransfersScaTests extends AbstractTransfersScaTests {
    private static String managedCardScaAppId;
    private static String managedCardAccountScaMaAppId;

    @BeforeAll
    public static void TestSetup() {

        final CreateCorporateModel createSendDestinationCorporateModelScaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaApp).build();
        final String sendDestinationIdentityTokenScaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createSendDestinationCorporateModelScaApp, secretKeyScaApp).getRight();

        managedCardScaAppId = ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardProfileIdScaApp, CURRENCY,
                secretKeyScaApp, sendDestinationIdentityTokenScaApp);

        final CreateCorporateModel createSendDestinationCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        final String sendDestinationIdentityTokenScaMaApp = CorporatesHelper.createEnrolledVerifiedCorporate(createSendDestinationCorporateModelScaMaApp, secretKeyScaMaApp).getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, sendDestinationIdentityTokenScaMaApp);

        managedCardAccountScaMaAppId = ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardProfileIdScaMaApp, CURRENCY,
                secretKeyScaMaApp, sendDestinationIdentityTokenScaMaApp);
    }

    @Override
    protected String getDestinationManagedCardScaAppId() {
        return managedCardScaAppId;
    }

    @Override
    protected String getIdentityManagedAccountsProfileScaApp() {
        return scaApp.getCorporatePayneticsEeaManagedAccountsProfileId();
    }

    @Override
    protected String getIdentityManagedAccountsProfileScaMaApp() {
        return scaMaApp.getCorporatePayneticsEeaManagedAccountsProfileId();
    }

    @Override
    protected String getDestinationManagedCardScaMaAppId() {
        return managedCardAccountScaMaAppId;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaApp() {
        return scaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return scaMaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
    }

    @Override
    protected Pair<String, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModelScaMaApp, programme.getSecretKey()).getRight();

        return Pair.of(createCorporateModelScaMaApp.getRootUser().getEmail(), token);
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, programme.getSecretKey(), token);

        return Pair.of(createCorporateModel.getRootUser().getEmail(), token);
    }
}
