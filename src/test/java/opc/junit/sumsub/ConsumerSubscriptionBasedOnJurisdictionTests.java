package opc.junit.sumsub;

import opc.enums.opc.CountryCode;
import opc.enums.opc.KycLevel;
import opc.enums.opc.KycState;
import opc.junit.database.SubscriptionsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.AddressModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static opc.enums.opc.CountryCode.GB;
import static opc.enums.opc.CountryCode.getAllEeaCountries;
import static opc.enums.opc.CountryCode.getAllUkCountries;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
@Execution(ExecutionMode.CONCURRENT)
public class ConsumerSubscriptionBasedOnJurisdictionTests extends BaseSumSubSetup{

    /**
     * In these tests, it is checked if a new onboarded consumers are successfully subscribed to paynetics in terms of
     * EEA and UK jurisdiction
     */

    @Test
    public void ConsumerSubscription_JurisdictionAllowsJustEEACountries_Successful () throws SQLException {

        setResidentialCountriesBasedOnJurisdiction(programmeId, innovatorId, List.of("MT", "DE", "IT", "BE"));
        final CountryCode countryCode = CountryCode.MT;

        final String consumerId = createConsumerAndCompleteApprovalProcess(applicationOne, countryCode);

        checkSubscriptionStatus(consumerId, countryCode.name());
    }

    @Test
    public void ConsumerSubscription_JurisdictionAllowsJustUkCountries_Successful () throws SQLException {

        setResidentialCountriesBasedOnJurisdiction(applicationOneUk.getProgrammeId(), applicationOneUk.getInnovatorId(), List.of("GB", "IM", "JE", "GG"));
        final CountryCode countryCode = GB;

        final String consumerId = createConsumerAndCompleteApprovalProcess(applicationOneUk, countryCode);

        checkSubscriptionStatus(consumerId, countryCode.name());
    }

    private CreateConsumerModel createDefaultConsumerModel(final String profileId, CountryCode countryCode){

        return   CreateConsumerModel.DefaultCreateConsumerModel(profileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setNationality(countryCode.name())
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(countryCode)
                                .build())
                        .build())
                .build();
    }

    private void checkSubscriptionStatus(final String consumerId,
                                         final String subscriberCountry) throws SQLException {

        TestHelper.ensureDatabaseResultAsExpected(120,
                () -> SubscriptionsDatabaseHelper.getSubscription(consumerId),
                x -> x.size() > 0 && x.get(0).get("status").equals("ACTIVE"),
                Optional.of(String.format("Subscription for identity with id %s not %s", consumerId, "ACTIVE")));

        final Map<String, String> subscriberInfo = SubscriptionsDatabaseHelper.getSubscriber(consumerId).get(0);

        assertEquals("READY_TO_SUBSCRIBE", subscriberInfo.get("status"));
        assertEquals(subscriberCountry, subscriberInfo.get("country"));
    }

    private static void setResidentialCountriesBasedOnJurisdiction(final String programmeId, final String tenantId, final List<String> countries) {

        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(countries)
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModel, programmeId, AdminService.impersonateTenant(tenantId, adminToken))
                .then()
                .statusCode(SC_OK);
    }

    @AfterEach
    public void resetResidentialCountries() {

        final UpdateProgrammeModel updateProgrammeModelEEA = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModelEEA, programmeId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);

        final UpdateProgrammeModel updateProgrammeModelUK = UpdateProgrammeModel.builder()
                .setCountry(getAllUkCountries())
                .setHasCountry(true)
                .build();

        AdminService.updateProgramme(updateProgrammeModelUK, applicationOneUk.getProgrammeId(),
                        AdminService.impersonateTenant(applicationOneUk.getInnovatorId(), adminToken))
                .then()
                .statusCode(SC_OK);
    }

    private String createConsumerAndCompleteApprovalProcess(final ProgrammeDetailsModel programme, final CountryCode countryCode){

        final CreateConsumerModel createConsumerModel = createDefaultConsumerModel(programme.getConsumersProfileId(), countryCode);
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(createConsumerModel, programme.getSecretKey());

        final String kycReferenceId = ConsumersHelper.startKyc(KycLevel.KYC_LEVEL_1, programme.getSecretKey(), consumer.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(programme.getSharedKey(), consumer.getRight(), kycReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitApplicantInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildKycLevel1DefaultQuestionnaire(applicantData)
        );

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.PENDING_REVIEW.name());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        ConsumersHelper.verifyConsumerState(consumer.getLeft(), KycState.APPROVED.name());

        return consumer.getLeft();
    }
}
