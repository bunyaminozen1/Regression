package opc.junit.multi.corporates;

import opc.enums.opc.IdentityType;
import opc.enums.opc.KybState;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.is;

public class GetCorporatesKybTests extends BaseCorporatesSetup {

    private String corporateId;
    private String authenticationToken;

    @BeforeEach
    public void Setup(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        authenticationToken = authenticatedCorporate.getRight();
    }

    @Test
    public void GetCorporatesKyb_NotStarted_Success(){
        CorporatesService.getCorporateKyb(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", is(KybState.NOT_STARTED.name()));

    }

    @Test
    public void GetCorporatesKyb_Initiated_Success(){
        CorporatesService.startCorporateKyb(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK);

        CorporatesService.getCorporateKyb(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", is(KybState.INITIATED.name()));

    }

    @Test
    public void GetCorporatesKyb_Approved_Success(){
        CorporatesService.startCorporateKyb(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK);

        SimulatorService.simulateKybApproval(secretKey, corporateId)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporateKyb(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", is(KybState.APPROVED.name()));
    }

    @Test
    public void GetCorporatesKyb_InvalidApiKey_Unauthorised(){

        CorporatesService.getCorporateKyb("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporatesKyb_NoApiKey_BadRequest(){

        CorporatesService.getCorporateKyb("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetCorporatesKyb_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        CorporatesService.getCorporateKyb(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetCorporatesKyb_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        CorporatesService.getCorporateKyb(secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetCorporatesKyb_BackofficeImpersonator_Forbidden(){
        CorporatesService.getCorporateKyb(secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);

    }
}