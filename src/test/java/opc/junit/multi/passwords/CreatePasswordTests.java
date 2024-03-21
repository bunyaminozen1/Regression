package opc.junit.multi.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CreatePasswordTests extends BasePasswordSetup {

    @Test
    public void CreatePassword_CorporateRoot_Success() {
        final String corporateId = createCorporate();
        final IdentityType identityType = IdentityType.CORPORATE;

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(corporateId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_ConsumerRoot_Success() {
        final String consumerId = createConsumer();
        final IdentityType identityType = IdentityType.CONSUMER;

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, consumerId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(consumerId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_CorporateRootUser_Success() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String userId = createUser(authenticatedCorporate.getRight());

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
                .body("passwordInfo.identityId.id", equalTo(authenticatedCorporate.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_ConsumerRootUser_Success() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        final String userId = createUser(authenticatedConsumer.getRight());

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
                .body("passwordInfo.identityId.id", equalTo(authenticatedConsumer.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_InvalidApiKey_Unauthorised() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePassword_NoApiKey_BadRequest() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreatePassword_DifferentInnovatorApiKey_NotFound() {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then().statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreatePassword_UnknownCorporateId_NotFound() {
        createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, TestHelper.VERIFICATION_CODE, secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreatePassword_AlreadyCreated_Conflict() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_OK);

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void CreatePassword_LongPassword_PasswordTooLong() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(51))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));
    }

    @Test
    public void CreatePassword_ShortPassword_PasswordTooShort() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(3))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }

    @Test
    public void CreatePassword_CreatePasswordAfterTooLongAndTooShortErrors_Success() {
        final String corporateId = createCorporate();

        final IdentityType identityType = IdentityType.CORPORATE;

        final CreatePasswordModel shortPasswordModel = CreatePasswordModel
            .newBuilder()
            .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(3))).build();

        PasswordsService.createPassword(shortPasswordModel, corporateId, secretKey)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final CreatePasswordModel longPasswordModel = CreatePasswordModel
            .newBuilder()
            .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(51))).build();

        PasswordsService.createPassword(longPasswordModel, corporateId, secretKey)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
            .newBuilder()
            .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
            .then()
            .statusCode(SC_OK)
            .body("passwordInfo.identityId.type", equalTo(identityType.name()))
            .body("passwordInfo.identityId.id", equalTo(corporateId))
            .body("passwordInfo.expiryDate", equalTo(0))
            .body("token", notNullValue());
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void CreatePassword_NoPassword_BadRequest(final String password) {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(password)).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreatePassword_NoPasswordModel_BadRequest() {
        final String corporateId = createCorporate();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(null).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void CreatePassword_OtherProgrammeApiKey_NotFound() {
        final String corporateId = createCorporate();
        final IdentityType identityType = IdentityType.CORPORATE;

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, applicationTwo.getSecretKey())
                .then()
                .statusCode(SC_NOT_FOUND);

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(identityType.name()))
                .body("passwordInfo.identityId.id", equalTo(corporateId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_CorporateRootInactive_Forbidden() {
        final String corporateId = createCorporate();

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "TEMPORARY"),
                corporateId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, corporateId, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePassword_ConsumerRootInactive_Forbidden() {
        final String consumerId = createConsumer();

        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, "TEMPORARY"),
                consumerId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, consumerId, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private String createCorporate() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        return TestHelper.ensureAsExpected(15,
                        () -> CorporatesService.createCorporate(createCorporateModel, secretKey, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id.id");
    }

    private String createConsumer() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        return TestHelper.ensureAsExpected(15,
                        () -> ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id.id");
    }

    private String createUser(final String authenticationToken) {
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        return TestHelper.ensureAsExpected(15,
                        () -> UsersService.createUser(usersModel, secretKey, authenticationToken, Optional.empty()),
                        SC_OK)
                .jsonPath()
                .get("id");
    }

}
