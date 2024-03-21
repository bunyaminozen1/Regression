package opc.services.sumsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import commons.config.ConfigHelper;
import opc.enums.opc.ConsumerApplicantLevel;
import opc.enums.opc.UrlType;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.AddDocumentModel;
import opc.models.sumsub.AddLegalEntityDirectorModel;
import opc.models.sumsub.CompanyInfoModel;
import opc.models.sumsub.FixedInfoModel;
import opc.models.sumsub.SetApplicantStateModel;
import opc.models.sumsub.SumSumAgreementModel;
import opc.models.sumsub.questionnaire.custom.QuestionnaireRequest;
import commons.services.BaseService;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;

public class SumSubService extends BaseService {

    private static final String APP_TOKEN = ConfigHelper.getEnvironmentConfiguration().getSumsubToken();
    private static final String SUMSUB_SECRET_KEY = ConfigHelper.getEnvironmentConfiguration().getSumsubSecret();

    public static Response getApplicantData(final String accessToken,
                                            final String externalUserId){

        final String url = String.format("/resources/applicants/-;externalUserId=%s/one", externalUserId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "GET", url, null))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .when()
                .get(url);
    }

    public static Response getApplicantDataOne(final String accessToken,
                                               final String applicantId) {
        final String url = String.format("/resources/applicants/%s/one", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "GET", url, null))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .when()
                .get(url);
    }

    public static Response getApplicantLatestCheck(final String applicantId) {
        final String url = String.format("/resources/checks/latest?type=COMPANY&applicantId=%s", applicantId) ;
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "GET", url, null))
                .header("X-App-Access-ts", timestamp)
                .when()
                .get(url);
    }

    public static Response getApplicantById(final String accessToken,
                                            final String applicantId){

        final String url = String.format("/resources/applicants/%s", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "GET", url, null))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .when()
                .get(url);
    }

    public static Response submitApplicantInformation(final String accessToken,
                                                      final String applicantId,
                                                      final FixedInfoModel fixedInfoModel){

        final String url = String.format("/resources/applicants/%s/fixedInfo", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "PATCH", url, fixedInfoModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .body(fixedInfoModel)
                .when()
                .patch(url);
    }
    public static Response updateApplicantInformation(final String accessToken,
        final String applicantId,
        final FixedInfoModel fixedInfoModel){

        final String url = String.format("/resources/applicants/%s/info", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
            .header("Content-type", "application/json")
            .header("X-App-Token", APP_TOKEN)
            .header("X-App-Access-Sig", createSignature(timestamp, "PATCH", url, fixedInfoModel))
            .header("X-App-Access-ts", timestamp)
            .header("X-Access-Token", accessToken)
            .body(fixedInfoModel)
            .when()
            .patch(url);
    }

    public static Response addIdDocument(final String accessToken,
                                         final String applicantId,
                                         final AddDocumentModel addDocumentModel,
                                         final String fileName) throws JsonProcessingException {
        final String url = String.format("/resources/applicants/%s/info/idDoc", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "multipart/form-data")
                .header("X-Return-Doc-Warnings", true)
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, addDocumentModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .multiPart("content", new File(String.format("./src/test/resources/SumSub/%s.png", fileName)))
                .multiPart("metadata", new ObjectMapper().writeValueAsString(addDocumentModel))
                .when()
                .post(url);
    }

    public static Response addQuestionnaire(final String accessToken,
                                            final String applicantId,
                                            final QuestionnaireRequest questionnaireModel) throws JsonProcessingException {
        final String url = String.format("/resources/applicants");
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "PATCH", url, questionnaireModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Applicant-Id", applicantId)
                .header("X-Access-Token", accessToken)
                .header("X-Id-DocSet-Type", "QUESTIONNAIRE")
                .body(questionnaireModel)
                .when()
                .patch(url);

    }

    public static Response changeApplicantLevel(final String applicantId,
                                                final String level) {

        final String url = String.format("/resources/applicants/%s/moveToLevel?name=%s", applicantId, level);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, null))
                .header("X-App-Access-ts", timestamp)
                .when()
                .post(url);
    }

    public static Response setApplicantInPendingState(final String accessToken,
                                                      final String applicantId) {
        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-Access-Token", accessToken)
                .pathParam("applicantId", applicantId)
                .when()
                .post("/resources/applicants/{applicantId}/status/pending");
    }

    public static Response setApplicantState(final SetApplicantStateModel setApplicantStateModel,
                                             final String applicantId) {
        final String url = String.format("/resources/applicants/%s/status/testCompleted", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, setApplicantStateModel))
                .header("X-App-Access-ts", timestamp)
                .body(setApplicantStateModel)
                .when()
                .post(url);
    }

    public static Response getApplicantRequiredDocStatus(final String accessToken, final String applicantId) {

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-Access-Token", accessToken)
                .pathParam("applicantId", applicantId)
                .when()
                .get("/resources/applicants/{applicantId}/requiredIdDocsStatus");
    }

    public static Response submitCompanyInformation(final String accessToken,
                                                    final String applicantId,
                                                    final CompanyInfoModel companyInfoModel){

        final String url = String.format("/resources/applicants/%s/info/companyInfo", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "PATCH", url, companyInfoModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .body(companyInfoModel)
                .when()
                .patch(url);
    }

    public static Response addBeneficiary(final String accessToken,
                                          final String applicantId,
                                          final AddBeneficiaryModel addBeneficiaryModel){

        final String url = String.format("/resources/applicants/%s/info/companyInfo/beneficiaries", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, addBeneficiaryModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .queryParam("sendEmail", false)
                .body(addBeneficiaryModel)
                .when()
                .post(url);
    }

    public static Response generateCorporateAccessToken(final String externalUserId,
                                                        final String levelName){

        final String url = String.format("/resources/accessTokens?userId=%s&levelName=%s", externalUserId, levelName);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, null))
                .header("X-App-Access-ts", timestamp)
                .when()
                .post(url);
    }

    public static Response generateConsumerAccessToken(final String externalUserId,
                                                       final ConsumerApplicantLevel applicantLevel){

        final String url = String.format("/resources/accessTokens?userId=%s&levelName=%s", externalUserId, applicantLevel.getLevelName());
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, null))
                .header("X-App-Access-ts", timestamp)
                .when()
                .post(url);
    }

    public static Response getImage(final String accessToken,
                                    final String applicantId,
                                    final FixedInfoModel fixedInfoModel){

        final String url = String.format("/resources/applicants/%s/fixedInfo", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "PATCH", url, fixedInfoModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .body(fixedInfoModel)
                .when()
                .patch(url);
    }

    private static RequestSpecification sumsubRestAssured() {
        //RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
        return restAssured(UrlType.SUMSUB);
    }

    private static String createSignature(long ts, String httpMethod, String path, final Object body)  {
        try {
            final Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(new SecretKeySpec(SUMSUB_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            hmacSha256.update((ts + httpMethod + path).getBytes(StandardCharsets.UTF_8));
            byte[] bytes = body == null ? hmacSha256.doFinal() : hmacSha256.doFinal(new ObjectMapper().writeValueAsBytes(body));
            return Hex.encodeHexString(bytes);
        } catch (Exception e){
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static Response addLegalEntityDirector(final String accessToken,
        final String applicantId,
        final AddLegalEntityDirectorModel addLegalEntityDirectorModel){

        final String url = String.format("/resources/applicants/%s/info/companyInfo/beneficiaries", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
            .header("Content-type", "application/json")
            .header("X-App-Token", APP_TOKEN)
            .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, addLegalEntityDirectorModel))
            .header("X-App-Access-ts", timestamp)
            .header("X-Access-Token", accessToken)
            .queryParam("sendEmail", false)
            .body(addLegalEntityDirectorModel)
            .when()
            .post(url);
    }

    public static Response submitConsent(final String accessToken,
                                         final String applicantId,
                                         final SumSumAgreementModel sumSumAgreementModel){

        final String url = String.format("/resources/applicants/%s/agreement", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, sumSumAgreementModel))
                .header("X-App-Access-ts", timestamp)
                .header("X-Access-Token", accessToken)
                .body(sumSumAgreementModel)
                .when()
                .post(url);
    }

    public static Response getApplicantTimeline(final String accessToken,
                                                final String applicantId){

        final String url = String.format("/resources/applicantTimeline/%s", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "GET", url, null))
                .header("X-App-Access-ts", timestamp)
                .when()
                .get(url);
    }

    public static Response addApplicantTag(final String applicantId,
                                            final String applicantTag){

        final String url = String.format("/resources/applicants/%s/tags", applicantId);
        final long timestamp = Instant.now().toEpochMilli() / 1000;

        return sumsubRestAssured()
                .header("Content-type", "application/json")
                .header("X-App-Token", APP_TOKEN)
                .header("X-App-Access-Sig", createSignature(timestamp, "POST", url, Arrays.asList(applicantTag)))
                .header("X-App-Access-ts", timestamp)
                .body(Arrays.asList(applicantTag))
                .when()
                .post(url);
    }
}
