package opc.junit.innovator.invitenewinnovator;

import opc.enums.opc.InnovatorRole;
import opc.models.innovator.InviteInnovatorUserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static opc.services.innovator.InnovatorService.inviteNewUser;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class InviteInnovatorTests extends BaseInnovatorSetup {

    final private static String script = "112>*<script>alert()</script>";

    @Test
    public void InviteNewInnovator_HappyPath_Success() {

        final InviteInnovatorUserModel inviteInnovatorUserModel = InviteInnovatorUserModel.builder()
                .name(RandomStringUtils.randomAlphabetic(5))
                .surname(RandomStringUtils.randomAlphabetic(5))
                .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphabetic(10)))
                .role(InnovatorRole.getRandomInnovatorRole().toString())
                .build();

        inviteNewUser(inviteInnovatorUserModel, innovatorToken)
                .then()
                .statusCode(SC_NO_CONTENT);

    }

    @Test
    public void InviteNewInnovator_ScriptInName_BadRequest() {

        final InviteInnovatorUserModel inviteInnovatorUserModel = InviteInnovatorUserModel.builder()
                .name(script)
                .surname(RandomStringUtils.randomAlphabetic(5))
                .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphabetic(10)))
                .role(InnovatorRole.getRandomInnovatorRole().toString())
                .build();

        inviteNewUser(inviteInnovatorUserModel, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void InviteNewInnovator_ScriptInSurname_BadRequest() {

        final InviteInnovatorUserModel inviteInnovatorUserModel = InviteInnovatorUserModel.builder()
                 .name(RandomStringUtils.randomAlphabetic(5))
                 .surname(script)
                 .email(String.format("%s@fakemail.com", RandomStringUtils.randomAlphabetic(10)))
                 .role(InnovatorRole.getRandomInnovatorRole().toString())
                 .build();

        inviteNewUser(inviteInnovatorUserModel, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }
}
