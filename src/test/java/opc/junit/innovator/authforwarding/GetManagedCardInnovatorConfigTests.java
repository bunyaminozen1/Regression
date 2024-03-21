package opc.junit.innovator.authforwarding;


import opc.junit.helpers.admin.AdminHelper;
import opc.services.innovator.InnovatorService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public class GetManagedCardInnovatorConfigTests extends BaseAuthForwardingSetup {

    @AfterAll
    public static void DisableAuthForwarding() {
        AdminHelper.enableAuthForwarding(false, innovatorId, adminImpersonatedTenantToken);
    }

    @Test
    public void GetManagedCardInnovatorConfig_AuthForwardingEnabled_Success() {
        AdminHelper.enableAuthForwarding(true, innovatorId, adminImpersonatedTenantToken);
        InnovatorService.getManagedCardInnovatorConfig(programmeId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("authForwardingEnabled", equalTo(true));
    }

    @Test
    public void GetManagedCardInnovatorConfig_AuthForwardingDisabled_Success() {
        AdminHelper.enableAuthForwarding(false, innovatorId, adminImpersonatedTenantToken);
        InnovatorService.getManagedCardInnovatorConfig(programmeId, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("authForwardingEnabled", equalTo(false));
    }
}
