package opc.junit.admin.templates;

import opc.enums.opc.EmailTemplateType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.EmailTemplateContextModel;
import opc.models.innovator.*;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class SetCorporateEmailTemplateTests extends BaseEmailTemplateSetup {

    @ParameterizedTest
    @EnumSource(value = EmailTemplateType.class,
            names = { "FORGOT_PASSWORD_CORPORATE", "DIRECTORS_KYC", "INVITE_CORPORATE",
                    "PASS_EXPIRY_NOTIFICATION_CORPORATE", "VERIFY_EMAIL" })
    public void SetTemplate_ValidCorporateTemplateTypes_Success(final EmailTemplateType emailTemplateType){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(emailTemplateType.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(emailTemplateType.name()).build();

        AdminService.getCorporateProfileTemplate(emailTemplateContextModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("senderAddress", equalTo(emailTemplateModel.getTemplate().getSenderAddress()))
                .body("senderName", equalTo(emailTemplateModel.getTemplate().getSenderName()))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @Test
    public void SetTemplate_RequiredOnly_Success(){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.builder()
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.DIRECTORS_KYC.name())
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.DIRECTORS_KYC.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.DIRECTORS_KYC.name())
                                .build())
                        .build();

        AdminService.getCorporateProfileTemplate(emailTemplateContextModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("senderAddress", equalTo(emailTemplateModel.getTemplate().getSenderAddress()))
                .body("senderName", equalTo(""))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @Test
    public void SetTemplate_RequiredOnlyInLocale_Success(){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.builder()
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.DIRECTORS_KYC.name())
                                .setLocale(LocaleModel.builder()
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.DIRECTORS_KYC.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.DIRECTORS_KYC.name())
                                .setLocale(LocaleModel.builder()
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        AdminService.getCorporateProfileTemplate(emailTemplateContextModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_OK)
                .body("senderAddress", equalTo(emailTemplateModel.getTemplate().getSenderAddress()))
                .body("senderName", equalTo(""))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @ParameterizedTest
    @EnumSource(value = EmailTemplateType.class,
            names = { "FORGOT_PASSWORD_CONSUMER", "INVITE_CONSUMER",
                    "PASS_EXPIRY_NOTIFICATION_CONSUMER" })
    public void SetTemplate_CrossIdentityTemplateType_InvalidContext(final EmailTemplateType emailTemplateType) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(emailTemplateType.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CONTEXT"));
    }

    @Test
    public void SetTemplate_UnknownTemplateType_BadRequest() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.UNKNOWN.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoTemplateType_BadRequest(final String context) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(context).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoAddress_BadRequest(final String address) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(address)
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aa", "a@a", "a.com"})
    public void SetTemplate_InvalidEmail_BadRequest(final String address) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(address)
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoSubjectTemplate_BadRequest(final String subjectTemplate) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(subjectTemplate)
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoContentTemplate_BadRequest(final String contentTemplate) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setContentTemplate(contentTemplate)
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"e", "englishLong"})
    public void SetTemplate_InvalidLanguage_BadRequest(final String language) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CORPORATE.name())
                                .setLocale(LocaleModel.builder()
                                        .setCountry("MT")
                                        .setLanguage(language)
                                        .build())
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M", "MLT"})
    public void SetTemplate_InvalidCountry_BadRequest(final String country) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CORPORATE.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CORPORATE.name())
                                .setLocale(LocaleModel.builder()
                                        .setCountry(country)
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId, corporateProfileId, adminToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SetTemplate_UnknownProfileId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CORPORATE.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId,
                RandomStringUtils.randomNumeric(18), adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_UnknownProgrammeId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CORPORATE.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, RandomStringUtils.randomNumeric(18),
                corporateProfileId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_CrossProgrammeChecks_NotFound() {

        final String otherProgrammeProfileId =
                InnovatorHelper
                        .createNewInnovatorWithCorporateProfile(CreateCorporateProfileModel
                                .DefaultCreateCorporateProfileModel().build()).getRight();

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CORPORATE.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId,
                otherProgrammeProfileId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_CrossIdentityProfileId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CORPORATE.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId,
                consumerProfileId, adminToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_InvalidToken_Unauthorised() {

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CORPORATE.name()).build();

        AdminService.setCorporateProfileTemplate(emailTemplateModel, programmeId,
                corporateProfileId, RandomStringUtils.randomAlphanumeric(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
