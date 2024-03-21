package opc.junit.multi.security;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.secure.*;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.admin.AdminService;
import opc.services.secure.SecureService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

@Tag(MultiTags.MULTI)
@Tag(MultiTags.SECURITY)
@Execution(ExecutionMode.SAME_THREAD)
public class BaseSecurityConfigurationTests {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static ProgrammeDetailsModel scaMcApp;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static String corporateCurrency;
    protected static String consumerCurrency;
    protected static String corporateId;
    protected static String consumerId;
    protected static String innovatorId;
    protected static String innovatorEmail;
    protected static String innovatorPassword;
    protected static String innovatorToken;
    protected static String programmeId;
    protected static String corporateAssociateRandom;
    protected static String consumerAssociateRandom;

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        scaMcApp = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.SCA_MC_APP);

        innovatorId = applicationOne.getInnovatorId();
        innovatorEmail = applicationOne.getInnovatorEmail();
        innovatorPassword = applicationOne.getInnovatorPassword();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountsProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountsProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();
        programmeId = applicationOne.getProgrammeId();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
    }

    protected static void updateSecurityModel(final Map<String, Boolean> securityModelConfig){
        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setWebhookDisabled(true)
                        .setSecurityModelConfig(securityModelConfig)
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateProgramme(updateProgrammeModel, programmeId, AdminService.loginAdmin()),
                SC_OK);
    }

    protected static String tokenizeAnon(final String value){

        final AnonTokenizeModel anonTokenizeModel =
                AnonTokenizeModel.builder()
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setPermanent(false)
                                        .setValue(value)
                                        .build())
                                .build()).build();

        return SecureService.anonTokenize(sharedKey, anonTokenizeModel, Optional.empty())
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }

    protected static String tokenize(final String value, final String associateRandom, final String authenticationToken){

        return tokenize(value, associateRandom, authenticationToken, sharedKey);
    }

    protected static String tokenize(final String value, final String associateRandom, final String authenticationToken, final String sharedKey){

        final TokenizeModel tokenizeModel =
                TokenizeModel.builder()
                        .setRandom(associateRandom)
                        .setValues(TokenizePropertiesModel.builder()
                                .setAdditionalProp1(AdditionalPropertiesModel.builder()
                                        .setValue(value)
                                        .setPermanent(false)
                                        .build())
                                .build())
                        .build();

        return SecureService.tokenize(sharedKey, authenticationToken, tokenizeModel)
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }

    protected String detokenize(final String token, final String associateRandom, final String authenticationToken){

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(token)
                        .setRandom(associateRandom)
                        .build();

        return SecureService.detokenize(sharedKey, authenticationToken, detokenizeModel)
                .then().statusCode(SC_OK).extract().jsonPath().get("value");
    }

    protected boolean isTokenized(final String token, final String associateRandom, final String authenticationToken){

        return isTokenized(token, associateRandom, authenticationToken, sharedKey);
    }

    protected boolean isTokenized(final String token, final String associateRandom, final String authenticationToken, final String sharedKey){

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(token)
                        .setRandom(associateRandom)
                        .build();

        return SecureService.detokenize(sharedKey,
                authenticationToken, detokenizeModel).statusCode() == SC_OK;
    }

    protected boolean isNotTokenized(final String token, final String associateRandom, final String authenticationToken){

        return isNotTokenized(token, associateRandom, authenticationToken, sharedKey);
    }

    protected boolean isNotTokenized(final String token, final String associateRandom, final String authenticationToken, final String sharedKey){

        final DetokenizeModel detokenizeModel =
                DetokenizeModel.builder()
                        .setPermanent(true)
                        .setToken(token)
                        .setRandom(associateRandom)
                        .build();

        return SecureService.detokenize(sharedKey,
                authenticationToken, detokenizeModel).statusCode() == SC_BAD_REQUEST;
    }

    protected static String associate(final String authenticationToken){

        return associate(authenticationToken, sharedKey);
    }

    protected static String associate(final String authenticationToken, final String sharedKey){

        return SecureService.associate(sharedKey, authenticationToken, Optional.empty())
                .then().statusCode(SC_OK).extract().jsonPath().get("random");
    }

    @AfterAll
    public static void resetConfiguration(){
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

}
