package opc.junit.multi.securitysimulated.passwords;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.IdentityType;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.multi.securitysimulated.BaseSecurityConfigurationTests;
import opc.models.PasswordValidationModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public abstract class AbstractPasswordSecurityDisabledTests extends BaseSecurityConfigurationTests {

    protected IdentityType identityType;
    protected String identityId;
    protected String identityEmail;
    protected String identityProfileId;
    protected String authenticationToken;

    @BeforeAll
    public static void Setup(){
        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

    @Test
    public void SecurityDisabled_LoginTokenizedPassword_Success(){
        final String tokenizedPassword = tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.PASSWORD, authenticationToken);

        AuthenticationService.loginWithPassword(new LoginModel(identityEmail, new PasswordModel(tokenizedPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_CreatePasswordTokenizedPassword_Success(){

        final String identityId = createIdentity();

        final String tokenizedPassword = tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.PASSWORD, authenticationToken);

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(tokenizedPassword)).build();

        PasswordsService.createPassword(createPasswordModel, identityId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(identityId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_UpdatePasswordTokenizedPassword_Success(){
        final String tokenizedOldPassword = tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.PASSWORD, authenticationToken);
        final String tokenizedNewPassword = tokenize(RandomStringUtils.randomAlphanumeric(10), SecurityModelConfiguration.PASSWORD, authenticationToken);

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(tokenizedOldPassword),
                        new PasswordModel(tokenizedNewPassword));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(identityId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_ValidatePasswordTokenizedPassword_Success(){
        final String tokenizedPassword = tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.PASSWORD, authenticationToken);

        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(tokenizedPassword)), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SecurityDisabled_ResumeLostPasswordTokenizedPassword_Success(){
        PasswordsService.startLostPassword(new LostPasswordStartModel(identityEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String tokenizedPassword = tokenize("NewPass1234", SecurityModelConfiguration.PASSWORD, authenticationToken);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(identityEmail)
                        .setNewPassword(new PasswordModel(tokenizedPassword))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_ConsumeUserInviteTokenizedPassword_Success(){
        final UsersModel model = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(model, secretKey, authenticationToken);

        UsersService.inviteUser(secretKey, userId, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final String tokenizedPassword = tokenize(TestHelper.getDefaultPassword(secretKey), SecurityModelConfiguration.PASSWORD, authenticationToken);

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(tokenizedPassword)), secretKey, userId)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_LoginNonTokenizedPassword_Success(){
        AuthenticationService.loginWithPassword(new LoginModel(identityEmail, new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_CreatePasswordNonTokenizedPassword_Success(){

        final String identityId = createIdentity();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, identityId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(identityId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_UpdatePasswordNonTokenizedPassword_Success(){

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(identityId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_ValidatePasswordNonTokenizedPassword_Success(){
        PasswordsService.validatePassword(new PasswordValidationModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SecurityDisabled_ResumeLostPasswordNonTokenizedPassword_Success(){
        PasswordsService.startLostPassword(new LostPasswordStartModel(identityEmail), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(identityEmail)
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void SecurityDisabled_ConsumeUserInviteNonTokenizedPassword_Success(){
        final UsersModel model = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(model, secretKey, authenticationToken);

        UsersService.inviteUser(secretKey, userId, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey, userId)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    protected abstract String createIdentity();
}
