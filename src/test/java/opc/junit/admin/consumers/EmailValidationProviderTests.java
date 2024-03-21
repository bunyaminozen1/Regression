package opc.junit.admin.consumers;

import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.admin.UpdateEmailValidationProviderModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.services.adminnew.AdminService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.SQLException;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
public class EmailValidationProviderTests extends BaseConsumersSetup {

    @AfterEach
    public void updateEmailValidationAsWeavr() throws SQLException {
        ConsumersDatabaseHelper.updateEmailValidationProvider(consumerProfileId, "WEAVR");
    }

    @Test
    public void ConsumersProfile_CheckDefaultValueIsWeavr_Success() {

        getEmailProvider(applicationTwo.getProgrammeId(), applicationTwo.getConsumersProfileId(), "WEAVR");

    }

    @Test
    public void Consumers_UpdateEmailValidationProviderAsEmbedder_Success() {

        getEmailProvider(programmeId, consumerProfileId, "WEAVR");

        updateEmailProvider("EMBEDDER", "EMBEDDER");
        getEmailProvider(programmeId, consumerProfileId, "EMBEDDER");

        // It should be embedder because it is not allowed to change embedder to weavr
        updateEmailProvider("WEAVR", "EMBEDDER");
        getEmailProvider(programmeId, consumerProfileId, "EMBEDDER");
    }

    @Test
    public void Consumers_CreateIdentityProviderEmbedderEmailAlreadyVerified_Success() throws SQLException {

        final CreateConsumerModel consumerModelWeavr = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerWeavrId = ConsumersHelper.createConsumer(consumerModelWeavr, secretKey);
        assertEquals("0", ConsumersDatabaseHelper.getConsumer(consumerWeavrId).get(0).get("email_address_verified"));

        updateEmailProvider("EMBEDDER", "EMBEDDER");

        final CreateConsumerModel consumerModelEmbedder = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerEmbedderId = ConsumersHelper.createConsumer(consumerModelEmbedder, secretKey);
        assertEquals("1", ConsumersDatabaseHelper.getConsumer(consumerEmbedderId).get(0).get("email_address_verified"));
    }

    @Test
    public void Consumers_UpdateEmailIdentityProviderEmbedder_Success() {

        updateEmailProvider("EMBEDDER", "EMBEDDER");

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
                PatchConsumerModel.newBuilder()
                        .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .build();

        // Email should be updated because there is no verification step to be handled
        ConsumersService.patchConsumer(patchConsumerModel, secretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.email", equalTo(patchConsumerModel.getEmail()))
                .body("rootUser.active", equalTo(true))
                .body("rootUser.emailVerified", equalTo(true));
    }

    @Test
    public void Consumers_CreateIdentityWithNotAllowedDomainProviderEmbedder_EmailDomainNotAllowed() {

        updateEmailProvider("EMBEDDER", "EMBEDDER");

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(String.format("%s%s@gav0.com", System.currentTimeMillis(),
                                        RandomStringUtils.randomAlphabetic(5)))
                                .build())
                        .build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_DOMAIN_NOT_ALLOWED"));
    }

    private void updateEmailProvider(final String requestedProvider, final String updatedProvider) {

        TestHelper.ensureAsExpected(15,
                () -> opc.services.adminnew.AdminService.updateEmailValidationProvider(new UpdateEmailValidationProviderModel(requestedProvider),
                        adminImpersonatedToken, programmeId, consumerProfileId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("emailValidationProvider").equals(updatedProvider),
                Optional.of(String.format("EmailValidationProvider is not %s, see logged payload", updatedProvider)));
    }

    private void getEmailProvider(final String programmeId, final String profileId, final String expectedProvider){

        TestHelper.ensureAsExpected(15,
                () -> AdminService.getConsumerProfile(adminImpersonatedToken, programmeId, profileId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("emailValidationProvider").equals(expectedProvider),
                Optional.of(String.format("EmailValidationProvider is not %s, see logged payload", expectedProvider)));
    }
}
