package opc.junit.secure.biometric;

import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

import static opc.enums.opc.UserType.ROOT;

public class CorporateLoginWithPasswordTests extends AbstractLoginWithPasswordTests{

    private String identityId;
    private String identityToken;
    private String identityEmail;
    private String identityManagedAccountId;

    @BeforeEach
    public void BeforeEach() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        identityToken = corporate.getRight();
        identityId = corporate.getLeft();
        identityEmail = createCorporateModel.getRootUser().getEmail();

        identityManagedAccountId = ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, Currency.EUR.name(), secretKey, identityToken);
        AdminHelper.fundManagedAccount(innovatorId, identityManagedAccountId, Currency.EUR.name(), 100000L);
    }
    @Override
    protected String getIdentityId() {
        return identityId;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    protected String getIdentityEmail() {
        return identityEmail;
    }

    @Override
    protected String getManagedAccountId() {
        return identityManagedAccountId;
    }

    @Override
    protected UserType getUserType() {
        return ROOT;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

}
