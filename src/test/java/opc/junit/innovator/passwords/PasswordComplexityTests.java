package opc.junit.innovator.passwords;

import io.restassured.response.Response;
import opc.junit.database.PasswordDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.innovator.CreateApplicationInitModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Execution(ExecutionMode.CONCURRENT)
public class PasswordComplexityTests extends BasePasswordsSetup{

    /**
     * This class checks if password complexity is 4 when constraint for a programme is PASSWORD. New profiles will
     * have complexity 4, but in this class we simulate existing profiles that had
     */

    private static IdentityDetails corporateRootDetails;
    private static IdentityDetails consumerRootDetails;
    private static IdentityDetails corporateAuthDetails;

    @BeforeAll
    public static void setup() throws SQLException {

        corporateRootDetails = corporateSetup();
        consumerRootDetails = consumerSetup();
        corporateAuthDetails = userSetup(corporateRootDetails.getToken());

        //Execute migration script
        PasswordDatabaseHelper.updatePasswordProfile(applicationTwo.getInnovatorId(), "4", "1");
    }

    @Test
    public void PasswordComplexity_CheckComplexityOnProfileLevel_Success(){
        InnovatorService.getPasswordProfile(innovatorToken, programmeIdAppTwo, consumerProfileIdAppTwo)
                .then()
                .statusCode(SC_OK)
                .body("configPerCredentialType.ROOT.complexity", equalTo(4))
                .body("configPerCredentialType.USER.complexity", equalTo(4));

        InnovatorService.getPasswordProfile(innovatorToken, programmeIdAppTwo, corporateProfileIdAppTwo)
                .then()
                .statusCode(SC_OK)
                .body("configPerCredentialType.ROOT.complexity", equalTo(4))
                .body("configPerCredentialType.USER.complexity", equalTo(4));
    }

    @Test
    public void PasswordComplexity_UpdatePassword_Success(){
        final String token = AuthenticationHelper.login(corporateRootDetails.getEmail(), TestHelper.DEFAULT_PASSWORD, secretKeyAppTwo);

        final UpdatePasswordModel updatePasswordModelComplexityOne =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKeyAppTwo)),
                        new PasswordModel(TestHelper.DEFAULT_PASSWORD));

        PasswordsService.updatePassword(updatePasswordModelComplexityOne, secretKeyAppTwo, token)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SIMPLE"));

        final UpdatePasswordModel updatePasswordModelComplexityFour =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKeyAppTwo)),
                        new PasswordModel(TestHelper.DEFAULT_COMPLEX_PASSWORD));

        PasswordsService.updatePassword(updatePasswordModelComplexityFour, secretKeyAppTwo, token)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void PasswordComplexity_ResumeLostPassword_Success(){

        AuthenticationHelper.login(consumerRootDetails.getEmail(), TestHelper.DEFAULT_PASSWORD, secretKeyAppTwo);

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerRootDetails.getEmail()), secretKeyAppTwo)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel resumeModelComplexityOne =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerRootDetails.getEmail())
                        .setNewPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(resumeModelComplexityOne, secretKeyAppTwo)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SIMPLE"));

        final LostPasswordResumeModel resumeModelComplexityFour =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(consumerRootDetails.getEmail())
                        .setNewPassword(new PasswordModel(TestHelper.DEFAULT_COMPLEX_PASSWORD))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(resumeModelComplexityFour, secretKeyAppTwo)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void PasswordComplexity_ExpiredPassword_Success(){

        AuthenticationHelper.login(corporateAuthDetails.getEmail(), TestHelper.DEFAULT_PASSWORD, secretKeyAppTwo);

        SimulatorHelper.simulateSecretExpiry(secretKeyAppTwo, corporateAuthDetails.getId());

        final String token = AuthenticationService.loginWithPassword(new LoginModel(corporateAuthDetails.getEmail(),
                        new PasswordModel(TestHelper.DEFAULT_PASSWORD)), secretKeyAppTwo)
                .then()
                .statusCode(SC_CONFLICT)
                .extract()
                .jsonPath()
                .getString("token");

        final UpdatePasswordModel updatePasswordModelComplexityOne = new UpdatePasswordModel(
                new PasswordModel(TestHelper.DEFAULT_PASSWORD), new PasswordModel(TestHelper.DEFAULT_PASSWORD));

        PasswordsService.updatePassword(updatePasswordModelComplexityOne, secretKeyAppTwo, token)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SIMPLE"));

        final UpdatePasswordModel updatePasswordModelComplexityFour =
                new UpdatePasswordModel(new PasswordModel(TestHelper.DEFAULT_PASSWORD),
                        new PasswordModel(TestHelper.DEFAULT_COMPLEX_PASSWORD));

        PasswordsService.updatePassword(updatePasswordModelComplexityFour, secretKeyAppTwo, token)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    @Disabled
    public void PasswordComplexity_CreateNewProgramme_Success(){

        final CreateApplicationInitModel model = CreateApplicationInitModel.createBusinessPayoutsModel(RandomStringUtils.randomAlphabetic(5));
        final Response response = TestHelper.ensureAsExpected(15, () -> InnovatorService.createPaymentModel(model, innovatorToken), SC_OK);

        final String programmeId = response.jsonPath().getString("programmeId");
        final String corporateProfileId = response.jsonPath().getString("corporatesProfileId[0]");
        final String consumerProfileId = response.jsonPath().getString("consumersProfileId[0]");

        InnovatorService.getPasswordProfile(innovatorToken, programmeId, corporateProfileId)
                .then()
                .statusCode(SC_OK)
                .body("configPerCredentialType.ROOT.complexity", equalTo(4))
                .body("configPerCredentialType.USER.complexity", equalTo(4));

        InnovatorService.getPasswordProfile(innovatorToken, programmeId, consumerProfileId)
                .then()
                .statusCode(SC_OK)
                .body("configPerCredentialType.ROOT.complexity", equalTo(4))
                .body("configPerCredentialType.USER.complexity", equalTo(4));

    }

    private static IdentityDetails corporateSetup(){
        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileIdAppTwo).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKeyAppTwo);
        return IdentityDetails.generateDetails(corporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), null, null, null);
    }

    private static IdentityDetails consumerSetup(){
        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileIdAppTwo).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerModel, secretKeyAppTwo);
        return IdentityDetails.generateDetails(consumerModel.getRootUser().getEmail(), consumer.getLeft(),
                consumer.getRight(), null, null, null);
    }

    private static IdentityDetails userSetup(final String authenticationToken){
        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(userModel, secretKeyAppTwo, authenticationToken);
        return IdentityDetails.generateDetails(userModel.getEmail(), user.getLeft(), user.getRight(), null, null, null);
    }

    @AfterAll
    public static void reset() throws SQLException {
        //Execute migration script
        PasswordDatabaseHelper.updatePasswordProfile(applicationTwo.getInnovatorId(), "1", "4");

    }
}
