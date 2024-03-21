package opc.junit.multi.sca;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationFactorsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static opc.junit.helpers.innovator.InnovatorHelper.enableAuthy;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.CoreMatchers.equalTo;


public abstract class AbstractPushEnrolmentTests extends BaseIdentitiesScaSetup {

    private final static EnrolmentChannel CHANNEL = EnrolmentChannel.AUTHY;
    private final static String VERIFICATION_CODE = "123456";

    @BeforeAll
    public static void setup() {
        final String innovatorToken = InnovatorHelper.loginInnovator(scaEnrolApp.getInnovatorEmail(),
                scaEnrolApp.getInnovatorPassword());

        enableAuthy(programmeIdScaEnrolApp, innovatorToken);

        AdminService.setEnrolmentSca(adminToken, programmeIdScaEnrolApp, false, false, true);
    }

    @Test
    public void Enrol_UserScaBeforeStepUp_Forbidden() {

        final String token = createIdentity(scaEnrolApp);

        AuthenticationFactorsService.enrolPush(CHANNEL.name(), secretKeyScaEnrolApp, token)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void Enrol_NonRootUserScaBeforeStepUp_Forbidden() {

        final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKeyScaEnrolApp, getIdentityTokenScaEnrolApp());

        AuthenticationFactorsService.enrolPush(CHANNEL.name(), secretKeyScaEnrolApp, authenticatedUser.getRight())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void Enrol_RootUserScaWithStepUp_Success() {

//        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, getIdentityTokenScaEnrolApp());
        assertSuccessfulEnrolment(getIdentityTokenScaEnrolApp());
    }

    @Test
    public void Enrol_NonRootUserVerifiedIdentity_Success() {

        final Pair<String, String> authenticatedUser = UsersHelper.createEnrolledAuthenticatedUser(UsersModel.DefaultUsersModel().build(), secretKeyScaEnrolApp, getIdentityTokenScaEnrolApp());
        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, authenticatedUser.getRight());
        assertSuccessfulEnrolment(authenticatedUser.getRight());

    }

    @Test
    public void Enrol_SameInnovatorDifferentIdentity_Forbidden() {

//        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, getIdentityTokenScaEnrolApp());

        final String identityToken;

        if (getIdentityType().equals(IdentityType.CORPORATE)) {
            identityToken = CorporatesHelper.createEnrolledCorporate(corporateProfileIdScaEnrolApp, secretKeyScaEnrolApp).getRight();
        } else {
            identityToken = ConsumersHelper.createEnrolledConsumer(consumerProfileIdScaEnrolApp, secretKeyScaEnrolApp).getRight();
        }

        AuthenticationFactorsService.enrolPush(CHANNEL.name(), secretKeyScaEnrolApp, identityToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void Enrol_SameInnovatorCrossIdentity_Forbidden() {

//        AuthenticationHelper.startAndVerifyStepup(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKeyScaEnrolApp, getIdentityTokenScaEnrolApp());

        final String identityToken;

        if (getIdentityType().equals(IdentityType.CORPORATE)) {
            identityToken = ConsumersHelper.createEnrolledConsumer(consumerProfileIdScaEnrolApp, secretKeyScaEnrolApp).getRight();
        } else {
            identityToken = CorporatesHelper.createEnrolledCorporate(corporateProfileIdScaEnrolApp, secretKeyScaEnrolApp).getRight();
        }

        AuthenticationFactorsService.enrolPush(CHANNEL.name(), secretKeyScaEnrolApp, identityToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    private void assertSuccessfulEnrolment(final String authenticationToken) {

        AuthenticationFactorsService.enrolPush(AbstractPushEnrolmentTests.CHANNEL.name(), secretKeyScaEnrolApp,
                        authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    protected abstract String getIdentityTokenScaEnrolApp();

    protected abstract IdentityType getIdentityType();

    protected abstract String createIdentity(final ProgrammeDetailsModel programmeDetailsModel);
}
