package opc.junit.sumsub;

import commons.models.CompanyModel;
import commons.models.MobileNumberModel;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.SumSubApplicantState;
import opc.junit.database.SubscriptionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static opc.enums.opc.CountryCode.getAllEeaCountries;
import static opc.enums.opc.CountryCode.getAllUkCountries;
import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
public class CorporateSubscriptionBasedOnJurisdictionTests extends BaseSumSubSetup{

    @Test
    public void CorporateSubscription_JurisdictionAllowsJustEeaCountries_Successful() throws SQLException {

        setRegistrationCountriesBasedOnJurisdiction(List.of("MT", "DE", "IT", "BE"));
        final CountryCode countryCode = CountryCode.MT;

        final String corporateId = createCorporateAndCompleteApprovalProcess(applicationOne, countryCode);
        checkSubscriptionStatus(corporateId, countryCode.name());
    }

    @Test
    public void CorporateSubscription_JurisdictionAllowsJustUKCountries_Successful() throws SQLException {

        setRegistrationCountriesBasedOnJurisdiction(List.of("GB", "IM", "JE", "GG"));
        final CountryCode countryCode = CountryCode.GB;

        final String corporateId = createCorporateAndCompleteApprovalProcess(applicationOneUk, countryCode);
        checkSubscriptionStatus(corporateId, countryCode.name());
    }


    private CreateCorporateModel createCorporateModel(final String profileId,
                                                      final CompanyType companyType,
                                                      final CountryCode countryCode) {

        return CreateCorporateModel.DefaultCreateCorporateModel(profileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setRegistrationCountry(countryCode.name())
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+356",
                                String.format("79%s", RandomStringUtils.randomNumeric(6))))
                        .build())
                .build();

    }

    private void checkSubscriptionStatus(final String corporateId,
                                         final String subscriberCountry) throws SQLException {

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> SubscriptionsDatabaseHelper.getSubscription(corporateId),
                x -> x.size() > 0 && x.get(0).get("status").equals("ACTIVE"),
                Optional.of(String.format("Subscription for identity with id %s not '%s'", corporateId, "ACTIVE")));

        final Map<String, String> subscriberInfo = SubscriptionsDatabaseHelper.getSubscriber(corporateId).get(0);

        assertEquals("READY_TO_SUBSCRIBE", subscriberInfo.get("status"));
        assertEquals(subscriberCountry, subscriberInfo.get("country"));
    }

    @AfterEach
    public void resetResidentialCountries() {

        final UpdateProgrammeModel updateProgrammeModelEEA = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModelEEA, programmeId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);

        final UpdateProgrammeModel updateProgrammeModelUK = UpdateProgrammeModel.builder()
                .setCountry(getAllUkCountries())
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModelUK, applicationOneUk.getProgrammeId(),
                        AdminService.impersonateTenant(applicationOneUk.getInnovatorId(), adminToken))
                .then()
                .statusCode(SC_OK);
    }

    private void setRegistrationCountriesBasedOnJurisdiction(final List<String> countries){

        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(countries)
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModel, programmeId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);
    }

    private String createCorporateAndCompleteApprovalProcess(final ProgrammeDetailsModel programme, final CountryCode countryCode){
        final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
        final CreateCorporateModel createCorporateModel = createCorporateModel(programme.getCorporatesProfileId(), companyType, countryCode);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, programme.getSecretKey());
        final String kybReferenceId = CorporatesHelper.startKyb(programme.getSecretKey(), corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(programme.getSharedKey(), corporate.getRight(), kybReferenceId)
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

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
                representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.PENDING_REVIEW.name());

        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());

        return corporate.getLeft();
    }
}
