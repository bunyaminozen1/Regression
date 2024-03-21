package opc.junit.multi.access;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerLoginWithBiometricTests extends AbstractLoginWithBiometricTests {

    @Override
    protected IdentityDetails getBiometricIdentity(final ProgrammeDetailsModel programme) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createBiometricEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey(), programme.getSharedKey());

        return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(), consumer.getLeft(),
                consumer.getRight(), IdentityType.CONSUMER, createConsumerModel.getRootUser().getName(),
                createConsumerModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(), consumer.getLeft(),
                consumer.getRight(), IdentityType.CONSUMER, createConsumerModel.getRootUser().getName(),
                createConsumerModel.getRootUser().getSurname());
    }

    @Override
    protected CreateManagedAccountModel getManagedAccountModel(final ProgrammeDetailsModel programme) {
        return CreateManagedAccountModel
                .DefaultCreateManagedAccountModel(programme.getConsumerPayneticsEeaManagedAccountsProfileId(), Currency.EUR.name())
                .build();
    }

    @Override
    protected CreateManagedCardModel getManagedCardModel(final ProgrammeDetailsModel programme) {
        return CreateManagedCardModel
                .DefaultCreatePrepaidManagedCardModel(programme.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), Currency.EUR.name())
                .build();
    }
}
