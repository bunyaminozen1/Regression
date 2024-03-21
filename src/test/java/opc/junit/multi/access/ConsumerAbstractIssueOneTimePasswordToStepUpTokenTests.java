package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class ConsumerAbstractIssueOneTimePasswordToStepUpTokenTests extends AbstractIssueOneTimePasswordToStepUpTokenTests {

    @Override
    protected IdentityDetails getEnrolledIdentity(ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(), consumer.getLeft(),
                consumer.getRight(), IdentityType.CONSUMER, createConsumerModel.getRootUser().getName(),
                createConsumerModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createConsumerModel.getRootUser().getEmail(), consumer.getLeft(),
                consumer.getRight(), IdentityType.CONSUMER, createConsumerModel.getRootUser().getName(),
                createConsumerModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails createEnrolledUser(final String identityToken) {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, secretKey, identityToken);

        return IdentityDetails.generateDetails(usersModel.getEmail(), user.getLeft(),
                user.getRight(), IdentityType.CORPORATE, usersModel.getName(), usersModel.getSurname());
    }
}
