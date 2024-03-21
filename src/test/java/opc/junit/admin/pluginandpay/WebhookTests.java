package opc.junit.admin.pluginandpay;

import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.enums.opc.WebhookType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.innovatornew.pluginandpay.BasePluginAndPaySetup;
import opc.models.admin.CreatePluginModel;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.webhook.WebhookLoginPasswordEventModel;
import opc.models.webhookcallback.WebhookCallbackModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.lang.Long.parseLong;
import static opc.services.admin.AdminService.createPlugin;
import static opc.services.innovator.InnovatorService.updateProgramme;
import static opc.services.webhookcallback.WebhookCallbackService.getWebhookCallback;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WebhookTests extends BasePluginAndPaySetup {
    final Pair<String, String> webhookUrl = WebhookHelper.generateWebhookUrl();

    @Test
    public void getWebhookCallback_HappyPath() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final CreatePluginModel createPluginModel = CreatePluginModel.defaultCreatePluginModel();
        final String fpiKey = createPlugin(createPluginModel, adminToken).jsonPath().get("fpiKey");

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final String programmeId = AdminService.createProgramme(tenantAdminToken, createProgrammeModel).jsonPath().get("id");

        final String identityId = corporate.getLeft();

        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setName(RandomStringUtils.randomAlphabetic(10))
                        .setWebhookUrl(webhookUrl.getRight())
                        .setWebhookDisabled(false)
                        .setAuthForwardingEnabled(false)
                        .build();

        updateProgramme(updateProgrammeModel, programmeId, innovatorToken).then().statusCode(SC_OK);

        final WebhookCallbackModel webhookCallbackModel = WebhookCallbackModel.defaultWebhookCallbackModel().build();
        webhookCallbackModel.setProgrammeId(parseLong(programmeId));

        final long timestamp = Instant.now().toEpochMilli();

        getWebhookCallback(webhookCallbackModel, fpiKey)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("tenantId", equalTo(Integer.parseInt(tenantId)))
                .body("programmeId", equalTo(parseLong(programmeId)))
                .body("operation", equalTo(webhookCallbackModel.getOperation()))
                .body("eventType", equalTo("Login"))
                .body("payload", notNullValue())
                .body("attempts[0].attemptTimestampUTC", notNullValue())
                .body("attempts[0].durationMillis", notNullValue())
                .body("attempts[0].trigger", equalTo("INITIAL_ATTEMPT"))
                .body("attempts[0].httpStatus", equalTo(200));

        final WebhookLoginPasswordEventModel loginEvent = getWebhookResponse(timestamp, identityId);

        assertLoginEvent(identityId, identityId, UserType.ROOT.name(),
                IdentityType.CORPORATE.getValue(), "VERIFIED", "PASSWORD", loginEvent);
    }

    @Test
    public void getWebhookCallback_UnknownFpiKey_Conflict() {
        final String fpiKey = "0000000000";

        final CreateProgrammeModel createProgrammeModel = CreateProgrammeModel.InitialProgrammeModel();
        final String programmeId = AdminService.createProgramme(tenantAdminToken, createProgrammeModel).jsonPath().get("id");

        final UpdateProgrammeModel updateProgrammeModel =
                UpdateProgrammeModel.builder()
                        .setName(RandomStringUtils.randomAlphabetic(10))
                        .setWebhookUrl(webhookUrl.getRight())
                        .setWebhookDisabled(false)
                        .setAuthForwardingEnabled(false)
                        .build();

        updateProgramme(updateProgrammeModel, programmeId, innovatorToken).then().statusCode(SC_OK);

        final WebhookCallbackModel webhookCallbackModel = WebhookCallbackModel.defaultWebhookCallbackModel().build();
        webhookCallbackModel.setProgrammeId(parseLong(programmeId));

        getWebhookCallback(webhookCallbackModel, fpiKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("message", equalTo("Fpi-key is not valid"));
    }

    private WebhookLoginPasswordEventModel getWebhookResponse(final long timestamp,
                                                              final String identityId) {
        return (WebhookLoginPasswordEventModel) WebhookHelper.getWebhookServiceEvent(
                webhookUrl.getLeft(),
                timestamp,
                WebhookType.LOGIN,
                Pair.of("identity.id", identityId),
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
    }
}
