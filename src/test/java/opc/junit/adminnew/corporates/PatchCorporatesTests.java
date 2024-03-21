package opc.junit.adminnew.corporates;

import opc.enums.opc.KybState;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.admin.UpdateKybModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

public class PatchCorporatesTests extends BaseCorporatesSetup {

    private CreateCorporateModel createCorporateModel;
    private String corporateId;
    private String adminToken;

    @BeforeEach
    public void Setup() {
        createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        adminToken = AdminService.loginAdmin();
    }

    @Test
    public void PatchCorporateKyb_EmailNotVerified_Success() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified("APPROVED")
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_OK)
                .body("fullCompanyChecksVerified", is(KybState.APPROVED.name()))
                .body("rootEmailVerified", equalTo(false))
                .body("rootMobileVerified", equalTo(true));
    }

    @Test
    public void PatchCorporateKyb_CorporateMobileAndEmailNotVerified_Success() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(false)
                .setRootMobileVerified(false)
                .setFullCompanyChecksVerified(KybState.APPROVED.name())
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_OK)
                .body("fullCompanyChecksVerified", is(KybState.APPROVED.name()))
                .body("rootEmailVerified", equalTo(false))
                .body("rootMobileVerified", equalTo(false));

    }

    @Test
    public void PatchCorporateKyb_CorporateMobileAndEmailVerifiedToPendingState_Success() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified(KybState.PENDING_REVIEW.name())
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_OK)
                .body("fullCompanyChecksVerified", is(KybState.PENDING_REVIEW.name()))
                .body("rootEmailVerified", equalTo(true))
                .body("rootMobileVerified", equalTo(true));
    }

    @Test
    public void PatchCorporateKyb_CorporateMobileAndEmailVerifiedToInitiated_Success() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setRootEmailVerified(true)
                .setRootMobileVerified(true)
                .setFullCompanyChecksVerified(KybState.INITIATED.name())
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_OK)
                .body("fullCompanyChecksVerified", is(KybState.INITIATED.name()))
                .body("rootEmailVerified", equalTo(true))
                .body("rootMobileVerified", equalTo(true));
    }

    @Test
    public void PatchCorporateKyb_KybNotStarted_Success() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setFullCompanyChecksVerified(KybState.NOT_STARTED.name())
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_OK)
                .body("fullCompanyChecksVerified", is(KybState.NOT_STARTED.name()));
    }

    @Test
    public void PatchCorporateKyb_NoToken_Unauthorised() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setFullCompanyChecksVerified(KybState.NOT_STARTED.name())
                .build();

        AdminService.updateCorporateKyb(updateKybModel, " ", corporateId)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
    @Test
    public void PatchCorporateKyb_IncorrectState_BadRequest() {

        final UpdateKybModel updateKybModel = UpdateKybModel.builder()
                .setFullCompanyChecksVerified(RandomStringUtils.randomAlphabetic(10))
                .build();

        AdminService.updateCorporateKyb(updateKybModel, adminToken, corporateId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

}
