package opc.junit.semi;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.CorporatesService;
import opc.services.multi.SemiService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CorporateDeactivatedSemiTests extends BaseSemiSetup {

    /**
     * SEMI flow for Disabling SEMI Corporates.
     * The expected result when disabling single or multiple linked identities
     * in SEMI logic, we expect the identities that were not disabled to still remain active and able to receive the access token.
     */

    private String identityId;
    private String corporateRootEmail;
    private String corporatePassword;
    private String name;
    private String surname;
    private String impersonatedAdminToken;


    @BeforeEach
    public void BeforeEach() {
        setupCorporate();
        impersonatedAdminToken = AdminService.impersonateTenant(tenantId, adminToken);
    }

    @Test
    public void DeactivateCorporate_FirstRootCorporateDeactivatedLinkedCorporatesActive_Success() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        //Deactivate first root corporate
        deactivateCorporate(firstSemiCorporate);

        //Expect to receive 'AUTH' token from login with password since 2 linked corporates are still active
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("tokenType", equalTo("AUTH"));

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Access Forbidden to the first root corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(firstSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Second linked corporate has access since it's not deactivated
        AuthenticationService.accessToken(new Identity(new IdentityModel(secondLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(secondLinkedSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());

        //Third linked corporate has access since it's not deactivated
        AuthenticationService.accessToken(new Identity(new IdentityModel(thirdLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(thirdLinkedSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());
    }

    @Test
    public void DeactivatedCorporate_FirstRootCorporateActiveLinkedCorporatesDeactivated_Success() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        //Deactivate second and third linked corporates
        deactivateCorporate(secondLinkedSemiCorporate);
        deactivateCorporate(thirdLinkedSemiCorporate);

        //Expect to receive 'ACCESS' token from login with password since 2 linked corporates are deactivated and only first root corporate is still active
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("credentials.type", equalTo("ROOT"))
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("identity.id", equalTo(firstSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("tokenType", equalTo("ACCESS"));

        final String accessToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //first root corporate has access since it's not deactivated
        AuthenticationService.accessToken(new Identity(new IdentityModel(firstSemiCorporate, IdentityType.CORPORATE)), secretKey, accessToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(firstSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());

        //Access Forbidden to second linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(secondLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, accessToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Access Forbidden to third linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(thirdLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, accessToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivatedCorporate_DeactivatedFirstCorporateActivated_Success() {
        final String firstSemiCorporate = identityId;
        createSemiCorporate();
        createSemiCorporate();

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Deactivate first root corporate
        deactivateCorporate(firstSemiCorporate);

        //Access Forbidden to the first root corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(firstSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Activate first root corporate
        activateCorporate(firstSemiCorporate);

        //First root corporate has access since it's activated again
        AuthenticationService.accessToken(new Identity(new IdentityModel(firstSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(firstSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());
    }

    @Test
    public void DeactivatedCorporate_DeactivatedLinkedCorporatesActivated_Success() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Deactivate second linked corporate
        deactivateCorporate(secondLinkedSemiCorporate);

        //Access Forbidden to the second linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(secondLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Activate second linked corporate
        activateCorporate(secondLinkedSemiCorporate);

        //Second linked corporate has access since it's activated again
        AuthenticationService.accessToken(new Identity(new IdentityModel(secondLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(secondLinkedSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());

        //Deactivate third linked corporate
        deactivateCorporate(thirdLinkedSemiCorporate);

        //Access Forbidden to the third linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(thirdLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        activateCorporate(thirdLinkedSemiCorporate);

        //Third linked corporate has access since it's activated again
        AuthenticationService.accessToken(new Identity(new IdentityModel(thirdLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_OK)
                .body("credentials.id", equalTo(firstSemiCorporate))
                .body("credentials.type", equalTo("ROOT"))
                .body("identity.id", equalTo(thirdLinkedSemiCorporate))
                .body("identity.type", equalTo("CORPORATE"))
                .body("token", notNullValue());
    }

    @Test
    public void DeactivatedCorporate_AllCorporatesDeactivated_Forbidden() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Deactivate all corporates
        deactivateCorporate(firstSemiCorporate);
        deactivateCorporate(secondLinkedSemiCorporate);
        deactivateCorporate(thirdLinkedSemiCorporate);

        //All corporates are deactivated, login with password returns forbidden
        final LoginModel loginModelWithIdentity = new LoginModel(corporateRootEmail, new PasswordModel(corporatePassword));
        AuthenticationService.loginWithPassword(loginModelWithIdentity, secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Access Forbidden to the first root corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(firstSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Access Forbidden to second linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(secondLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);

        //Access Forbidden to third linked corporate
        AuthenticationService.accessToken(new Identity(new IdentityModel(thirdLinkedSemiCorporate, IdentityType.CORPORATE)), secretKey, authToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetActiveIdentities_LinkedCorporatesDeactivated_Success() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Get linked identities
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(secondLinkedSemiCorporate))
                .body("id[2].id", equalTo(thirdLinkedSemiCorporate));

        //Deactivate second and third linked corporates
        deactivateCorporate(secondLinkedSemiCorporate);
        deactivateCorporate(thirdLinkedSemiCorporate);

        //Get linked identities and check that second and third linked deactivated corporates do not display
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(null))
                .body("id[2].id", equalTo(null));
    }

    @Test
    public void GetActiveIdentities_AllCorporatesDeactivated_Success() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        final String authToken = AuthenticationHelper.login(corporateRootEmail, corporatePassword, secretKey);

        //Get linked identities
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(secondLinkedSemiCorporate))
                .body("id[2].id", equalTo(thirdLinkedSemiCorporate));

        //Deactivate all corporates
        deactivateCorporate(firstSemiCorporate);
        deactivateCorporate(secondLinkedSemiCorporate);
        deactivateCorporate(thirdLinkedSemiCorporate);

        //Get linked identities and check that all deactivated corporates do not display
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(null))
                .body("id[1].id", equalTo(null))
                .body("id[2].id", equalTo(null));

        //Activate first semi corporate

        activateCorporate(firstSemiCorporate);

        //Get linked identities and check that the first semi corporate is displaying
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_OK)
                .body("id[0].id", equalTo(firstSemiCorporate))
                .body("id[1].id", equalTo(null))
                .body("id[2].id", equalTo(null));
    }

    @Test
    public void GetActiveIdentities_InvalidAuthToken_Unauthorized() {
        final String firstSemiCorporate = identityId;
        final String secondLinkedSemiCorporate = createSemiCorporate();
        final String thirdLinkedSemiCorporate = createSemiCorporate();

        final String authToken = RandomStringUtils.randomAlphabetic(34);

        //Get linked identities
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);

        //Deactivate all corporates
        deactivateCorporate(firstSemiCorporate);
        deactivateCorporate(secondLinkedSemiCorporate);
        deactivateCorporate(thirdLinkedSemiCorporate);

        //Get linked identities and check that all deactivated corporates do not display in the list
        SemiService.getLinkedIdentities(secretKey, Optional.empty(), authToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    //Methods for tests
    private void setupCorporate() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                corporatesProfileId).build();

        corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        name = createCorporateModel.getRootUser().getName();
        surname = createCorporateModel.getRootUser().getSurname();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, secretKey);
        identityId = authenticatedCorporate.getLeft();
        corporatePassword = TestHelper.getDefaultPassword(secretKey);

        CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
        CorporatesHelper.verifyKyb(secretKey, identityId);
    }

    public void activateCorporate(final String corporateId) {
        AdminHelper.activateCorporate(
                new ActivateIdentityModel(false), corporateId, impersonatedAdminToken);
    }

    public void deactivateCorporate(final String corporateId) {
        AdminHelper.deactivateCorporate(
                new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"), corporateId, impersonatedAdminToken);
    }

    public String createSemiCorporate() {
        return CorporatesService.createCorporate(CreateCorporateModel.DefaultCreateCorporateModel(corporatesProfileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(corporateRootEmail)
                                .setName(name)
                                .setSurname(surname)
                                .build()).build(), secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("id.id");
    }
}