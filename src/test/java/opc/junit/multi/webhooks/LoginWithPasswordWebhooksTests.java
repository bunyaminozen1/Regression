package opc.junit.multi.webhooks;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.webhook.WebhookLoginPasswordEventModel;
import opc.services.multi.AuthenticationService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(MultiTags.IDENTITY_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class LoginWithPasswordWebhooksTests extends BaseWebhooksSetup {

    final static String PASSWORD = TestHelper.getDefaultPassword(secretKey);

    @Test
    public void Corporate_RootUserLoginWithPassword_Verified() throws InterruptedException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                corporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, secretKey);

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(corporateRootEmail, new PasswordModel(PASSWORD)),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, corporate.getLeft());

        assertLoginEvent(corporate.getLeft(), corporate.getLeft(), UserType.ROOT.name(),
                IdentityType.CORPORATE.getValue(), "VERIFIED", "PASSWORD", loginEvent);
    }


    @Test
    public void Consumer_RootUserLoginWithPassword_Verified() throws InterruptedException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(
                consumerProfileId).build();
        final String consumerRootEmail = createConsumerModel.getRootUser().getEmail();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                createConsumerModel, secretKey);

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(consumerRootEmail, new PasswordModel(PASSWORD)),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, consumer.getLeft());

        assertLoginEvent(consumer.getLeft(), consumer.getLeft(), UserType.ROOT.name(),
                IdentityType.CONSUMER.getValue(), "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void Corporate_AuthenticatedUserLoginWithPassword_Verified() throws InterruptedException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                corporateProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userEmail = usersModel.getEmail();

        final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(usersModel,
                secretKey, corporate.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(userEmail, new PasswordModel(PASSWORD)),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, corporate.getLeft());

        assertLoginEvent(authenticatedUser.getLeft(), corporate.getLeft(), UserType.USER.name(),
                IdentityType.CORPORATE.getValue(), "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void Consumer_AuthenticatedUserLoginWithPassword_Verified() throws InterruptedException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userEmail = usersModel.getEmail();

        final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(usersModel,
                secretKey, consumer.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(userEmail, new PasswordModel(PASSWORD)),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, consumer.getLeft());

        assertLoginEvent(authenticatedUser.getLeft(), consumer.getLeft(), UserType.USER.name(),
                IdentityType.CONSUMER.getValue(), "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void RootUser_LoginWithInvalidPassword_Declined() throws InterruptedException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                corporateProfileId).build();
        final String corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, secretKey);

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(corporateRootEmail, new PasswordModel(RandomStringUtils.randomAlphanumeric(5))),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, corporate.getLeft());

        assertLoginEvent(corporate.getLeft(), corporate.getLeft(), UserType.ROOT.name(),
                IdentityType.CORPORATE.getValue(), "DECLINED", "PASSWORD", loginEvent);
    }

    @Test
    public void AuthenticatedUser_LoginWithInvalidPassword_Declined() throws InterruptedException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userEmail = usersModel.getEmail();

        final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(usersModel,
                secretKey, consumer.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationService.loginWithPassword(
                new LoginModel(userEmail, new PasswordModel(RandomStringUtils.randomAlphanumeric(5))),
                secretKey);

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, consumer.getLeft());

        assertLoginEvent(authenticatedUser.getLeft(), consumer.getLeft(), UserType.USER.name(),
                IdentityType.CONSUMER.getValue(), "DECLINED", "PASSWORD", loginEvent);
    }


    private void assertLoginEvent(final String userId,
                                  final String identityId,
                                  final String userType,
                                  final String identityType,
                                  final String status,
                                  final String eventType,
                                  final WebhookLoginPasswordEventModel loginEvent) {

        assertEquals(userId, loginEvent.getCredential().get("id"));
        assertEquals(userType, loginEvent.getCredential().get("type"));
        assertEquals(identityId, loginEvent.getIdentity().get("id"));
        assertEquals(identityType, loginEvent.getIdentity().get("type"));
        assertEquals(status, loginEvent.getStatus());
        assertEquals(eventType, loginEvent.getType());
    }

    private WebhookLoginPasswordEventModel getWebhookResponse(final long timestamp,
                                                              final String identityId) {
        return (WebhookLoginPasswordEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.LOGIN,
                Pair.of("identity.id", identityId),
                WebhookLoginPasswordEventModel.class,
                ApiSchemaDefinition.LoginEvent);
    }
}
