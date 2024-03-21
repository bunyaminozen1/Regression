package opc.junit.admin.corporates;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.CompanyType;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.innovator.DeactivateIdentityModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IdentityClosureTests extends BaseCorporatesSetup{

    private final static String CORPORATE_QUESTIONNAIRE_ID = "corporate_questionnaire";
    private final static CompanyType DEFAULT_COMPANY_TYPE = CompanyType.PUBLIC_LIMITED_COMPANY;
    private static CreateCorporateModel createCorporateModel;
    private static Pair<String, String> corporate;

    @BeforeEach
    public void createCorporate(){
        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(DEFAULT_COMPANY_TYPE.name())
                        .build())
                .build();
        corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    }

    @Test
    public void CorporateIdentityClosure_KybNotStartedStatus_Success() throws SQLException {

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertSuccessfulCorporateClosure(corporate.getLeft(), createCorporateModel);

        checkCorporateClosureStatus(corporate.getLeft(), "1");
    }

    @Test
    public void CorporateIdentityClosure_KycInitiatedStatus_Success() throws SQLException {

        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                        .getParams();

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertSuccessfulCorporateClosure(corporate.getLeft(), createCorporateModel);

        checkCorporateSumSubStatus(weavrIdentity, createCorporateModel, "init");

        checkCorporateClosureStatus(corporate.getLeft(), "1");
    }

    @Test
    public void CorporateIdentityClosure_KycPendingReviewStatus_Success() throws SQLException {

        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);


        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
                CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(DEFAULT_COMPANY_TYPE, representativeId, createCorporateModel,
                representativeExternalUserId, CORPORATE_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        DEFAULT_COMPANY_TYPE.name()));

        SumSubHelper.addAndApproveDirector(DEFAULT_COMPANY_TYPE, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        DEFAULT_COMPANY_TYPE.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertSuccessfulCorporateClosure(corporate.getLeft(), createCorporateModel);

        checkCorporateSumSubStatus(weavrIdentity, createCorporateModel, "pending");

        checkCorporateClosureStatus(corporate.getLeft(), "1");
    }

    @Test
    public void CorporateIdentityClosure_KycApprovedStatus_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
                CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION);

        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(DEFAULT_COMPANY_TYPE, representativeId, createCorporateModel,
                representativeExternalUserId, CORPORATE_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        DEFAULT_COMPANY_TYPE.name()));

        SumSubHelper.addAndApproveDirector(DEFAULT_COMPANY_TYPE, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        DEFAULT_COMPANY_TYPE.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(),
                applicantData.getId());

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertSuccessfulCorporateClosure(corporate.getLeft(), createCorporateModel);

        checkCorporateSumSubStatus(weavrIdentity, createCorporateModel, "completed");

        checkCorporateClosureStatus(corporate.getLeft(), "1");
    }

    @Test
    public void CorporateIdentityClosure_IdentityNotDeactivated_ConditionsNotMet() throws SQLException {

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        deactivateRootUser(corporate.getLeft());

        assertFailedCorporateClosure(corporate.getLeft());

        checkCorporateClosureStatus(corporate.getLeft(), "0");
    }

    @Test
    public void CorporateIdentityClosure_RootUserNotDeactivated_ConditionsNotMet() throws SQLException {

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        deactivateCorporateIdentity(corporate.getLeft());

        assertFailedCorporateClosure(corporate.getLeft());

        checkCorporateClosureStatus(corporate.getLeft(), "0");
    }

    @Test
    public void CorporateIdentityClosure_AuthorizedUserNotDeactivated_ConditionsNotMet() throws SQLException {

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Create authorized user
        UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertFailedCorporateClosure(corporate.getLeft());

        checkCorporateClosureStatus(corporate.getLeft(), "0");
    }

    @Test
    public void CorporateIdentityClosure_ManagedAccountNotDeactivated_ConditionsNotMet() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Create managed account
        ManagedAccountsHelper.createManagedAccount(managedAccountProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getRight());

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertFailedCorporateClosure(corporate.getLeft());

        checkCorporateClosureStatus(corporate.getLeft(), "0");
    }

    @Test
    public void CorporateIdentityClosure_ManagedCardNotDeactivated_ConditionsNotMet() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Create managed card
        ManagedCardsHelper.createManagedCard(prepaidCardProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getRight());

        deactivateRootUser(corporate.getLeft());

        deactivateCorporateIdentity(corporate.getLeft());

        assertFailedCorporateClosure(corporate.getLeft());

        checkCorporateClosureStatus(corporate.getLeft(), "0");
    }

    @Test
    public void CorporateIdentityClosure_MultipleConditions_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate= CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);

        checkCorporateClosureStatus(corporate.getLeft(), "0");

        // Create authorized user
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());
        //Create managed account
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(managedAccountProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getRight());
        //Create managed card
        final String managedCardId = ManagedCardsHelper.createManagedCard(prepaidCardProfileId, createCorporateModel.getBaseCurrency(), secretKey, corporate.getRight());

        //Try to close identity, failing
        assertFailedCorporateClosure(corporate.getLeft());
        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Deactivate authorized user and try to close, failing
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), user.getLeft(), adminToken);
        assertFailedCorporateClosure(corporate.getLeft());
        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Deactivate Managed Account and try to close, failing
        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, corporate.getRight());
        assertFailedCorporateClosure(corporate.getLeft());
        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Deactivate Managed Card and try to close, failing
        ManagedCardsHelper.removeManagedCard(secretKey, managedCardId, corporate.getRight());
        assertFailedCorporateClosure(corporate.getLeft());
        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Deactivate root user and try to close, failing
        deactivateRootUser(corporate.getLeft());
        assertFailedCorporateClosure(corporate.getLeft());
        checkCorporateClosureStatus(corporate.getLeft(), "0");

        //Deactivate identity and try to close, successful
        deactivateCorporateIdentity(corporate.getLeft());
        assertSuccessfulCorporateClosure(corporate.getLeft(), createCorporateModel);
        checkCorporateClosureStatus(corporate.getLeft(), "1");
    }

    @Test
    public void CorporateIdentityClosure_WithoutToken_Unauthorized() {

        AdminService.corporateIdentityClosure(corporate.getLeft(), "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CorporateIdentityClosure_InvalidToken_Unauthorized() {

        AdminService.corporateIdentityClosure(corporate.getLeft(), RandomStringUtils.randomAlphanumeric(100))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CorporateIdentityClosure_DifferentIdentityToken_Forbidden() {

        AdminService.corporateIdentityClosure(corporate.getLeft(), corporate.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CorporateIdentityClosure_DifferentImpersonatedTenantToken_NotFound() {

        final Pair<String, String> corporate= CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final String impersonateTenantToken = AdminService.impersonateTenant("1", adminToken);

        AdminService.corporateIdentityClosure(corporate.getLeft(), impersonateTenantToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CorporateIdentityClosure_CrossIdentityId_NotFound() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AdminService.corporateIdentityClosure(consumer.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CorporateIdentityClosure_UnknownIdentityId_NotFound() {

        AdminService.corporateIdentityClosure(RandomStringUtils.randomNumeric(18), impersonatedAdminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CorporateIdentityClosure_WithoutIdentityId_MethodNotAllowed() {

        AdminService.corporateIdentityClosure("", impersonatedAdminToken)
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    private void deactivateRootUser(final String corporateId ){
        AdminHelper.deactivateCorporateUser(corporateId, corporateId, adminToken);
    }

    private void deactivateCorporateIdentity(final String corporateId){
        AdminHelper.deactivateCorporate(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), corporateId, impersonatedAdminToken);
    }

    private void assertSuccessfulCorporateClosure(final String corporateId, final CreateCorporateModel createCorporateModel){
        AdminService.corporateIdentityClosure(corporateId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(corporateId))
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("rootUser.active", equalTo(false))
                .body("active", equalTo(false));
    }

    private void assertFailedCorporateClosure(final String corporateId){

        AdminService.corporateIdentityClosure(corporateId, impersonatedAdminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CONDITIONS_NOT_MET"));
    }

    private void checkCorporateClosureStatus(final String corporateId, final String status) throws SQLException {
        final Map<String, String> corporate = CorporatesDatabaseHelper.getCorporate(corporateId).get(0);
        assertEquals(status, corporate.get("permanently_closed"));
        if (status.equals("1")) {
            assertNotNull(status, corporate.get("closed_timestamp"));
        }
    }

    private void checkCorporateSumSubStatus(final IdentityDetailsModel weavrIdentity,
                                            final CreateCorporateModel createCorporateModel,
                                            final String status){
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("info.companyInfo.companyName", equalTo(createCorporateModel.getCompany().getName()))
                .body("email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("review.reviewStatus", equalTo(status))
                .body("deleted", equalTo(true));
    }
}
