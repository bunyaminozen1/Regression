package data;

import commons.models.CompanyModel;
import io.restassured.path.json.JsonPath;
import opc.enums.opc.CompanyType;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class CorporateTests extends BaseTestSetup {

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, mode = EnumSource.Mode.EXCLUDE, names = { "NON_PROFIT_ORGANISATION" })
    public void CreateCorporate_Success(final CompanyType companyType){

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i -> {
            final CreateCorporateModel createCorporateModel =
                    CreateCorporateModel.dataCreateCorporateModel(corporateProfileId)
                            .setCompany(CompanyModel
                                    .dataCompanyModel()
                                    .setType(companyType.name())
                                    .build())
                            .build();

            CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty())
                    .then()
                    .statusCode(SC_OK);
        });
    }

    @Test
    public void CreateCorporate_KybInitiated_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.dataCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        CorporatesService.startCorporateKyb(secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void CreateCorporate_KybApproved_Success(){

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.dataCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        CorporatesService.startCorporateKyb(secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK);

        verifyKyb(corporate.getLeft(), secretKey);
    }

    @Test
    public void CreateCorporate_KybRejected_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.dataCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.dataCompanyModel()
                        .setType(CompanyType.LLC.name()).build())
                .build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final Pair<IdentityDetailsModel, SumSubApplicantDataModel> applicantData =
                updateKybData(corporate.getRight(), kybReferenceId, createCorporateModel);

        SumSubHelper.setApplicantInPendingState(applicantData.getLeft(), applicantData.getRight().getId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        final List<String> rejectLabels = Arrays.asList(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name(),
                RejectLabels.COMPANY_NOT_DEFINED_REPRESENTATIVES.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL, Optional.of("Issue with verification."),
                        applicantData.getRight().getId(), applicantData.getLeft().getExternalUserId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"))
                .body("review.reviewResult.reviewRejectType", equalTo("FINAL"));
    }

    @Test
    public void CreateCorporate_KybPending_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.dataCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.dataCompanyModel()
                        .setType(CompanyType.LLC.name()).build())
                .build();
        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final Pair<IdentityDetailsModel, SumSubApplicantDataModel> applicantData =
                updateKybData(corporate.getRight(), kybReferenceId, createCorporateModel);

        SumSubHelper.setApplicantInPendingState(applicantData.getLeft(), applicantData.getRight().getId());

        SumSubHelper.getApplicantData(applicantData.getLeft().getAccessToken(), applicantData.getLeft().getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));
    }

    private Pair<IdentityDetailsModel, SumSubApplicantDataModel> updateKybData(final String corporateAuthenticationToken,
                                                                               final String kybReferenceId,
                                                                               final CreateCorporateModel createCorporateModel) {
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporateAuthenticationToken, kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), createCorporateModel.getCompany().getType()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(CompanyType.valueOf(createCorporateModel.getCompany().getType()), representativeId, representativeExternalUserId, "ubo_director_questionnaire");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), createCorporateModel.getCompany().getType()));

        SumSubHelper.addDirector(CompanyType.valueOf(createCorporateModel.getCompany().getType()), weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        createCorporateModel.getCompany().getType()));

        return Pair.of(weavrIdentity, applicantData);
    }
}
