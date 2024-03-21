package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class CorporateAbstractIssueOneTimePasswordToStepUpTokenTests extends AbstractIssueOneTimePasswordToStepUpTokenTests {

    @Override
    protected IdentityDetails getEnrolledIdentity(ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), IdentityType.CORPORATE, createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(), corporate.getLeft(),
                corporate.getRight(), IdentityType.CORPORATE, createCorporateModel.getRootUser().getName(),
                createCorporateModel.getRootUser().getSurname());
    }

    @Override
    protected IdentityDetails createEnrolledUser(final String identityToken) {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createEnrolledAuthenticatedUser(usersModel, secretKey, identityToken);

        return IdentityDetails.generateDetails(usersModel.getEmail(), user.getLeft(),
                user.getRight(), IdentityType.CORPORATE, usersModel.getName(), usersModel.getSurname());
    }
}
