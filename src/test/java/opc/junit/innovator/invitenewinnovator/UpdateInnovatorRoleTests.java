package opc.junit.innovator.invitenewinnovator;

import opc.enums.opc.InnovatorRole;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.innovator.ConsumeInviteInnovatorModel;
import opc.models.innovator.InnovatorRoleModel;
import opc.models.innovator.InviteInnovatorUserModel;
import opc.models.innovator.ValidateInnovatorInvite;
import opc.models.shared.GetInnovatorUserModel;
import opc.models.shared.PasswordModel;
import opc.services.innovatornew.InnovatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static opc.enums.opc.InnovatorRole.INNOVATOR_OWNER;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UpdateInnovatorRoleTests extends BaseInnovatorSetup {

    private static InviteInnovatorUserModel inviteInnovatorUserModel;
    private static Pair<String, String> nonceAndInviteId;

    @ParameterizedTest
    @EnumSource(value = InnovatorRole.class)
    public void InviteNewInnovator_InnovatorInvited_Success(final InnovatorRole innovatorRole) {
        createInnovator(innovatorRole);

        final String token = InnovatorHelper.loginInnovator(inviteInnovatorUserModel.getEmail(), TestHelper.DEFAULT_INNOVATOR_PASSWORD);
        assertNotNull(token);

        Assert.assertEquals(getInnovatorName(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getName());
        Assert.assertEquals(getInnovatorRole(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getRole());
        Assert.assertEquals(getInnovatorSurname(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getSurname());

        final String innovatorId = getInnovatorId(inviteInnovatorUserModel.getEmail(), innovatorToken);

        InnovatorService.getUser(token, innovatorId)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(inviteInnovatorUserModel.getEmail()))
                .body("name", equalTo(inviteInnovatorUserModel.getName()))
                .body("role", equalTo(inviteInnovatorUserModel.getRole()))
                .body("surname", equalTo(inviteInnovatorUserModel.getSurname()));
    }

    @Test
    public void UpdateInnovator_NewInnovatorUpdatedUsingOwnToken_Success() {
        createInnovator(INNOVATOR_OWNER);

        final String createdInnovatorToken = InnovatorHelper.loginInnovator(inviteInnovatorUserModel.getEmail(), TestHelper.DEFAULT_INNOVATOR_PASSWORD);
        final String userId = getInnovatorUserId(inviteInnovatorUserModel.getEmail());

        final InnovatorRoleModel updateInnovatorRole = new InnovatorRoleModel(InnovatorRole.getRandomWithExcludedInnovatorRole(INNOVATOR_OWNER).getInnovatorRole());
        InnovatorService.innovatorUpdateRole(updateInnovatorRole,
                        userId, createdInnovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        Assert.assertEquals(getInnovatorName(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getName());
        Assert.assertEquals(getInnovatorRole(inviteInnovatorUserModel.getEmail(), innovatorToken), updateInnovatorRole.getRole());
        Assert.assertEquals(getInnovatorSurname(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getSurname());

        final String innovatorId = getInnovatorId(inviteInnovatorUserModel.getEmail(), innovatorToken);

        InnovatorService.getUser(innovatorToken, innovatorId)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(inviteInnovatorUserModel.getEmail()))
                .body("name", equalTo(inviteInnovatorUserModel.getName()))
                .body("role", equalTo(updateInnovatorRole.getRole()))
                .body("surname", equalTo(inviteInnovatorUserModel.getSurname()));
    }

    @Test
    public void UpdateInnovator_NonInnovatorOwnerTryingToUpdate_Conflict() {
        createInnovator(InnovatorRole.getRandomWithExcludedInnovatorRole(INNOVATOR_OWNER));

        final String createdInnovatorToken = InnovatorHelper.loginInnovator(inviteInnovatorUserModel.getEmail(), TestHelper.DEFAULT_INNOVATOR_PASSWORD);
        InnovatorService.innovatorUpdateRole(new InnovatorRoleModel(InnovatorRole.getRandomWithExcludedInnovatorRole(INNOVATOR_OWNER).getInnovatorRole()),
                        nonceAndInviteId.getRight(), createdInnovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVALID_PERMISSION"));

        Assert.assertEquals(getInnovatorName(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getName());
        Assert.assertEquals(getInnovatorRole(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getRole());
        Assert.assertEquals(getInnovatorSurname(inviteInnovatorUserModel.getEmail(), innovatorToken), inviteInnovatorUserModel.getSurname());

        final String innovatorId = getInnovatorId(inviteInnovatorUserModel.getEmail(), innovatorToken);

        InnovatorService.getUser(innovatorToken, innovatorId)
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(inviteInnovatorUserModel.getEmail()))
                .body("name", equalTo(inviteInnovatorUserModel.getName()))
                .body("role", equalTo(inviteInnovatorUserModel.getRole()))
                .body("surname", equalTo(inviteInnovatorUserModel.getSurname()));
    }


    private String getInnovatorUserId(final String email) {
        return TestHelper.ensureAsExpected(15,
                        () -> opc.services.innovator.InnovatorService.getInnovatorUsers(new GetInnovatorUserModel(email), innovatorToken), SC_OK)
                .jsonPath().getString("userdetails[0].id");
    }

    private static void createInnovator(InnovatorRole innovatorRole) {
        inviteInnovatorUserModel = InviteInnovatorUserModel.defaultInviteUserModel(innovatorRole);

        InnovatorService.inviteNewUser(inviteInnovatorUserModel, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        nonceAndInviteId = MailhogHelper.getUserInviteNonceAndInviteId(inviteInnovatorUserModel.getEmail());

        InnovatorService.validateInviteNewUser(new ValidateInnovatorInvite(nonceAndInviteId.getLeft(), inviteInnovatorUserModel.getEmail()), nonceAndInviteId.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);


        InnovatorService.consumeInviteInnovator(new ConsumeInviteInnovatorModel(nonceAndInviteId.getLeft(), inviteInnovatorUserModel.getEmail(),
                        new PasswordModel(TestHelper.DEFAULT_INNOVATOR_PASSWORD)), nonceAndInviteId.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    private static String getInnovatorName(final String email,
                                           final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getUsers(token),
                        SC_OK)
                .path("userDetails.findAll{user -> user.email=='" + email + "'}.name").toString().replace("[", "").replace("]", "");
    }


    private static String getInnovatorRole(final String email,
                                           final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getUsers(token),
                        SC_OK)
                .path("userDetails.findAll{user -> user.email=='" + email + "'}.role").toString().replace("[", "").replace("]", "");
    }

    private static String getInnovatorSurname(final String email,
                                              final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getUsers(token),
                        SC_OK)
                .path("userDetails.findAll{user -> user.email=='" + email + "'}.surname").toString().replace("[", "").replace("]", "");
    }

    private static String getInnovatorId(final String email,
                                         final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.getUsers(token),
                        SC_OK)
                .path("userDetails.findAll{user -> user.email=='" + email + "'}.id").toString().replace("[", "").replace("]", "");
    }


}
