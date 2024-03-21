package opc.junit.secure.biometric;

import commons.enums.State;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.IdentityType;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CorporatesEnrolBiometricTests extends AbstractEnrolBiometricTests {

  private String identityMobileCountryCode;
  private String identityMobileNumber;

  @Override
  protected IdentityDetails getIdentity(final ProgrammeDetailsModel programme) {

    final CreateCorporateModel createCorporateModel = CreateCorporateModel
            .DefaultCreateCorporateModel(programme.getCorporatesProfileId()).build();

    identityMobileCountryCode = createCorporateModel.getRootUser().getMobile().getCountryCode();
    identityMobileNumber = createCorporateModel.getRootUser().getMobile().getNumber();

    final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, programme.getSecretKey());

    return IdentityDetails.generateDetails(createCorporateModel.getRootUser().getEmail(),
            corporate.getLeft(), corporate.getRight(), IdentityType.CORPORATE, null, null);
  }

  /**
   * This test confirms that an alert sms is sent to corporate user from second enrolment since user mobile is verified
   * It is not used for consumer authorized user, because we dont have mobile validation process for consumer authorized users
   */
  @Test
  public void EnrolUser_AuthorizedUserSendSmsFromSecondEnrolment_Success() {

    final IdentityDetails identity = getIdentity(passcodeApp);

    final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
    final Pair<String, String> user = UsersHelper.createEnrolledUser(usersModel, secretKey, identity.getToken());

    //First enrolment
    final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(user.getRight(), sharedKey);
    final String linkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);
    SimulatorHelper.simulateEnrolmentLinking(secretKey, user.getLeft(), linkingCode);
    assertAuthFactorsState(user.getRight(), State.ACTIVE.name());

    //Second enrolment
    final EnrolBiometricModel secondEnrolBiometricModel = getEnrolBiometricModel(user.getRight(), sharedKey);
    final String secondLinkingCode = issueEnrolChallenge(user.getRight(), secondEnrolBiometricModel);
    SimulatorHelper.simulateEnrolmentLinking(secretKey, user.getLeft(), secondLinkingCode);

    final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(usersModel.getMobile().getNumber());
    assertEquals(MailHogSms.SCA_NEW_ENROLMENT_ALERT.getFrom(), sms.getFrom());
    assertEquals(String.format("%s%s@weavr.io", usersModel.getMobile().getCountryCode(),
            usersModel.getMobile().getNumber()), sms.getTo());
    assertEquals(getSmsAudit(user.getLeft()), sms.getBody());
  }

  @Override
  protected String getIdentityMobileNumber() { return identityMobileNumber;}
  @Override
  protected String getIdentityMobileCountryCode() {return identityMobileCountryCode;}
  @Override
  protected String getSmsAudit(final String identityId) {
    return TestHelper.ensureDatabaseDataRetrieved(10,
            () -> CorporatesDatabaseHelper.getCorporateSmsNewestToOldest(identityId),
            x -> x.size() > 0).get(0).get("text");
  }
}

