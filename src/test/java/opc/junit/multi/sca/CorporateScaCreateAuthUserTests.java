package opc.junit.multi.sca;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class CorporateScaCreateAuthUserTests extends AbstractScaCreateAuthUserTests{

    @Override
    protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel
                .DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, programme.getSecretKey());

        return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(),
                corporate.getLeft(), corporate.getRight(), IdentityType.CORPORATE, null, null);
    }
}
