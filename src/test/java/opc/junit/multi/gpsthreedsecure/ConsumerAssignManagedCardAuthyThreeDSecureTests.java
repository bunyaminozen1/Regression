package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.testmodels.UnassignedManagedCardDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class ConsumerAssignManagedCardAuthyThreeDSecureTests extends AbstractAssignManagedCardAuthyThreeDSecureTests{
    private static String biometricIdentityToken;
    private static List<UnassignedManagedCardDetails> biometricIdentityUnassignedCards;
    private static String authyIdentityToken;
    private static List<UnassignedManagedCardDetails> authyIdentityUnassignedCards;

    @BeforeAll
    public static void IdentitySetup() {
        //    setup identity not enabled for Authy
        final CreateConsumerModel threeDSConsumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(threeDSConsumerProfileId).build();

        final Pair<String, String> threeDSAuthenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                threeDSConsumerDetails, secretKey);
        biometricIdentityToken = threeDSAuthenticatedConsumer.getRight();
        final String threeDSIdentityCurrency = threeDSConsumerDetails.getBaseCurrency();

        //    setup identity enabled for Authy
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                consumerDetails, secretKey);
        authyIdentityToken = authenticatedConsumer.getRight();
        final String identityCurrency = consumerDetails.getBaseCurrency();

        final String identityManagedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, identityCurrency, authyIdentityToken);
        final String threeDSIdentityManagedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, threeDSIdentityCurrency, biometricIdentityToken);

        authyIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(identityManagedAccountId, consumerPrepaidManagedCardsProfileId,
                        consumerDebitManagedCardsProfileId, identityCurrency, CardLevelClassification.CONSUMER, innovatorToken);
        biometricIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(threeDSIdentityManagedAccountId, consumerPrepaidManagedCardsProfileId,
                        consumerDebitManagedCardsProfileId, threeDSIdentityCurrency, CardLevelClassification.CONSUMER, innovatorToken);
    }

    @Override
    protected String getBiometricIdentityToken() {
        return biometricIdentityToken;
    }

    @Override
    protected String getAuthyIdentityToken() {
        return authyIdentityToken;
    }

    @Override
    public List<UnassignedManagedCardDetails> getBiometricIdentityUnassignedCards() {
        return biometricIdentityUnassignedCards;
    }

    @Override
    public List<UnassignedManagedCardDetails> getAuthyIdentityUnassignedCards() {
        return authyIdentityUnassignedCards;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }
}
