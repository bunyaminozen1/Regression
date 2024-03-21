package opc.junit.innovator.corporates;

import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.innovator.UpdateCorporateModel;
import opc.models.innovator.UpdateCorporateUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.AddressModel;
import opc.services.admin.AdminService;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClosedIdentityFlagTests extends BaseCorporatesSetup{

    private static CreateCorporateModel createCorporateModel;
    private static Pair<String, String> corporate;
    private static Pair<String, String> authorizedUser;

    @BeforeAll
    public static void setup() throws SQLException {

        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        authorizedUser = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        AdminHelper.deactivateCorporateUser(corporate.getLeft(), authorizedUser.getLeft(), impersonatedAdminToken);
        AdminHelper.deactivateCorporateUser(corporate.getLeft(), corporate.getLeft(), impersonatedAdminToken);

        AdminHelper.deactivateCorporate(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), corporate.getLeft(), impersonatedAdminToken);

        AdminService.corporateIdentityClosure(corporate.getLeft(), impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);

        assertEquals("1", CorporatesDatabaseHelper.getCorporate(corporate.getLeft()).get(0).get("permanently_closed"));
    }

    @Test
    public void IdentityClosureFlag_GetAllCorporates_Success(){
        final CorporatesFilterModel filterModel = CorporatesFilterModel.builder()
                .setRootEmail(createCorporateModel.getRootUser().getEmail())
                .build();

        InnovatorService.getCorporates(filterModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("corporateWithKyb[0].corporate.id.id", equalTo(corporate.getLeft()))
                .body("corporateWithKyb[0].corporate.permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_FilterAllPermanentlyClosedCorporates_Success(){
        final CorporatesFilterModel filterModel = CorporatesFilterModel.builder()
                .setPermanentlyClosed("TRUE")
                .build();

        InnovatorService.getCorporates(filterModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("corporateWithKyb[0].corporate.permanentlyClosed", equalTo(true))
                .body("corporateWithKyb[-1].corporate.permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_GetCorporate_Success(){

        InnovatorService.getCorporate(innovatorToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(corporate.getLeft()))
                .body("rootUser.name", equalTo(createCorporateModel.getRootUser().getName()))
                .body("rootUser.surname", equalTo(createCorporateModel.getRootUser().getSurname()))
                .body("rootUser.email", equalTo(createCorporateModel.getRootUser().getEmail()))
                .body("permanentlyClosed", equalTo(true));

    }

    @Test
    public void IdentityClosureFlag_UpdateCorporates_Success(){

        final UpdateCorporateModel updateCorporateModel = UpdateCorporateModel.builder().businessAddress(AddressModel.RandomAddressModel()).build();

        InnovatorService.updateCorporate(updateCorporateModel, innovatorToken, corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id.id", equalTo(corporate.getLeft()))
                .body("businessAddress.addressLine1", equalTo(updateCorporateModel.getBusinessAddress().getAddressLine1()))
                .body("businessAddress.addressLine2", equalTo(updateCorporateModel.getBusinessAddress().getAddressLine2()))
                .body("businessAddress.city", equalTo(updateCorporateModel.getBusinessAddress().getCity()))
                .body("businessAddress.country", equalTo(updateCorporateModel.getBusinessAddress().getCountry()))
                .body("businessAddress.postCode", equalTo(updateCorporateModel.getBusinessAddress().getPostCode()))
                .body("businessAddress.state", equalTo(updateCorporateModel.getBusinessAddress().getState()))
                .body("permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_ActivateCorporate_ClosedIdentity(){
        InnovatorService.activateCorporate(new ActivateIdentityModel(true), corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));
    }

    @Test
    public void IdentityClosureFlag_UpdateCorporateUser_ClosedIdentity(){

        final UpdateCorporateUserModel model = UpdateCorporateUserModel.builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(5)).build();

        //Update root user
        InnovatorService.updateCorporateUser(model, innovatorToken, corporate.getLeft(), corporate.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(corporate.getLeft()))
                .body("name", equalTo(model.getName()))
                .body("surname", equalTo(model.getSurname()))
                .body("permanentlyClosed", equalTo(true));

        //Update authorized user
        InnovatorService.updateCorporateUser(model, innovatorToken, corporate.getLeft(), authorizedUser.getLeft())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(authorizedUser.getLeft()))
                .body("name", equalTo(model.getName()))
                .body("surname", equalTo(model.getSurname()))
                .body("permanentlyClosed", equalTo(true));
    }

    @Test
    public void IdentityClosureFlag_ActivateCorporateUser_ClosedIdentity(){

        //Try to activate root user
        InnovatorService.activateCorporateUser( corporate.getLeft(), corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));

        //Try to activate authorized user
        InnovatorService.activateCorporateUser( corporate.getLeft(), corporate.getLeft(), innovatorToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CLOSED_IDENTITY"));
    }
}
