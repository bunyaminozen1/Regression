package opc.junit.multi.securitysimulated;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.InnovatorSetup;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.DetokenizeModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.services.admin.AdminService;
import opc.services.simulator.SimulatorService;
import opc.tags.MultiTags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;

@Tag(MultiTags.MULTI)
@Tag(MultiTags.SECURITY)
@Execution(ExecutionMode.SAME_THREAD)
public class BaseSecurityConfigurationTests {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String corporateManagedAccountsProfileId;
    protected static String consumerManagedAccountsProfileId;
    protected static String secretKey;
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

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

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

    protected String tokenizeAnon(final String value, final SecurityModelConfiguration field){
        return SimulatorService.tokenizeAnon(secretKey,
                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(value, field.name()))))
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }

    protected String tokenize(final String value, final SecurityModelConfiguration field, final String token){
        return SimulatorService.tokenize(secretKey,
                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(value, field.name()))), token)
                .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");
    }

    protected boolean isTokenized(final String token, final SecurityModelConfiguration field, final String authenticationToken){
        return SimulatorService.detokenize(secretKey,
                new DetokenizeModel(token, field.name()), authenticationToken).statusCode() == SC_OK;
    }

    protected boolean isNotTokenized(final String token, final SecurityModelConfiguration field, final String authenticationToken){
        return SimulatorService.detokenize(secretKey,
                new DetokenizeModel(token, field.name()), authenticationToken).statusCode() == SC_BAD_REQUEST;
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
