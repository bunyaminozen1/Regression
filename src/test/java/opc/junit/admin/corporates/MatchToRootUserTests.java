package opc.junit.admin.corporates;

import io.restassured.path.json.JsonPath;
import opc.enums.opc.CompanyType;
import opc.enums.sumsub.IdDocType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.GetCorporateInformationModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import commons.models.MobileNumberModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MatchToRootUserTests extends BaseCorporatesSetup {

    private static String adminToken;
    private static Pair<String, String> corporate;
    private static GetCorporateInformationModel getCorporateInformation;


    @BeforeAll
    public static void Setup() {

        final CompanyType companyType = CompanyType.LLC;
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                        corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModelNameWithSpaces()
                        .setMobile(new MobileNumberModel("+356",
                                String.format("79%s", RandomStringUtils.randomNumeric(6))))
                        .build())
                .build();
        corporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(applicationOne.getSharedKey(), corporate.getRight(),
                        kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        final CompanyInfoModel companyInfoModel =
                SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId());
        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA,
                CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                        .build();

        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                addBeneficiaryModel).jsonPath().get("applicantId");

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
                representativeExternalUserId, "ubo_director_questionnaire");

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId());

        SumSubHelper.addAndApproveDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        CorporatesHelper.verifyKyb(secretKey, corporate.getLeft());

        adminToken = AdminService.loginAdmin();
        getCorporateInformation = GetCorporateInformationModel.builder().companyName(companyInfoModel.getCompanyName())
                .build();
    }

    @Test
    public void matchingBeneficiaryIdForRootUserDirectorSuccess() {

        final JsonPath corporatesInfo = AdminService.getCorporatesInformation(getCorporateInformation, adminToken).jsonPath();
        final String directorId = corporatesInfo.getString("corporateWithKyb[0].director[0].id");
        final String rootUserId = corporatesInfo.getString("corporateWithKyb[0].corporate.rootUser.id");

        AdminService.matchToRootUser(adminToken, corporate.getLeft(), directorId)
                .then()
                .statusCode(SC_OK)
                .body("rootUserId", equalTo(rootUserId));

        AdminService.getCorporatesInformation(getCorporateInformation, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("corporateWithKyb[0].director[0].rootUser", equalTo(true));

        assertNamesOfRootUserAndDirector();
    }

    @Test
    public void matchingBeneficiaryIdForRootUserNotDirectorConflict() {
        final JsonPath corporatesInfo = AdminService.getCorporatesInformation(getCorporateInformation, adminToken).jsonPath();
        final String uboId = corporatesInfo.getString("corporateWithKyb[0].ubo[0].id");

        AdminService.matchToRootUser(adminToken, corporate.getLeft(), uboId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BENEFICIARY_NOT_DIRECTOR"));

        AdminService.getCorporatesInformation(getCorporateInformation, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("corporateWithKyb[0].ubo[0].rootUser", equalTo(false));
    }

    @Test
    public void matchingBeneficiaryIdForRootUserUnknownIdConflict() {
        AdminService.matchToRootUser(adminToken, corporate.getLeft(), RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
    }

    @Test
    public void matchingBeneficiaryIdForRootUserUnknownCorporateConflict() {
        AdminService.matchToRootUser(adminToken, corporate.getLeft(), RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
    }

    @Test
    public void matchingBeneficiaryIdForRootUserWithoutCorporateIdConflict() {

        AdminService.matchToRootUser(adminToken, "", RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void matchingBeneficiaryIdForRootUserWithoutBeneficiaryIdConflict() {

        AdminService.matchToRootUser(adminToken, "", RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void matchingBeneficiaryIdForRootUserDifferentCorporateConflict() {
        final Pair<String, String> newCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final JsonPath corporatesInfo = AdminService.getCorporatesInformation(getCorporateInformation, adminToken).jsonPath();
        final String directorId = corporatesInfo.getString("corporateWithKyb[0].director[0].id");

        AdminService.matchToRootUser(adminToken, newCorporate.getLeft(), directorId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
    }

    public void assertNamesOfRootUserAndDirector() {
        JsonPath response = AdminService.getCorporatesInformation(getCorporateInformation, adminToken).jsonPath();
        String rootUserName = response.getString("corporateWithKyb[0].corporate.rootUser.name");
        String rootUserSurname = response.getString("corporateWithKyb[0].corporate.rootUser.surname");
        String directorName = response.getString("corporateWithKyb[0].director[0].firstName");
        String directorSurname = response.getString("corporateWithKyb[0].director[0].lastName");

        assertTrue(rootUserName.trim().equalsIgnoreCase(directorName));
        assertTrue(rootUserSurname.trim().equalsIgnoreCase(directorSurname));
    }

}
