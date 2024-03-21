package opc.junit.adminnew.corporates;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.services.adminnew.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class GetCorporateTests extends BaseCorporatesSetup {

    @Test
    public void GetCorporate_WithoutImpersonatedToken_Success(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.getCorporate(adminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("id.id", equalTo(corporate.getLeft()))
                .body("tenantId", equalTo(Integer.parseInt(tenantId)))
                .body("programmeId", equalTo(programmeId));
    }

    @Test
    public void GetCorporate_ImpersonatedToken_Success(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AdminService.getCorporate(impersonatedAdminToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo(IdentityType.CORPORATE.getValue()))
                .body("id.id", equalTo(corporate.getLeft()))
                .body("tenantId", equalTo(Integer.parseInt(tenantId)))
                .body("programmeId", equalTo(programmeId));
    }
}
