package opc.junit.innovator.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public class ConsumersPasscodeLengthTests extends AbstractPasscodeLengthTests{

    @Override
    protected IdentityDetails getPasswordCreatedIdentity(ProgrammeDetailsModel programme) {
        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(consumerModel.getRootUser().getEmail(), consumer.getLeft(), consumer.getRight(),
                IdentityType.CONSUMER, null, null);
    }

    @Override
    protected IdentityDetails getPasswordCreatedUser(ProgrammeDetailsModel programme) {
        final IdentityDetails identity = getPasswordCreatedIdentity(programme);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, programme.getSecretKey(), identity.getToken());

        return IdentityDetails.generateDetails(usersModel.getEmail(), user.getLeft(),
                user.getRight(), IdentityType.CONSUMER, null,null);
    }

    @Override
    protected IdentityDetails getWithoutPasswordIdentity(ProgrammeDetailsModel programme) {

        final CreateConsumerModel consumerModel = CreateConsumerModel.DefaultCreateConsumerModel(programme.getConsumersProfileId()).build();

        final String identityId = ConsumersService.createConsumer(consumerModel, programme.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id.id");

        return IdentityDetails.generateDetails(consumerModel.getRootUser().getEmail(), identityId,
                null, IdentityType.CONSUMER, null, null);
    }

    @Override
    protected IdentityDetails getInvitedUser(ProgrammeDetailsModel programme) {
        final IdentityDetails identity = getPasswordCreatedIdentity(programme);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(programme.getSecretKey(), identity.getToken());
        UsersHelper.inviteUser(programme.getSecretKey(), userId, identity.getToken());

        return IdentityDetails.generateDetails(usersModel.getEmail(), userId,
                null, IdentityType.CONSUMER, null,null);
    }
}
