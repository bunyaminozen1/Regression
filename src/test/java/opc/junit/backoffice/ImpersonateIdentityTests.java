package opc.junit.backoffice;

import opc.enums.opc.IdentityType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.backoffice.ImpersonateIdentityModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.backoffice.multi.BackofficeMultiService;
import opc.services.innovator.InnovatorService;
import opc.tags.MultiBackofficeTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiBackofficeTags.MULTI_BACKOFFICE_IDENTITIES)
public class ImpersonateIdentityTests {

    @RegisterExtension
    static opc.junit.innovator.BaseSetupExtension setupExtension = new BaseSetupExtension();

    private static ProgrammeDetailsModel applicationOne;
    private static String corporateProfileId;
    private static String consumerProfileId;
    private static String secretKey;
    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        secretKey = applicationOne.getSecretKey();

        corporateSetup();
        consumerSetup();
    }

    @Test
    public void ImpersonateIdentity_CorporateIdentity_Success(){
        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(corporateId, IdentityType.CORPORATE), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token.token", notNullValue())
                .body("identity.type", equalTo("CORPORATE"))
                .body("identity.id", equalTo(corporateId))
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(corporateId));
    }

    @Test
    public void ImpersonateIdentity_ConsumerIdentity_Success(){
        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(consumerId, IdentityType.CONSUMER), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token.token", notNullValue())
                .body("identity.type", equalTo("CONSUMER"))
                .body("identity.id", equalTo(consumerId))
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(consumerId));
    }

    @Test
    public void ImpersonateIdentity_CrossIdentity_NotFound(){
        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(consumerId, IdentityType.CORPORATE), secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ImpersonateIdentity_NonRootConsumerUserIdentity_NotFound(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerAuthenticationToken);

        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(user.getLeft(), IdentityType.CONSUMER), secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ImpersonateIdentity_NonRootCorporateUserIdentity_NotFound(){

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(user.getLeft(), IdentityType.CORPORATE), secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ImpersonateIdentity_UnknownIdentity_NotFound(){

        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(RandomStringUtils.randomNumeric(18), IdentityType.CORPORATE),
                secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ImpersonateIdentity_InvalidApiKey_Unauthorised(){
        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(corporateId, IdentityType.CORPORATE),
                "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ImpersonateIdentity_NoApiKey_BadRequest(){
        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(corporateId, IdentityType.CORPORATE),
                "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ImpersonateIdentity_DifferentInnovatorApiKey_NotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(corporateId, IdentityType.CORPORATE), secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
