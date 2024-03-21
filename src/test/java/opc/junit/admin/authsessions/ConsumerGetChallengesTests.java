package opc.junit.admin.authsessions;

import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerGetChallengesTests extends AbstractGetChallengesTests {
    private static String identityToken;
    private static String identityId;
    private static String identityManagedAccountId;
    private static String identityManagedCardId;
    private static String identityCurrency;

    @BeforeAll
    public static void IdentitySetup(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secretKey);
        identityId = authenticatedConsumer.getLeft();
        identityToken = authenticatedConsumer.getRight();
        identityCurrency = createConsumerModel.getBaseCurrency();
        identityManagedAccountId = createManagedAccount(consumerManagedAccountProfileId,
                identityCurrency, identityToken).getLeft();

        final CreateConsumerModel destinationConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> destinationConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(destinationConsumerModel, secretKey);
        identityManagedCardId = createPrepaidManagedCard(consumerPrepaidManagedCardsProfileId,
                destinationConsumerModel.getBaseCurrency(), destinationConsumer.getRight()).getLeft();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(identityId, secretKey, identityToken);
        fundManagedAccount(identityManagedAccountId, identityCurrency, 100000L);
    }

    @Override
    public String getIdentityToken() {
        return identityToken;
    }

    @Override
    public String getIdentityId() {
        return identityId;
    }

    @Override
    public String getIdentityManagedAccountId() {
        return identityManagedAccountId;
    }

    @Override
    public String getIdentityManagedCardId() {
        return identityManagedCardId;
    }

    @Override
    public String getIdentityCurrency() {
        return identityCurrency;
    }
}
