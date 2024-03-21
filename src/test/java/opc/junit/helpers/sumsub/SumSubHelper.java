package opc.junit.helpers.sumsub;

import fpi.paymentrun.models.CreateBuyerModel;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import commons.config.ConfigHelper;
import opc.enums.opc.CompanyType;
import opc.enums.opc.ConsumerApplicantLevel;
import opc.enums.opc.KycState;
import opc.enums.sumsub.CallbackReviewStatus;
import opc.enums.sumsub.CallbackType;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.enums.sumsub.SumSubApplicantState;
import opc.junit.database.SumsubDatabaseHelper;
import opc.junit.helpers.TestHelper;
import commons.models.DateOfBirthModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.AddDocumentModel;
import opc.models.sumsub.AddLegalEntityDirectorModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.FixedInfoModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.IdentityParamsResponse;
import opc.models.sumsub.ReviewResultModel;
import opc.models.sumsub.SetApplicantStateModel;
import opc.models.sumsub.SumSubAddressModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.models.sumsub.SumSubCallbackModel;
import opc.models.sumsub.SumSumAgreementModel;
import opc.models.sumsub.questionnaire.custom.QuestionnaireRequest;
import opc.services.secure.SecureService;
import opc.services.sumsub.SumSubCallbackService;
import opc.services.sumsub.SumSubService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static opc.enums.sumsub.IdDocType.GERMAN_DRIVER_LICENCE;
import static opc.enums.sumsub.IdDocType.ID_CARD_BACK;
import static opc.enums.sumsub.IdDocType.ID_CARD_FRONT;
import static opc.enums.sumsub.IdDocType.SELFIE;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL;
import static opc.enums.sumsub.IdDocType.UTILITY_BILL2;
import static org.apache.http.HttpStatus.SC_OK;

public class SumSubHelper {

    public static Map<String, List<Integer>> uploadRequiredDocuments(final List<IdDocType> documents,
                                                                     final String accessToken,
                                                                     final String applicantId) {

        final Map<String, List<Integer>> documentImageMap = new HashMap<>();
        documents.forEach(doc -> documentImageMap.put(doc.getType(), new ArrayList<>()));

        documents.forEach(doc -> {

            final AddDocumentModel addDocumentModel =
                    AddDocumentModel.defaultDocumentModel(doc, applicantId).build();

            final String imageId = TestHelper.ensureAsExpected(15,
                    () -> SumSubService.addIdDocument(accessToken, applicantId, addDocumentModel, doc.getFileName()),
                    SC_OK)
                    .getHeader("X-Image-Id");

            documentImageMap.get(doc.getType()).add(Integer.parseInt(imageId));
        });

        return documentImageMap;
    }

    public static void uploadQuestionnaire(final String accessToken, final String applicantId, final QuestionnaireRequest model) {
        TestHelper.ensureAsExpected(15,
                () -> SumSubService.addQuestionnaire(accessToken, applicantId, model), SC_OK);
    }

    public static QuestionnaireRequest buildPEPQuestionnaire(SumSubApplicantDataModel applicantData) {
        return QuestionnaireRequest.builder()
                .id(applicantData.getId())
                .questionnaires(addQuestionnaire("consumer_level2_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("empstatus", "SELF_EMPLOYED"),
                                addItem("industry", "AUTO_AVIATION"),
                                addItem("receivefundsfrom", "DOMESTIC"),
                                addItem("originoffunds", "DIVIDENDS"),
                                addItem("valueofexpecteddeposits", "lessthan1k"),
                                addItem("expectedvalueofdeposits", "lessthan1k"),
                                addItem("frequencyofexpectedincomingtxs", "lessthan2"),
                                addItem("expectedfrequencyofincomingtxs", "lessthan2"),
                                addItem("otheroriginoffunds", null)
                        ))),
                        addSection("pep", addItems(Map.ofEntries(
                                addItem("pepcategory", "HEADS_OF_STATE"),
                                addItem("nolongerpep", "yes"),
                                addItem("rcacategory", "SPOUSES"),
                                addItem("nolongerrca", "yes")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static QuestionnaireRequest buildDefaultQuestionnaire(SumSubApplicantDataModel applicantData) {
        return QuestionnaireRequest.builder()
                .id(applicantData.getId())
                .questionnaires(addQuestionnaire("consumer_level2_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("empstatus", "SELF_EMPLOYED"),
                                addItem("industry", "AUTO_AVIATION"),
                                addItem("receivefundsfrom", "DOMESTIC"),
                                addItem("originoffunds", "DIVIDENDS"),
                                addItem("valueofexpecteddeposits", "lessthan1k"),
                                addItem("expectedvalueofdeposits", "lessthan1k"),
                                addItem("frequencyofexpectedincomingtxs", "lessthan2"),
                                addItem("expectedfrequencyofincomingtxs", "lessthan2"),
                                addItem("otheroriginoffunds", null)
                        ))),
                        addSection("pep", addItems(Map.ofEntries(
                                addItem("pepcategory", "NOT_A_PEP"),
                                addItem("nolongerpep", "no"),
                                addItem("rcacategory", "NOT_AN_RCA"),
                                addItem("nolongerrca", "no")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static QuestionnaireRequest buildNoOriginOfFundsQuestionnaire(SumSubApplicantDataModel applicantData, String industry) {
        return QuestionnaireRequest.builder()
                .id(applicantData.getId())
                .questionnaires(addQuestionnaire("consumer_level1_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("empstatus", "SELF_EMPLOYED"),
                                addItem("industry", industry),
                                addItem("receivefundsfrom", "DOMESTIC"),
                                addItem("valueofexpecteddeposits", "lessthan1k"),
                                addItem("frequencyofexpectedincomingtxs", "lessthan2"),
                                addItem("otheroriginoffunds", null)
                        ))),
                        addSection("pep", addItems(Map.ofEntries(
                                addItem("pepcategory", "NOT_A_PEP"),
                                addItem("nolongerpep", "no"),
                                addItem("rcacategory", "NOT_AN_RCA"),
                                addItem("nolongerrca", "no")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static QuestionnaireRequest buildKycLevel1DefaultQuestionnaire(SumSubApplicantDataModel applicantData) {
        return QuestionnaireRequest.builder()
                .id(applicantData.getId())
                .questionnaires(addQuestionnaire("consumer_level1_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("empstatus", "SELF_EMPLOYED"),
                                addItem("industry", "AUTO_AVIATION")
                        ))),
                        addSection("pep", addItems(Map.ofEntries(
                                addItem("pepcategory", "NOT_A_PEP"),
                                addItem("nolongerpep", "no"),
                                addItem("rcacategory", "NOT_AN_RCA"),
                                addItem("nolongerrca", "no")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static QuestionnaireRequest buildUboDirectorQuestionnaire(final String applicantId){
        return QuestionnaireRequest.builder()
            .id(applicantId)
            .questionnaires(addQuestionnaire("ubo_director_questionnaire", Map.ofEntries(
                addSection("section1", addItems(Map.ofEntries(
                    addItem("industry", "AUTO_AVIATION")
                ))),
                addSection("pep", addItems(Map.ofEntries(
                    addItem("pepcategory", "NOT_A_PEP"),
                    addItem("rcacategory", "NOT_AN_RCA")
                ))),
                addSection("declarationsection", addItems(Map.ofEntries(
                    addItem("declaration", "true")
                )))
            )))
            .build();
    }

    public static Map<String, List<Integer>> uploadRequiredDocuments(final Map<IdDocType, AddDocumentModel> documentModels,
                                                                     final String accessToken,
                                                                     final String applicantId) {

        final Map<String, List<Integer>> documentImageMap = new HashMap<>();
        documentModels.forEach((key, value) -> documentImageMap.put(key.getType(), new ArrayList<>()));

        documentModels.forEach((key, value) -> {

            final String imageId = TestHelper.ensureAsExpected(15,
                    () -> SumSubService.addIdDocument(accessToken, applicantId, value, key.getFileName()),
                    SC_OK)
                    .getHeader("X-Image-Id");

            documentImageMap.get(key.getType()).add(Integer.parseInt(imageId));
        });

        return documentImageMap;
    }

    public static IdentityParamsResponse getWeavrIdentityDetails(final String sharedKey,
                                                                 final String token,
                                                                 final String referenceId){

        return TestHelper.ensureAsExpected(15, () -> SecureService.getIdentityDetails(sharedKey, token, referenceId), SC_OK)
                .as(IdentityParamsResponse.class);
    }

    public static Response changeApplicantLevel(final String applicantId,
                                               final String level) {
        return TestHelper.ensureAsExpected(15, () -> SumSubService.changeApplicantLevel(applicantId, level), SC_OK);
    }

    public static Response getApplicantData(final String accessToken,
                                            final String externalUserId){

        return TestHelper.ensureAsExpected(15, () -> SumSubService.getApplicantData(accessToken,
                externalUserId), SC_OK);
    }

    public static Response getApplicantData(final String accessToken,
                                            final String externalUserId,
                                            final SumSubApplicantState expectedStatus){

        return TestHelper.ensureAsExpected(5, () -> SumSubService.getApplicantData(accessToken,
                externalUserId), x ->  x.statusCode() == SC_OK && x.jsonPath().getString("review.reviewStatus").equals(expectedStatus.name()),
                Optional.of(String.format("Applicant with external user id %s not in state %s", externalUserId, expectedStatus)));
    }

    public static Response getApplicantDataOne(final String accessToken,
                                               final String applicantId) {

        return TestHelper.ensureAsExpected(15, () -> SumSubService.getApplicantDataOne(accessToken,
                        applicantId), SC_OK);
    }

    public static Response getApplicantLatestCheck(final String applicantId) {

        return TestHelper.ensureAsExpected(15, () -> SumSubService.getApplicantLatestCheck(applicantId), SC_OK);
    }

    public static FixedInfoModel submitApplicantInformation(final String accessToken,
                                                            final SumSubApplicantDataModel applicantData){

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.defaultFixedInfoModel(applicantData.getFixedInfo()).build();

        submitApplicantInformation(fixedInfoModel, accessToken, applicantData.getId());
        return fixedInfoModel;
    }

    public static List<QuestionnaireRequest.Questionnaires> addQuestionnaire(String questionnaire,
                                                                             Map<String, QuestionnaireRequest.Items> sections) {
        return List.of(
                QuestionnaireRequest.Questionnaires.builder()
                        .id(questionnaire)
                        .sections(sections)
                        .build()
        );
    }

    public static QuestionnaireRequest.Items addItems(Map<String, QuestionnaireRequest.Value> items) {
        return QuestionnaireRequest.Items.builder()
                .items(items)
                .build();
    }

    public static Map.Entry<String, QuestionnaireRequest.Items> addSection(String sectionName,
                                                                           QuestionnaireRequest.Items items) {
        return Map.entry(sectionName,items);
    }

    public static Map.Entry<String, QuestionnaireRequest.Value> addItem(String key, String value) {
        return Map.entry(key,
                QuestionnaireRequest.Value.builder()
                        .value(value)
                        .values(List.of())
                        .build());
    }

    public static void submitApplicantInformation(final FixedInfoModel fixedInfoModel,
                                                  final String accessToken,
                                                  final String applicantId){

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.submitApplicantInformation(accessToken, applicantId, fixedInfoModel), SC_OK);
    }
    public static void updateApplicantInformation(final FixedInfoModel fixedInfoModel,
        final String accessToken,
        final String applicantId){

        TestHelper.ensureAsExpected(15, () ->
            SumSubService.updateApplicantInformation(accessToken, applicantId, fixedInfoModel), SC_OK);
    }

    public static void setApplicantInPendingState(final IdentityDetailsModel weavrIdentity,
                                                  final String applicantId){

        TestHelper.ensureAsExpected(30, () ->
                SumSubService.setApplicantInPendingState(weavrIdentity.getAccessToken(), applicantId), SC_OK);

        if (isDevEnvironment())
            sendPendingCallback(weavrIdentity.getExternalUserId());
    }

    public static void setApplicantInPendingState(final String accessToken,
                                                  final String applicantId,
                                                  final String externalUserId){

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.setApplicantInPendingState(accessToken, applicantId), SC_OK);

        if (isDevEnvironment())
            sendPendingCallback(externalUserId);
    }

    public static void setApplicantInApprovedState(final String externalUserId,
                                                   final String applicantId){

        final SetApplicantStateModel setApplicantStateModel = SetApplicantStateModel.builder()
                .setReviewAnswer("GREEN")
                .setRejectLabels(new ArrayList<>())
                .build();

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.setApplicantState(setApplicantStateModel, applicantId), SC_OK);

        if (isDevEnvironment())
            sendApprovedCallback(externalUserId, CallbackType.REVIEWED);
    }

    public static void setWorkflowApplicantInApprovedState(final String externalUserId){
        sendApprovedCallback(externalUserId, CallbackType.WORKFLOW_COMPLETED);
    }

    public static void setApplicantInRejectState(final List<String> rejectLabels,
                                                 final ReviewRejectType reviewRejectType,
                                                 final Optional<String> moderationComment,
                                                 final String applicantId,
                                                 final String externalUserId){

        final SetApplicantStateModel setApplicantStateModel = SetApplicantStateModel.builder()
                .setReviewAnswer("RED")
                .setRejectLabels(rejectLabels)
                .setReviewRejectType(reviewRejectType)
                .setModerationComment(moderationComment.orElse(null))
                .build();

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.setApplicantState(setApplicantStateModel, applicantId), SC_OK);

        if (isDevEnvironment())
            sendRejectedCallback(externalUserId, rejectLabels, reviewRejectType, moderationComment, CallbackType.REVIEWED);
    }

    public static void setWorkflowApplicantInRejectState(final List<String> rejectLabels,
                                                 final ReviewRejectType reviewRejectType,
                                                 final Optional<String> moderationComment,
                                                 final String applicantId,
                                                 final String externalUserId){

        final SetApplicantStateModel setApplicantStateModel = SetApplicantStateModel.builder()
                .setReviewAnswer("RED")
                .setRejectLabels(rejectLabels)
                .setReviewRejectType(reviewRejectType)
                .setModerationComment(moderationComment.orElse(null))
                .build();

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.setApplicantState(setApplicantStateModel, applicantId), SC_OK);

        if (isDevEnvironment())
            sendRejectedCallback(externalUserId, rejectLabels, reviewRejectType, moderationComment, CallbackType.WORKFLOW_COMPLETED);
    }

    public static void submitCompanyInformation(final String accessToken,
                                                final CompanyInfoModel companyInfoModel,
                                                final String applicantId){

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.submitCompanyInformation(accessToken, applicantId, companyInfoModel), SC_OK);
    }

    public static CompanyInfoModel submitCompanyInformation(final String accessToken,
                                                            final SumSubApplicantDataModel applicantData){

        final CompanyInfoModel companyInfoModel =
                CompanyInfoModel.defaultCompanyInfoModel(applicantData.getInfo().getCompanyInfo()).build();

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.submitCompanyInformation(accessToken, applicantData.getId(), companyInfoModel), SC_OK);

        return companyInfoModel;
    }

    public static Response getApplicantRequiredDocStatus(final String accessToken,
                                                         final String applicantId){

        return TestHelper.ensureAsExpected(15, () -> SumSubService.getApplicantRequiredDocStatus(accessToken, applicantId),
                SC_OK);
    }

    public static Response addBeneficiary(final String accessToken,
                                          final String applicantId,
                                          final AddBeneficiaryModel addBeneficiaryModel){

        return TestHelper.ensureAsExpected(15, () -> SumSubService.addBeneficiary(accessToken, applicantId, addBeneficiaryModel),
                SC_OK);
    }

    public static JsonPath addBeneficiaryWithQuestionnaire(final String weavrIdentityAcessToken,
                                                         final String applicantId,
                                                         final String companyName,
                                                         final AddBeneficiaryModel addBeneficiaryModel) {
        final JsonPath beneficiary =
                SumSubHelper.addBeneficiary(weavrIdentityAcessToken, applicantId, addBeneficiaryModel).jsonPath();

        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");
        final String accessToken = SumSubHelper.generateCorporateAccessToken(beneficiaryExternalUserId, companyName);

        uploadQuestionnaire(accessToken, beneficiaryId, buildUboDirectorQuestionnaire(beneficiaryId));

        return beneficiary;
    }
    public static Response getApplicantById(final String accessToken,
                                            final String applicantId){

        return TestHelper.ensureAsExpected(15, () -> SumSubService.getApplicantById(accessToken, applicantId),
                SC_OK);
    }

    private static void sendPendingCallback(final String externalUserId) {
        final SumSubCallbackModel sumSubPendingCallbackModel =
                SumSubCallbackModel.builder()
                        .setType(CallbackType.PENDING)
                        .setReviewStatus(CallbackReviewStatus.PENDING)
                        .setExternalUserId(externalUserId)
                        .setCreatedAt()
                        .build();
        TestHelper.ensureAsExpected(15, () -> SumSubCallbackService.sumsubCallback(sumSubPendingCallbackModel), SC_OK);
    }

    private static void sendApprovedCallback(final String externalUserId, final CallbackType callbackType){
        final SumSubCallbackModel sumSubReviewedCallbackModel =
                SumSubCallbackModel.builder()
                        .setType(callbackType)
                        .setReviewStatus(CallbackReviewStatus.COMPLETED)
                        .setExternalUserId(externalUserId)
                        .setReviewResult(ReviewResultModel.builder().setReviewAnswer("GREEN").build())
                        .setCreatedAt()
                        .build();
        TestHelper.ensureAsExpected(15, () -> SumSubCallbackService.sumsubCallback(sumSubReviewedCallbackModel), SC_OK);
    }

    private static void sendRejectedCallback(final String externalUserId,
                                             final List<String> rejectLabels,
                                             final ReviewRejectType reviewRejectType,
                                             final Optional<String> moderationComment,
                                             final CallbackType callbackType){
        final SumSubCallbackModel sumSubReviewedCallbackModel =
                SumSubCallbackModel.builder()
                        .setType(callbackType)
                        .setReviewStatus(CallbackReviewStatus.COMPLETED)
                        .setExternalUserId(externalUserId)
                        .setCreatedAt()
                        .setReviewResult(ReviewResultModel
                                .builder()
                                .setReviewAnswer("RED")
                                .setReviewRejectType(reviewRejectType)
                                .setRejectLabels(rejectLabels)
                                .setModerationComment(moderationComment.orElse(null))
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15, () -> SumSubCallbackService.sumsubCallback(sumSubReviewedCallbackModel), SC_OK);
    }

    public static String generateCorporateAccessToken(final String externalUserId,
                                                      final String companyType){
        return TestHelper.ensureAsExpected(15,
                () -> SumSubService.generateCorporateAccessToken(externalUserId, CompanyType.getLevelName(companyType)),
                SC_OK)
                .jsonPath()
                .get("token");
    }

    public static String generateConsumerAccessToken(final String externalUserId,
                                                     final ConsumerApplicantLevel applicantLevel){
        return TestHelper.ensureAsExpected(15,
                () -> SumSubService.generateConsumerAccessToken(externalUserId, applicantLevel),
                SC_OK)
                .jsonPath()
                .get("token");
    }

    public static boolean isDevEnvironment(){
        return ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment().equals("dev");
    }

    public static void setRepresentativeInInitiatedState(final CompanyType companyType,
                                                         final String applicantId,
                                                         final CreateCorporateModel createCorporateModel,
                                                         final String externalUserId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());
        final DateOfBirthModel dateOfBirth = createCorporateModel.getRootUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createCorporateModel.getRootUser().getName())
                        .setLastName(createCorporateModel.getRootUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(), createCorporateModel.getRootUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createCorporateModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createCorporateModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createCorporateModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();
        submitApplicantInformation(fixedInfoModel, accessToken, applicantId);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        uploadQuestionnaire(accessToken, applicantId, buildUboDirectorQuestionnaire(applicantId));
    }

    public static void setRepresentativeInInitiatedState(final CompanyType companyType,
                                                         final String applicantId,
                                                         final CreateBuyerModel createBuyerModel,
                                                         final String externalUserId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());
        final DateOfBirthModel dateOfBirth = createBuyerModel.getAdminUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createBuyerModel.getAdminUser().getName())
                        .setLastName(createBuyerModel.getAdminUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createBuyerModel.getAdminUser().getMobile().getCountryCode(), createBuyerModel.getAdminUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createBuyerModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createBuyerModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createBuyerModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createBuyerModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createBuyerModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();
        submitApplicantInformation(fixedInfoModel, accessToken, applicantId);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        uploadQuestionnaire(accessToken, applicantId, buildUboDirectorQuestionnaire(applicantId));
    }

    public static QuestionnaireRequest createSumSubQuestionnaire(final String applicantId,
                                                                 final String questionnaireId,
                                                                 final String industry,
                                                                 final String sourceOfFunds,
                                                                 final String sourceOfFundsOther) {
        switch(questionnaireId) {
            case "corporate_questionnaire":
                return QuestionnaireRequest.builder()
                        .id(applicantId)
                        .questionnaires(addQuestionnaire(questionnaireId, Map.ofEntries(
                                addSection("section1", addItems(Map.ofEntries(
                                        addItem("businessaddressissame", "yes"),
                                        addItem("correspondenceaddressissame", "yes")
                                ))),
                                addSection("section2", addItems(Map.ofEntries(
                                        addItem("website", null),
                                        addItem("lengthofoperation", "lessthan12months"),
                                        addItem("industrycategory", "category_transport"),
                                        addItem("category_transport", industry),
                                        addItem("licencerequired", "no"),
                                        addItem("expectedmonthlyturnover", "between10kand200k")

                                ))),
                                addSection("section3", addItems(Map.ofEntries(
                                        addItem("expectedsourceoffundslocation", "domestic"),
                                        addItem("expectedoriginoffunds", sourceOfFunds),
                                        addItem("expectedoriginoffundsother", sourceOfFundsOther)
                                ))),
                                addSection("section4", addItems(Map.ofEntries(
                                        addItem("expectedcardsmonthly", "2"),
                                        addItem("expectedincomingtransfervolumemonthly", "between15kand150k"),
                                        addItem("expectedincomingtransfersmonthly", "betwen10and25"),
                                        addItem("expectedaverageincomingfundsamount", "between15kand150k"),
                                        addItem("expectedpaymentreceiptcountries", "IT, BE, NE, UK, ML"),
                                        addItem("expectedoutgoingtransfervolumemonthly", "between15kand150k"),
                                        addItem("expectedoutgoingtransfersmonthly", "between10and30"),
                                        addItem("expectedaverageoutgoingfundsamount","between15kand150k")
                                ))),
                                addSection("declarationsection", addItems(Map.ofEntries(
                                        addItem("declaration", "true")
                                )))
                        )))
                        .build();
            default:
                return QuestionnaireRequest.builder()
                        .id(applicantId)
                        .questionnaires(addQuestionnaire(questionnaireId, Map.ofEntries(
                                addSection("section1", addItems(Map.ofEntries(
                                        addItem("declaration", "true"),
                                        addItem("whichIndustryDoYouDe", "PUBLIC_SECTOR_ADMINISTRATION")
                                ))),
                                addSection("pep", addItems(Map.ofEntries(
                                        addItem("pepcategory", "AMBASSADORS"),
                                        addItem("nolongerpep", "yes"),
                                        addItem("rcacategory", "NOT_AN_RCA"),
                                        addItem("nolongerrca", "yes")
                                ))),
                                addSection("declarationsection", addItems(Map.ofEntries(
                                        addItem("declaration", "true")
                                )))
                        )))
                        .build();
        }
    }


    public static void setRepresentativeInInitiatedState(final CompanyType companyType,
                                                         final String applicantId,
                                                         final String externalUserId,
                                                         final String questionnaireId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.randomFixedInfoModel().build();
        submitApplicantInformation(fixedInfoModel, accessToken, applicantId);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        uploadQuestionnaire(accessToken, applicantId, buildUboDirectorQuestionnaire(applicantId));

    }

    public static void setRepresentativeInPendingState(final CompanyType companyType,
                                                       final String applicantId,
                                                       final CreateCorporateModel createCorporateModel,
                                                       final String externalUserId,
                                                       final String questionnaireId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setRepresentativeInInitiatedState(companyType, applicantId, createCorporateModel, externalUserId);
        setApplicantInPendingState(accessToken, applicantId, externalUserId);
    }

    public static void setRepresentativeInPendingState(final CompanyType companyType,
                                                       final String applicantId,
                                                       final CreateBuyerModel createBuyerModel,
                                                       final String externalUserId,
                                                       final String questionnaireId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setRepresentativeInInitiatedState(companyType, applicantId, createBuyerModel, externalUserId);
        setApplicantInPendingState(accessToken, applicantId, externalUserId);
    }

    public static void setRepresentativeInPendingState(final CompanyType companyType,
                                                       final String applicantId,
                                                       final String externalUserId,
                                                       final String questionnaireId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setRepresentativeInInitiatedState(companyType, applicantId, externalUserId, questionnaireId);
        setApplicantInPendingState(accessToken, applicantId, externalUserId);
    }

    public static void setInitiatedRepresentativeInPendingState(final CompanyType companyType,
                                                                final String applicantId,
                                                                final String externalUserId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setApplicantInPendingState(accessToken, applicantId, externalUserId);
    }

    public static void approveInitiatedRepresentative(final CompanyType companyType,
                                                      final String applicantId,
                                                      final String externalUserId) {
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setApplicantInPendingState(accessToken, applicantId, externalUserId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approvePendingRepresentative(final String applicantId,
                                                    final String externalUserId){
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approveRepresentative(final CompanyType companyType,
                                             final String applicantId,
                                             final CreateCorporateModel createCorporateModel,
                                             final String externalUserId,
                                             final String questionnaireId){
        setRepresentativeInPendingState(companyType, applicantId, createCorporateModel, externalUserId, questionnaireId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approveRepresentative(final CompanyType companyType,
                                             final String applicantId,
                                             final CreateBuyerModel createBuyerModel,
                                             final String externalUserId,
                                             final String questionnaireId){
        setRepresentativeInPendingState(companyType, applicantId, createBuyerModel, externalUserId, questionnaireId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approveRepresentativePukLevel(final CompanyType companyType,
                                             final String applicantId,
                                             final CreateCorporateModel createCorporateModel,
                                             final String externalUserId,
                                             final String questionnaireId){

        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());
        final DateOfBirthModel dateOfBirth = createCorporateModel.getRootUser().getDateOfBirth();

        final FixedInfoModel fixedInfoModel =
                FixedInfoModel.builder()
                        .setFirstName(createCorporateModel.getRootUser().getName())
                        .setLastName(createCorporateModel.getRootUser().getSurname())
                        .setDob(String.format("%s-%s-%s", dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDay()))
                        .setNationality("MLT")
                        .setPhone(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(), createCorporateModel.getRootUser().getMobile().getNumber()))
                        .setAddresses(Collections.singletonList(SumSubAddressModel.builder()
                                .setStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine1())
                                .setSubStreet(createCorporateModel.getCompany().getBusinessAddress().getAddressLine2())
                                .setTown(createCorporateModel.getCompany().getBusinessAddress().getCity())
                                .setPostCode(createCorporateModel.getCompany().getBusinessAddress().getPostCode())
                                .setCountry("MLT")
                                .setState(createCorporateModel.getCompany().getBusinessAddress().getState())
                                .build()))
                        .setPlaceOfBirth("Malta")
                        .setCountry("MLT")
                        .build();
        submitApplicantInformation(fixedInfoModel, accessToken, applicantId);

        final List<IdDocType> requiredDocuments = Arrays.asList(GERMAN_DRIVER_LICENCE, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        uploadQuestionnaire(accessToken, applicantId, buildUboDirectorQuestionnaire(applicantId));

        TestHelper.ensureAsExpected(15, () ->
                SumSubService.setApplicantInPendingState(accessToken, applicantId), SC_OK);

        if (isDevEnvironment())
            sendPendingCallback(externalUserId);

        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void setShareholderInInitiatedState(final CompanyType companyType,
                                                         final String applicantId,
                                                         final String externalUserId){
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        final List<IdDocType> requiredDocuments = Arrays.asList(SHAREHOLDER_REGISTRY);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);
    }

    public static void setInitiatedShareholderInPendingState(final CompanyType companyType,
                                                                final String applicantId,
                                                                final String externalUserId) {
        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        setApplicantInPendingState(accessToken, applicantId, externalUserId);
    }

    public static void approveShareholder(final CompanyType companyType,
                                             final String applicantId,
                                             final String externalUserId){
        setInitiatedShareholderInPendingState(companyType, applicantId, externalUserId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approveRepresentative(final CompanyType companyType,
                                             final String applicantId,
                                             final String externalUserId,
                                             final String questionnaireId){
        setRepresentativeInInitiatedState(companyType, applicantId, externalUserId, questionnaireId);
        setInitiatedRepresentativeInPendingState(companyType, applicantId, externalUserId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void rejectRepresentative(final CompanyType companyType,
                                            final String applicantId,
                                            final String externalUserId,
                                            final String questionnaireId){
        setRepresentativeInInitiatedState(companyType, applicantId, externalUserId, questionnaireId);
        setInitiatedRepresentativeInPendingState(companyType, applicantId, externalUserId);
        final List<String> rejectLabels = List.of(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        setApplicantInRejectState(rejectLabels, ReviewRejectType.FINAL, Optional.empty(), applicantId, externalUserId);

        verifyBeneficiaryState(applicantId, KycState.REJECTED);
    }

    public static void approveBeneficiary(final String applicantId,
                                          final String externalUserId,
                                          final CompanyType companyType){

        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, UTILITY_BILL);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        setApplicantInPendingState(accessToken, applicantId, externalUserId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void approveBeneficiaryPukLevel(final String applicantId,
                                                  final String externalUserId,
                                                  final CompanyType companyType){

        final String accessToken = generateCorporateAccessToken(externalUserId, companyType.name());

        SumSubHelper.submitApplicantInformation(FixedInfoModel.randomFixedInfoModel().build(), accessToken, applicantId);

        final List<IdDocType> requiredDocuments = Arrays.asList(ID_CARD_FRONT, ID_CARD_BACK, SELFIE, UTILITY_BILL, UTILITY_BILL2);
        uploadRequiredDocuments(requiredDocuments, accessToken, applicantId);

        setApplicantInPendingState(accessToken, applicantId, externalUserId);
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void verifyBeneficiaryState(final String applicantId, final KycState state) {

        TestHelper.ensureDatabaseResultAsExpected(45,
                () -> SumsubDatabaseHelper.getBeneficiary(applicantId),
                x -> x.size() > 0 && x.get(0).get("status").equals(state.name()),
                Optional.of(String.format("Applicant with id %s not in state %s as expected", applicantId, state.name())));
    }

    public static QuestionnaireRequest buildDefaultCorporateQuestionnaire(final String applicantId) {
        return QuestionnaireRequest.builder()
                .id(applicantId)
                .questionnaires(addQuestionnaire("corporate_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("businessaddress", null),
                                addItem("businessaddressissame", "yes"),
                                addItem("correspondenceaddress", null),
                                addItem("correspondenceaddressissame", "yes"),
                                addItem("microenterprise", "no"),
                                addItem("tin", "12345678")
                        ))),
                        addSection("section2", addItems(Map.ofEntries(
                                addItem("website", "www.test.com"),
                                addItem("category_art", "art-galleries"),
                                addItem("expectedmonthlyturnover", "upto10k"),
                                addItem("industrycategory", "category_art"),
                                addItem("lengthofoperation", "lessthan12months"),
                                addItem("licencerequired", "no")
                        ))),
                        addSection("section3", addItems(Map.ofEntries(
                                addItem("expectedoriginoffunds", "dividends"),
                                addItem("expectedoriginoffundsother", null),
                                addItem("expectedsourceoffundslocation", "domestic"),
                                addItem("expectedpaymentreceiptcountries", "Malta, Italy, Spain, France, Portugal")
                        ))),
                        addSection("section4", addItems(Map.ofEntries(
                                addItem("expectedaverageincomingfundsamount", "upto15k"),
                                addItem("expectedaverageoutgoingfundsamount","upto15k"),
                                addItem("expectedcardsmonthly", "100"),
                                addItem("expectedincomingtransfersmonthly", "morethan25"),
                                addItem("expectedincomingtransfervolumemonthly", "upto15k"),
                                addItem("expectedoutgoingtransfersmonthly", "lessthan10"),
                                addItem("expectedoutgoingtransfervolumemonthly", "upto15k")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static QuestionnaireRequest buildDefaultCorporateSoleTraderQuestionnaire(final String applicantId,
                                                                                    final String industry,
                                                                                    final String sourceOfFunds,
                                                                                    final String registrationNumber) {
        return QuestionnaireRequest.builder()
                .id(applicantId)
                .questionnaires(addQuestionnaire("corporate_sole_trader_questionnaire", Map.ofEntries(
                        addSection("section1", addItems(Map.ofEntries(
                                addItem("correspondenceaddress", "123"),
                                addItem("correspondenceaddressissame", "yes"),
                                addItem("isregistered", "yes"),
                                addItem("registrationnumber", registrationNumber)
                        ))),
                        addSection("section2", addItems(Map.ofEntries(
                                addItem("website", "www.test.com"),
                                addItem("category_art", industry),
                                addItem("industrycategory", "category_art"),
                                addItem("lengthofoperation", "lessthan12months"),
                                addItem("licencerequired", "no")
                        ))),
                        addSection("section3", addItems(Map.ofEntries(
                                addItem("expectedoriginoffunds", sourceOfFunds),
                                addItem("expectedoriginoffundsother", null),
                                addItem("expectedsourceoffundslocation", "domestic"),
                                addItem("expectedpaymentreceiptcountries", "Malta, Italy, Spain, France, Portugal")
                        ))),
                        addSection("section4", addItems(Map.ofEntries(
                                addItem("expectedcardsmonthly", "100"),
                                addItem("expectedaverageincomingfundsamount", "upto15k"),
                                addItem("expectedaverageoutgoingfundsamount", "upto15k"),
                                addItem("expectedincomingtransfersmonthly", "morethan25"),
                                addItem("expectedincomingtransfervolumemonthly", "upto15k"),
                                addItem("expectedoutgoingtransfersmonthly", "lessthan10"),
                                addItem("expectedoutgoingtransfervolumemonthly", "upto15k")
                        ))),
                        addSection("declarationsection", addItems(Map.ofEntries(
                                addItem("declaration", "true")
                        )))
                )))
                .build();
    }

    public static Pair<String, String> addDirector(final CompanyType companyType,
                                                   final String accessToken,
                                                   final String applicantId) {
        final AddBeneficiaryModel addDirectorModel =
                AddBeneficiaryModel.defaultAddDirectorModel().build();

        final JsonPath director =
                SumSubHelper.addBeneficiary(accessToken, applicantId,
                        addDirectorModel).jsonPath();
        final String directorId = director.get("applicantId");
        final String directorExternalUserId = director.get("applicant.externalUserId");

        final String directorAccessToken =
                SumSubHelper.generateCorporateAccessToken(directorExternalUserId,
                        companyType.name());

        SumSubHelper.submitConsent(directorAccessToken, directorId);

        return Pair.of(directorId, directorExternalUserId);
    }

    public static Pair<String, String> addDirectorWithSpecialCharacters(final AddBeneficiaryModel addDirectorModel,
                                                                        final CompanyType companyType,
                                                                        final String accessToken,
                                                                        final String applicantId) {

        final JsonPath director = SumSubHelper.addBeneficiary(accessToken, applicantId, addDirectorModel).jsonPath();
        final String directorId = director.get("applicantId");
        final String directorExternalUserId = director.get("applicant.externalUserId");
        final String directorAccessToken = SumSubHelper.generateCorporateAccessToken(directorExternalUserId, companyType.name());
        SumSubHelper.submitConsent(directorAccessToken, directorId);

        return Pair.of(directorId, directorExternalUserId);
    }

    public static void approveDirector(final String applicantId,
                                       final String externalUserId){
        setApplicantInApprovedState(externalUserId, applicantId);
    }

    public static void addAndApproveDirector(final CompanyType companyType,
                                             final String accessToken,
                                             final String applicantId) {
        final Pair<String, String> director = addDirector(companyType, accessToken, applicantId);
        approveDirector(director.getLeft(), director.getRight());
    }

    public static Response addLegalEntityDirector(final String accessToken,
        final String applicantId,
        final AddLegalEntityDirectorModel addLegalEntityDirectorModel) {

        return TestHelper.ensureAsExpected(15,
            () -> SumSubService.addLegalEntityDirector(accessToken, applicantId, addLegalEntityDirectorModel),
            SC_OK);
    }

    public static Response submitConsent(final String accessToken,
                                         final String applicantId) {

        return TestHelper.ensureAsExpected(15,
                () -> SumSubService.submitConsent(accessToken, applicantId, SumSumAgreementModel.defaultModel()),
                SC_OK);
    }
}
