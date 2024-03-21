package opc.junit.multi.webhooks;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.shared.Identity;
import opc.models.webhook.WebhookLoginPasswordEventModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag(MultiTags.IDENTITY_WEBHOOKS)
@Tag(MultiTags.WEBHOOKS_PARALLEL)
public class AccessTokenWebhookTests extends BaseWebhooksSetup {

    @Test
    public void Corporate_RootUserCreateAccessToken_Success() throws InterruptedException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                corporateProfileId, secretKey);

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, corporate.getRight());

        final WebhookLoginPasswordEventModel event = getWebhookResponse(timestamp, corporate.getLeft());

        assertLoginEvent(corporate.getLeft(), corporate.getLeft(), UserType.ROOT.name(),
                IdentityType.CORPORATE.getValue(), "VERIFIED", "TOKEN", event);
    }

    @Test
    public void Consumer_RootUserCreateAccessToken_Success() throws InterruptedException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                consumerProfileId, secretKey);

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(consumer.getLeft(), IdentityType.CONSUMER)),
                secretKey, consumer.getRight());

        final WebhookLoginPasswordEventModel event = getWebhookResponse(timestamp, consumer.getLeft());

        assertLoginEvent(consumer.getLeft(), consumer.getLeft(), UserType.ROOT.name(),
                IdentityType.CONSUMER.getValue(), "VERIFIED", "TOKEN", event);
    }

    @Test
    public void Corporate_AuthorizedUserCreateAccessToken_Success() throws InterruptedException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                corporateProfileId, secretKey);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
                corporate.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(corporate.getLeft(), IdentityType.CORPORATE)),
                secretKey, user.getRight());

        final WebhookLoginPasswordEventModel event = getWebhookResponse(timestamp, user.getLeft());

        assertLoginEvent(user.getLeft(), corporate.getLeft(), UserType.USER.name(),
                IdentityType.CORPORATE.getValue(), "VERIFIED", "TOKEN", event);
    }

    @Test
    public void Consumer_AuthorizedUserCreateAccessToken_Success() throws InterruptedException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                consumerProfileId, secretKey);

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey,
                consumer.getRight());

        TimeUnit.SECONDS.sleep(1);

        final long timestamp = Instant.now().toEpochMilli();

        AuthenticationHelper.requestAccessToken(
                new Identity(new IdentityModel(consumer.getLeft(), IdentityType.CONSUMER)),
                secretKey, user.getRight());

        final WebhookLoginPasswordEventModel event = getWebhookResponse(timestamp, user.getLeft());

        assertLoginEvent(user.getLeft(), consumer.getLeft(), UserType.USER.name(),
                IdentityType.CONSUMER.getValue(), "VERIFIED", "TOKEN", event);
    }

    private WebhookLoginPasswordEventModel getWebhookResponse(final long timestamp,
                                                              final String userId) {
        return (WebhookLoginPasswordEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookServiceDetails.getLeft(),
                timestamp,
                WebhookType.LOGIN,
                Pair.of("credential.id", userId),
                WebhookLoginPasswordEventModel.class,
                ApiSchemaDefinition.LoginEvent);
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
        assertNotNull(loginEvent.getPublishedTimestamp());
    }
}
