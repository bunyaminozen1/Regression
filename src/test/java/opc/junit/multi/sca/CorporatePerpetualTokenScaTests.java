package opc.junit.multi.sca;

import opc.enums.opc.CardLevelClassification;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.ImpersonateIdentityModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;

public class CorporatePerpetualTokenScaTests extends AbstractPerpetualTokenScaTests {

    private static String initialPerpetualTokenScaMaApp;
    private static String identityManagedAccountProfileScaMaApp;
    private static String identityManagedCardsProfileScaMaApp;
    private static String identityEmailScaMaApp;

    private static String initialPerpetualTokenScaMcApp;
    private static String identityEmailScaMcApp;
    private static String identityPrepaidManagedCardProfileScaMcApp;
    private static CardLevelClassification identityCardLevelClassificationScaMcApp;
    private static String sendDestinationInitialPerpetualTokenScaMaApp;

    @BeforeAll
    public static void TestSetup() throws SQLException {

        final CreateCorporateModel createCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        Pair<String, String> corporateScaMaApp = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelScaMaApp, secretKeyScaMaApp);
        final ImpersonateIdentityModel impersonateIdentityModelScaMaApp = new ImpersonateIdentityModel(programmeIdScaMaApp, corporateScaMaApp.getLeft());
        initialPerpetualTokenScaMaApp = AdminService.impersonateIdentity(impersonateIdentityModelScaMaApp, adminToken);
        identityManagedAccountProfileScaMaApp = corporateManagedAccountProfileIdScaMaApp;
        identityManagedCardsProfileScaMaApp = corporatePrepaidManagedCardProfileIdScaMaApp;
        identityEmailScaMaApp = createCorporateModelScaMaApp.getRootUser().getEmail();

        final CreateCorporateModel createCorporateModelScaMcApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMcApp).build();
        Pair<String, String> corporateScaMcApp = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModelScaMcApp, secretKeyScaMcApp);
        final ImpersonateIdentityModel impersonateIdentityModelScaMcApp = new ImpersonateIdentityModel(programmeIdScaMcApp, corporateScaMcApp.getLeft());
        initialPerpetualTokenScaMcApp = AdminService.impersonateIdentity(impersonateIdentityModelScaMcApp, adminToken);
        identityEmailScaMcApp = createCorporateModelScaMcApp.getRootUser().getEmail();
        identityPrepaidManagedCardProfileScaMcApp = corporatePrepaidManagedCardProfileIdScaMcApp;
        identityCardLevelClassificationScaMcApp = CardLevelClassification.CONSUMER;

        final CreateCorporateModel createSendDestinationCorporateModelScaMaApp =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdScaMaApp).build();
        final Pair<String, String> corporateSendDestinationScaMaApp = CorporatesHelper.createAuthenticatedVerifiedCorporate(createSendDestinationCorporateModelScaMaApp, secretKeyScaMaApp);
        final ImpersonateIdentityModel impersonateSendDestinationIdentityModelScaMaApp = new ImpersonateIdentityModel(programmeIdScaMaApp, corporateSendDestinationScaMaApp.getLeft());
        sendDestinationInitialPerpetualTokenScaMaApp = AdminService.impersonateIdentity(impersonateSendDestinationIdentityModelScaMaApp, adminToken);
    }

    @Override
    protected String getInitialPerpetualTokenScaMaApp() {
        return initialPerpetualTokenScaMaApp;
    }

    @Override
    protected String getIdentityManagedAccountProfileScaMaApp() {
        return identityManagedAccountProfileScaMaApp;
    }

    @Override
    protected String getIdentityManagedCardsProfileScaMaApp() {
        return identityManagedCardsProfileScaMaApp;
    }

    @Override
    protected String getIdentityEmailScaMaApp() {
        return identityEmailScaMaApp;
    }

    @Override
    protected String getInitialPerpetualTokenScaMcApp() {
        return initialPerpetualTokenScaMcApp;
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
    protected String getSendDestinationInitialPerpetualTokenScaMaApp() {
        return sendDestinationInitialPerpetualTokenScaMaApp;
    }

    @Override
    protected Triple<String, ImpersonateIdentityModel, String> createIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();
        final Pair<String, String> identity = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, programme.getSecretKey());

        final ImpersonateIdentityModel impersonateIdentityModel = new ImpersonateIdentityModel(programme.getProgrammeId(), identity.getLeft());
        final String initialPerpetualToken = AdminService.impersonateIdentity(impersonateIdentityModel, adminToken);

        return Triple.of(createCorporateModel.getRootUser().getEmail(), impersonateIdentityModel, initialPerpetualToken);
    }
}
