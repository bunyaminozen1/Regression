package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.PasswordsService;
import org.junit.jupiter.api.BeforeEach;

import static opc.enums.opc.UserType.ROOT;

public class RequestAccessTokenCorporateTests extends AbstractRequestAccessTokenTests{
    private String identityId;
    private String corporateRootEmail;

    @BeforeEach
    public void BeforeEach() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        this.identityId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        this.corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        CorporatesHelper.createCorporatePassword(identityId, secretKey);
        CorporatesHelper.verifyEmail(createCorporateModel.getRootUser().getEmail(), secretKey);
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getLoginEmail() {
        return this.corporateRootEmail;
    }

    @Override
    protected String getIdentityId() {
        return this.identityId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

    @Override
    protected UserType getUserType() {
        return ROOT;
    }

    @Override
    protected String createPassword(final String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }
}
