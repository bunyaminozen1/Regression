package opc.junit.innovator.templates;

import opc.enums.opc.EmailTemplateType;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.models.EmailTemplateContextModel;
import opc.models.innovator.*;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetConsumerEmailTemplateTests extends BaseEmailTemplateSetup {

    private static final List<EmailTemplateModel> emailTemplates = new ArrayList<>();

    @BeforeAll
    public static void TestSetup(){
        Arrays.stream(EmailTemplateType.values()).filter(x -> x.name().contains("CONSUMER")).forEach(template -> {
            final EmailTemplateModel emailTemplateModel =
                    EmailTemplateModel.defaultEmailTemplateModel(template.name()).build();

            InnovatorService.setConsumerProfileTemplate(emailTemplateModel, programmeId, consumerProfileId, innovatorToken)
                    .then()
                    .statusCode(SC_NO_CONTENT);

            emailTemplates.add(emailTemplateModel);
        });
    }

    @Test
    public void GetTemplate_SpecificContext_Success(){
        emailTemplates.forEach(template -> {

            final EmailTemplateContextModel emailTemplateContextModel =
                    EmailTemplateContextModel.builder()
                            .setContext(template.getContext())
                            .build();

            InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                    .then()
                    .statusCode(SC_OK)
                    .body("senderAddress", equalTo(template.getTemplate().getSenderAddress()))
                    .body("senderName", equalTo(template.getTemplate().getSenderName()))
                    .body("subjectTemplate", equalTo(template.getTemplate().getSubjectTemplate()))
                    .body("contentTemplate", equalTo(template.getTemplate().getContentTemplate()));
        });
    }

    @Test
    public void GetTemplate_PartialContext_Success(){

        final EmailTemplateModel emailTemplateModel =
                EmailTemplateModel.builder()
                        .setTemplate(TemplateModel.builder()
                                .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                                .setSenderName(RandomStringUtils.randomAlphabetic(5))
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
                .body("senderName", equalTo(emailTemplateModel.getTemplate().getSenderName()))
                .body("subjectTemplate", equalTo(emailTemplateModel.getTemplate().getSubjectTemplate()))
                .body("contentTemplate", equalTo(emailTemplateModel.getTemplate().getContentTemplate()));
    }

    @ParameterizedTest
    @EnumSource(value = EmailTemplateType.class,
            names = { "FORGOT_PASSWORD_CORPORATE", "INVITE_CORPORATE",
                    "PASS_EXPIRY_NOTIFICATION_CORPORATE", "DIRECTORS_KYC" })
    public void GetTemplate_CrossIdentityTemplateType_InvalidContext(final EmailTemplateType emailTemplateType) {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(emailTemplateType.name()).build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_CONTEXT"));
    }

    @Test
    public void GetTemplate_UnknownTemplateType_BadRequest() {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.UNKNOWN.name()).build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void GetTemplate_NoTemplateType_BadRequest(final String context) {
        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(context).build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"e", "englishLong"})
    public void GetTemplate_InvalidLanguage_BadRequest(final String language) {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.builder()
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setLanguage(language)
                                        .build())
                                .build())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M", "MLT"})
    public void GetTemplate_InvalidCountry_BadRequest(final String country) {
        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.builder()
                        .setContext(TemplateContextModel.builder()
                                .setType(EmailTemplateType.INVITE_CONSUMER.name())
                                .setLocale(LocaleModel.builder()
                                        .setCountry(country)
                                        .setLanguage("en")
                                        .build())
                                .build())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId, consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetTemplate_UnknownProfileId_NotFound() {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId,
                RandomStringUtils.randomNumeric(18), innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTemplate_UnknownProgrammeId_NotFound() {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, RandomStringUtils.randomNumeric(18),
                consumerProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTemplate_CrossProgrammeChecks_NotFound() {

        final String otherProgrammeProfileId =
                InnovatorHelper
                        .createNewInnovatorWithConsumerProfile(CreateConsumerProfileModel
                                .DefaultCreateConsumerProfileModel()).getRight();

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId,
                otherProgrammeProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTemplate_CrossIdentityProfileId_NotFound() {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId,
                corporateProfileId, innovatorToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetTemplate_InvalidToken_Unauthorised() {

        final EmailTemplateContextModel emailTemplateContextModel =
                EmailTemplateContextModel.defaultEmailTemplateContextModel(EmailTemplateType.INVITE_CONSUMER.name())
                        .build();

        InnovatorService.getConsumerProfileTemplate(emailTemplateContextModel, programmeId,
                consumerProfileId, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}