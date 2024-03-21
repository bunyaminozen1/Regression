package opc.junit.admin.corporates;

import opc.enums.opc.CompanyType;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.KybState;
import opc.enums.opc.KycState;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubAuthenticatedUserDataModel;
import opc.services.admin.AdminService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Execution(ExecutionMode.CONCURRENT)
public class StartCorporateUserKycTests extends BaseCorporatesSetup {

    private final static String corporateQuestionnaireId = "paynetics_ubo_director_questionnaire";

    private String corporateId;
    private String corporateUserId;
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
        corporateUserId = user.getLeft();
        corporateUserAuthenticationToken = user.getRight();
    }

    @Test
    public void StartUserKyc_KycNotStarted_Success(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @Test
    public void StartUserKyc_KycInitiated_Success(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String kycReferenceId = AdminHelper.startCorporateUserKyc(corporateId, corporateUserId, adminToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.setRepresentativeInInitiatedState(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @ParameterizedTest
    @EnumSource(value = KybState.class, names = { "NOT_STARTED", "INITIATED", "PENDING_REVIEW", "REJECTED" })
    public void StartUserKyc_NotApproved_KybIdentityNotApproved(final KybState kybState) throws SQLException {

        verifyUserMobile();

        CorporatesDatabaseHelper.updateCorporateKyb(kybState, corporateId);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYB_IDENTITY_NOT_APPROVED"));
    }

    @Test
    public void StartUserKyc_IdentityNotFoundOnSumsub_NotFound() {

        verifyUserMobile();

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void StartUserKyc_EmailNotVerified_EmailNotVerified() {

        final String userId = UsersHelper.createUser(secretKey, corporateAuthenticationToken);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);

        AdminService.startCorporateUserKyc(corporateId, userId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_VERIFIED"));
    }

    @Test
    public void StartUserKyc_MobileNotVerified_MobileNotVerified() {

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NOT_VERIFIED"));
    }

    @Test
    public void StartUserKyc_KycApproved_KycAlreadyApproved(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String kycReferenceId = AdminHelper.startCorporateUserKyc(corporateId, corporateUserId, adminToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.approveRepresentative(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_ALREADY_APPROVED"));
    }

    @Test
    public void StartUserKyc_KycPendingReview_KycPendingReview(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String kycReferenceId = AdminHelper.startCorporateUserKyc(corporateId, corporateUserId, adminToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.setRepresentativeInPendingState(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        SumSubHelper.verifyBeneficiaryState(applicantData.getId(), KycState.PENDING_REVIEW);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_PENDING_REVIEW"));
    }

    @Test
    public void StartUserKyc_KycRejected_KycRejected(){
        verifyUserMobile();

        CorporatesHelper.startKyb(secretKey, corporateAuthenticationToken);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String kycReferenceId = AdminHelper.startCorporateUserKyc(corporateId, corporateUserId, adminToken);

        final IdentityDetailsModel weavrUserIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporateUserAuthenticationToken, kycReferenceId).getParams();

        final SumSubAuthenticatedUserDataModel applicantData =
                SumSubHelper.getApplicantData(weavrUserIdentity.getAccessToken(), weavrUserIdentity.getExternalUserId())
                        .as(SumSubAuthenticatedUserDataModel.class);

        SumSubHelper.rejectRepresentative(CompanyType.valueOf(createCorporateModel.getCompany().getType()),
                applicantData.getId(), weavrUserIdentity.getExternalUserId(), corporateQuestionnaireId);

        SumSubHelper.verifyBeneficiaryState(applicantData.getId(), KycState.REJECTED);

        AdminService.startCorporateUserKyc(corporateId, corporateUserId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYC_REJECTED"));
    }

    @Test
    public void StartUserKyc_OtherIdentityTypeUser_Forbidden(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        AdminService.startCorporateUserKyc(corporateId, user.getLeft(), user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartUserKyc_OtherIdentityTypeRoot_Forbidden(){

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        AdminService.startCorporateUserKyc(corporateId, user.getLeft(), consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartUserKyc_UnknownUserId_NotFound(){

        AdminService.startCorporateUserKyc(corporateId, RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private void verifyUserMobile() {
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, corporateUserAuthenticationToken);
    }
}
