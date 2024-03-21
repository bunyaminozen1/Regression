package opc.junit.innovator.templates;

import opc.enums.opc.EmailTemplateType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.EmailTemplateContextModel;
import opc.models.innovator.*;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class SetConsumerEmailTemplateTests extends BaseEmailTemplateSetup {

    @ParameterizedTest
    @EnumSource(value = EmailTemplateType.class,
            names = { "FORGOT_PASSWORD_CONSUMER", "INVITE_CONSUMER",
                    "PASS_EXPIRY_NOTIFICATION_CONSUMER" })
    public void SetTemplate_ValidConsumerTemplateTypes_Success(final EmailTemplateType emailTemplateType){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(emailTemplateType.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(emailTemplateType.name()).build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("senderAddress", equalTo(emailTemplateModel.getTemplate().getSenderAddress()))
                .body("senderName", equalTo(emailTemplateModel.getTemplate().getSenderName()))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @Test
    public void SetTemplate_RequiredOnlyWithoutOptionalLocale_Success(){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.builder()
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .build())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
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
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("senderAddress", equalTo(emailTemplateModel.getTemplate().getSenderAddress()))
                .body("senderName", equalTo(""))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @ParameterizedTest
    @EnumSource(value = EmailTemplateType.class,
            names = { "FORGOT_PASSWORD_CORPORATE", "DIRECTORS_KYC", "INVITE_CORPORATE",
                    "PASS_EXPIRY_NOTIFICATION_CORPORATE" })
    public void SetTemplate_CrossIdentityTemplateType_InvalidContext(final EmailTemplateType emailTemplateType) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(emailTemplateType.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CONTEXT"));
    }

    @Test
    public void SetTemplate_UnknownTemplateType_BadRequest() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.UNKNOWN.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoTemplateType_BadRequest(final String context) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(context).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoAddress_BadRequest(final String address) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(address)
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"aa", "a@a", "a.com"})
    public void SetTemplate_InvalidEmail_BadRequest(final String address) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(address)
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoSubjectTemplate_BadRequest(final String subjectTemplate) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setSubjectTemplate(subjectTemplate)
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SetTemplate_NoContentTemplate_BadRequest(final String contentTemplate) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                                .setContentTemplate(contentTemplate)
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"e", "englishLong"})
    public void SetTemplate_InvalidLanguage_BadRequest(final String language) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setCountry("MT")
                                        .setLanguage(language)
                                        .build())
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M", "MLT"})
    public void SetTemplate_InvalidCountry_BadRequest(final String country) {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setCountry(country)
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SetTemplate_UnknownProfileId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CONSUMER.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId,
                RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_UnknownProgrammeId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CONSUMER.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, RandomStringUtils.randomNumeric(18),
                consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_CrossProgrammeChecks_NotFound() {

        final String otherProgrammeProfileId =
                InnovatorHelper
                        .createNewInnovatorWithConsumerProfile(CreateConsumerProfileModel
                                .DefaultCreateConsumerProfileModel()).getRight();

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CONSUMER.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId,
                otherProgrammeProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_CrossIdentityProfileId_NotFound() {
        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CONSUMER.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId,
                corporateProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SetTemplate_InvalidToken_Unauthorised() {

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.defaultEmailTemplateModel(EmailTemplateType.FORGOT_PASSWORD_CONSUMER.name()).build();

        InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId,
                consumerProfileId, RandomStringUtils.randomAlphanumeric(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}