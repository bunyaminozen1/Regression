package opc.junit.multi.users;

import opc.enums.opc.CompanyType;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.KybState;
import opc.enums.opc.KycState;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Execution(ExecutionMode.CONCURRENT)
public class StartCorporateUserKycTests extends BaseUsersSetup {

    private final static String corporateQuestionnaireId = "paynetics_ubo_director_questionnaire";
    private String corporateId;
    private String corporateAuthenticationToken;
    private String corporateUserAuthenticationToken;
    private CreateCorporateModel createCorporateModel;

    @BeforeEach
    public void Setup(){

        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(secretKey, authenticatedCorporate.getRight());
        corporateUserAuthenticationToken = user.getRight();
    }

    @Test
    public void StartUserKyc_KycNotStarted_Success(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @Test
    public void StartUserKyc_KycInitiated_Success(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, corporateUserAuthenticationToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.setRepresentativeInInitiatedState(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @Test
    public void StartUserKyc_ManualEmailVerification_Success(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(usersModel, secretKey, corporateAuthenticationToken);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String token =
                PasswordsService.createPassword(createPasswordModel, userId, secretKey)
                        .jsonPath().getString("token");

        UsersHelper.verifyEmail(secretKey, usersModel.getEmail());
        verifyUserMobile(token);

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        UsersService.startUserKyc(secretKey, token)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = KybState.class, names = { "NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED" })
    public void StartUserKyc_NotApproved_KybIdentityNotApproved(final KybState kybState) throws SQLException {

        verifyUserMobile();

        CorporatesDatabaseHelper.updateCorporateKyb(kybState, corporateId);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYB_IDENTITY_NOT_APPROVED"));
    }

    @Test
    public void StartUserKyc_IdentityNotFoundOnSumsub_NotFound() {

        verifyUserMobile();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartUserKyc_EmailNotVerified_EmailNotVerified() {

        final String userId = UsersHelper.createUser(secretKey, corporateAuthenticationToken);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        final String token =
                PasswordsService.createPassword(createPasswordModel, userId, secretKey)
                        .jsonPath().getString("token");

        UsersService.startUserKyc(secretKey, token)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_VERIFIED"));
    }

    @Test
    public void StartUserKyc_MobileNotVerified_MobileNotVerified() {

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NOT_VERIFIED"));
    }

    @Test
    public void StartUserKyc_TokenRootNotUser_NotFound() {

        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        UsersService.startUserKyc(secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartUserKyc_KycApproved_KycAlreadyApproved(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, corporateUserAuthenticationToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.approveRepresentative(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        SumSubHelper.verifyBeneficiaryState(applicantData.getId(), KycState.APPROVED);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_APPROVED"));
    }

    @Test
    public void StartUserKyc_KycPendingReview_KycPendingReview(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, corporateUserAuthenticationToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.setRepresentativeInPendingState(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        SumSubHelper.verifyBeneficiaryState(applicantData.getId(), KycState.PENDING_REVIEW);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_PENDING_REVIEW"));
    }

    @Test
    public void StartUserKyc_KycRejected_KycRejected(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        final String kycReferenceId = UsersHelper.startUserKyc(secretKey, corporateUserAuthenticationToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.rejectRepresentative(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        SumSubHelper.verifyBeneficiaryState(applicantData.getId(), KycState.REJECTED);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_REJECTED"));
    }

    @Test
    public void StartUserKyc_InvalidApiKey_Unauthorised(){

        UsersService.startUserKyc("abc", corporateUserAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartUserKyc_NoApiKey_BadRequest(){

        UsersService.startUserKyc("", corporateUserAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartUserKyc_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartUserKyc_UserLoggedOut_Unauthorised(){

        AuthenticationHelper.logout(corporateUserAuthenticationToken, secretKey);

        UsersService.startUserKyc(secretKey, corporateUserAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartUserKyc_OtherIdentityTypeUser_Forbidden(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        UsersService.startUserKyc(secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartUserKyc_OtherIdentityTypeRoot_Forbidden(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);

        UsersService.startUserKyc(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void verifyUserMobile() {
        verifyUserMobile(corporateUserAuthenticationToken);
    }

    private void verifyUserMobile(final String token) {
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, token);
    }
}
