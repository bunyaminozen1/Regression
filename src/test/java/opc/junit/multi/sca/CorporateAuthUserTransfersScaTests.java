package opc.junit.multi.sca;

import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAuthUserTransfersScaTests extends AbstractTransfersScaTests {
    private static String managedCardScaAppId;
    private static String managedCardScaMaAppId;

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

        managedCardScaMaAppId = ManagedCardsHelper.createManagedCard(corporatePrepaidManagedCardProfileIdScaMaApp, CURRENCY,
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
        return managedCardScaMaAppId;
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

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), token);

        return Pair.of(usersModel.getEmail(), user.getRight());
    }

    @Override
    protected Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final String token = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey()).getRight();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, programme.getSecretKey(), token);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, programme.getSecretKey(), user.getRight());

        return Pair.of(usersModel.getEmail(), user.getRight());
    }
}
