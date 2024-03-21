package opc.junit.innovatornew.corporates;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.services.innovatornew.InnovatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;

public class DeactivateCorporateUserTests extends BaseCorporatesSetup{

    @Test
    public void DeactivateCorporateUser_TenantNotSetInConfiguration_Forbidden(){

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        //Attempt to deactivate root user
        InnovatorService.deactivateCorporateUser(corporate.getLeft(), corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Attempt to deactivate authorized user
        InnovatorService.deactivateCorporateUser(corporate.getLeft(), user.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }
}
