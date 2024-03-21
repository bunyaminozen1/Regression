package opc.junit.adminnew.passwords;

import opc.enums.opc.InnovatorSetup;
import opc.junit.innovator.BaseSetupExtension;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.adminnew.AdminService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BasePasswordsSetup {

  protected static String secretKey;
  protected static String secretKey2;
  protected static String programmeId;
  protected static String programmeId2;

  protected static String corporateProfileId;
  protected static String corporateProfileId2;
  protected static String consumerProfileId;
  protected static String consumerProfileId2;
  protected static String adminToken;

  @RegisterExtension
  static BaseSetupExtension setupExtension = new BaseSetupExtension();

  @BeforeAll
  public static void Setup() {

    final ProgrammeDetailsModel applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(
        InnovatorSetup.APPLICATION_ONE);
    final ProgrammeDetailsModel applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(
        InnovatorSetup.APPLICATION_TWO);

    corporateProfileId = applicationOne.getCorporatesProfileId();
    corporateProfileId2 = applicationTwo.getCorporatesProfileId();
    consumerProfileId = applicationOne.getConsumersProfileId();
    consumerProfileId2 = applicationTwo.getConsumersProfileId();

    secretKey = applicationOne.getSecretKey();
    secretKey2 = applicationTwo.getSecretKey();
    programmeId = applicationOne.getProgrammeId();
    programmeId2 = applicationTwo.getProgrammeId();

    adminToken = AdminService.loginAdmin();
  }
}
