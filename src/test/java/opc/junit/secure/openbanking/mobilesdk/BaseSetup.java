package opc.junit.secure.openbanking.mobilesdk;

import opc.enums.opc.InnovatorSetup;
import opc.junit.multi.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BaseSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationOne;
    protected static String sharedKey;
    protected static String secretKey;
    protected static String corporateProfileId;

    @BeforeAll
    public static void GlobalSetup() {
        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);
        sharedKey = applicationOne.getSharedKey();
        secretKey = applicationOne.getSecretKey();
        corporateProfileId = applicationOne.getCorporatesProfileId();
    }
}
