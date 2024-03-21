package opc.junit.mobile;

import opc.enums.opc.InnovatorSetup;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseIdentitySetup {
    //TODO - Move these as part of secure directory
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String consumersProfileId;
    protected static String corporatesProfileId;
    protected static String secretKey;
    protected static String sharedKey;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        consumersProfileId = applicationOne.getConsumersProfileId();
        corporatesProfileId = applicationOne.getCorporatesProfileId();
        secretKey = applicationOne.getSecretKey();
        sharedKey = applicationOne.getSharedKey();
    }
}
