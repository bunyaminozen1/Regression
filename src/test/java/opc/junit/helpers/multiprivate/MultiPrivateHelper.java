package opc.junit.helpers.multiprivate;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.models.admin.AssignRoleModel;
import opc.models.backoffice.IdentityModel;
import opc.models.multiprivate.RegisterLinkedAccountModel;
import opc.services.multiprivate.MultiPrivateService;
import org.apache.commons.lang3.tuple.Pair;

import static org.apache.http.HttpStatus.SC_OK;

public class MultiPrivateHelper {

    public static Pair<String, RegisterLinkedAccountModel> createLinkedManagedAccount(final String corporateId,
                                                                                      final String currency,
                                                                                      final String pluginProgrammeId,
                                                                                      final String pluginCorporateLinkedManagedAccountProfileId,
                                                                                      final String fpiKey) {

        final IdentityModel identityModel = new IdentityModel(corporateId, IdentityType.CORPORATE);

        final RegisterLinkedAccountModel registerLinkedAccountModel = RegisterLinkedAccountModel.DefaultRegisterLinkedAccountFasterModel(identityModel, pluginProgrammeId, pluginCorporateLinkedManagedAccountProfileId, currency).build();

        final String linkedManagedAccountId = TestHelper.ensureAsExpected(60,
                        () -> MultiPrivateService.createLinkedAccount(registerLinkedAccountModel, fpiKey),
                        SC_OK)
                .jsonPath()
                .get("id");

        return Pair.of(linkedManagedAccountId, registerLinkedAccountModel);
    }

    public static void assignRole(final String secretKey,
                                                                      final String token,
                                                                      final String userId,
                                                                      final String roleId) {
        TestHelper.ensureAsExpected(5,
                () -> MultiPrivateService.assignRole(secretKey, token, userId, new AssignRoleModel(Long.parseLong(roleId))),
                SC_OK);
    }
}
