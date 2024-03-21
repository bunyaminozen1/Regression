package opc.junit.admin.corporates;

import opc.enums.opc.CompanyType;
import opc.junit.database.BeneficiaryDatabaseHelper;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CorporateRootUserModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.services.admin.AdminService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LinkToUserTests extends BaseCorporatesSetup {
    @Test
    public void Corporate_KycVerifiedUserLinkToUser_Success() throws SQLException {

        final CompanyType companyType = CompanyType.LLC;

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                        corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name()).build())
                .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                        .setMobile(new MobileNumberModel("+356",
                                String.format("79%s", RandomStringUtils.randomNumeric(6))))
                        .build())
                .build();
        final Pair<String, String> corporate = CorporatesHelper.createKybVerifiedCorporate(
                createCorporateModel, secretKey);

        final CorporateRootUserModel rootUser = createCorporateModel.getRootUser();

        CorporatesService.getCorporates(secretKey, corporate.getRight())
                .then()
                .body("rootUser.dateOfBirth.day", equalTo(rootUser.getDateOfBirth().getDay()))
                .body("rootUser.dateOfBirth.month", equalTo(rootUser.getDateOfBirth().getMonth()))
                .body("rootUser.dateOfBirth.year", equalTo(rootUser.getDateOfBirth().getYear()))
                .body("rootUser.email", equalTo(rootUser.getEmail()))
                .body("rootUser.id.id", equalTo(corporate.getLeft()))
                .body("rootUser.mobile.countryCode", equalTo(rootUser.getMobile().getCountryCode()))
                .body("rootUser.mobile.number", equalTo(rootUser.getMobile().getNumber()))
                .body("rootUser.name", equalTo(rootUser.getName()))
                .body("rootUser.surname", equalTo(rootUser.getSurname()));

        final Pair<String, UsersModel> user = startAndApproveAuthenticatedUserKyc(createCorporateModel,
                corporate.getRight(), companyType);

        final Map<String, String> beneficiary = BeneficiaryDatabaseHelper.findByCorporateId(
                corporate.getLeft()).get(1);

        final String beneficiaryId = beneficiary.get("id");

        AdminService.linkToUser(AdminService.loginAdmin(), corporate.getLeft(), beneficiaryId, user.getLeft())
                .then()
                .body("userId", equalTo(user.getLeft()))
                .statusCode(SC_OK);

        assertEquals(beneficiaryId, CorporatesDatabaseHelper.getCorporateUser(user.getLeft()).get(0).get("beneficiary_id"));
    }

    @Test
    public void linkBeneficiaryIdForAuthUserUnknownIdConflict() {
        AdminService.linkToUser(adminToken, RandomStringUtils.randomNumeric(18),RandomStringUtils.randomNumeric(18), RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("BENEFICIARY_NOT_FOUND"));
    }
}
