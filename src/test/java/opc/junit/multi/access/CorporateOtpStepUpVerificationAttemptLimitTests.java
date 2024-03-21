package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;

public class CorporateOtpStepUpVerificationAttemptLimitTests extends AbstractOtpStepUpVerificationAttemptLimitTests{

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
}
