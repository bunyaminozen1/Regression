package opc.junit.innovator.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.multi.CorporatesService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public class CorporatesPasscodeLengthTests extends AbstractPasscodeLengthTests{

    @Override
    protected IdentityDetails getPasswordCreatedIdentity(ProgrammeDetailsModel programme) {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(corporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), IdentityType.CORPORATE, null,null);
    }

    @Override
    protected IdentityDetails getPasswordCreatedUser(ProgrammeDetailsModel programme) {
        final IdentityDetails identity = getPasswordCreatedIdentity(programme);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, programme.getSecretKey(), identity.getToken());

        return IdentityDetails.generateDetails(usersModel.getEmail(), user.getLeft(),
                user.getRight(), IdentityType.CORPORATE, null,null);
    }

    @Override
    protected IdentityDetails getWithoutPasswordIdentity(ProgrammeDetailsModel programme) {

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final String identityId = CorporatesService.createCorporate(corporateModel, programme.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id.id");

        return IdentityDetails.generateDetails(corporateModel.getRootUser().getEmail(), identityId,
                null, IdentityType.CORPORATE, null,null);
    }

    @Override
    protected IdentityDetails getInvitedUser(ProgrammeDetailsModel programme) {
        final IdentityDetails identity = getPasswordCreatedIdentity(programme);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(programme.getSecretKey(), identity.getToken());
        UsersHelper.inviteUser(programme.getSecretKey(), userId, identity.getToken());

        return IdentityDetails.generateDetails(usersModel.getEmail(), userId,
                null, IdentityType.CORPORATE, null,null);
    }
}
