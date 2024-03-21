package opc.junit.admin.consumers;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CountryCode;
import opc.models.admin.UpdateConsumerProfileModel;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.AddressModel;
import opc.services.admin.AdminService;
import opc.services.multi.ConsumersService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import java.util.Optional;

import static opc.enums.opc.CountryCode.getAllEeaCountries;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.SAME_THREAD)
public class AllowedCountriesConfigurationTests extends BaseConsumersSetup {

    final private static String newConsumerProfileId = "111805576092778780";

    /**
     * system level : Default
     * programme level : "MT", "IT", "BG"
     * profile level : "MT", "DE", "BE"
     * Country codes that are used for test "MT", "IT", "BG", "DE", "BE", "FR"
     * Only consumers from MT should be allowed under applicationOne because it is defined all three levels,
     * but all consumers should be allowed under applicationTwo since it has not programme or profile level, so it uses system level
     */
    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "IT", "BG", "DE", "BE", "FR"})
    public void Consumer_AllowedCountriesSetInProgrammeAndProfileLevelCheckOtherProgrammeEEA_Success(final CountryCode countryCode){

        updateCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "IT", "BG")).build());

        updateCountriesOnProfileLevel(UpdateConsumerProfileModel.builder().allowedCountries(List.of("MT", "DE", "BE")).build());

        //Create a consumer under ApplicationOne
        final CreateConsumerModel consumerModel = createConsumerModel(consumerProfileId, countryCode);

        final ValidatableResponse responseAppOne = ConsumersService.createConsumer(consumerModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseAppOne.statusCode(SC_OK);
        }else {
            responseAppOne.statusCode(SC_CONFLICT);
            responseAppOne.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }

        // //Create a consumer under ApplicationTwo that has no selected country in programme or profile level

        final CreateConsumerModel consumerModelAppTwo = createConsumerModel(applicationTwo.getConsumersProfileId(), countryCode);
        ConsumersService.createConsumer(consumerModelAppTwo, applicationTwo.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * system level : Default
     * programme level : Not Defined
     * profile level : "MT", "DE", "BE"
     * Country codes that are used for test "MT", "IT", "BG", "FR"
     * Only consumers from MT should be allowed under existing profile, but all consumers should be allowed under new profile
     * since there is no defined country inside it, so it uses system level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "IT", "BG", "FR"})
    public void Consumer_AllowedCountriesSetInProfileLevelCheckOtherProfile_Success(final CountryCode countryCode){

        updateCountriesOnProfileLevel(UpdateConsumerProfileModel.builder().allowedCountries(List.of("MT", "DE", "BE")).build());

        final CreateConsumerModel consumerModel = createConsumerModel(consumerProfileId, countryCode);

        final ValidatableResponse responseExistingProfile= ConsumersService.createConsumer(consumerModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseExistingProfile.statusCode(SC_OK);
        }else {
            responseExistingProfile.statusCode(SC_CONFLICT);
            responseExistingProfile.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }

        // ApplicationTwo that has no selected country in programme or profile level

        final CreateConsumerModel newConsumerModel = createConsumerModel(newConsumerProfileId, countryCode);
        ConsumersService.createConsumer(newConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * system level : Default
     * programme level : Not Defined
     * profile level : "MT", "IT", "BG"
     * Country codes that are used for test "MT", "DE", "BE"
     * Only consumers from MT should be allowed because it is defined in profile level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "DE", "BE"})
    public void Consumer_AllowedCountriesSetInProfileLevelNotDefinedInProgrammeLevel_Success(final CountryCode countryCode){

        updateCountriesOnProfileLevel(UpdateConsumerProfileModel.builder().allowedCountries(List.of("MT", "IT", "BG")).build());

        final CreateConsumerModel consumerModel = createConsumerModel(consumerProfileId, countryCode);

        final ValidatableResponse responseAppOne = ConsumersService.createConsumer(consumerModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseAppOne.statusCode(SC_OK);
        }else {
            responseAppOne.statusCode(SC_CONFLICT);
            responseAppOne.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }
    }

    /**
     * system level : Default
     * programme level : "MT", "IT", "BG"
     * profile level : Not Defined
     * Country codes that are used for test "MT", "DE", "BE"
     * Only consumers from MT should be allowed because it is defined in programme level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "DE", "BE"})
    public void Consumer_AllowedCountriesSetInProgrammeLevelNotDefinedInProfileLevel_Success(final CountryCode countryCode){

        updateCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "IT", "BG")).build());

        final CreateConsumerModel consumerModel = createConsumerModel(consumerProfileId, countryCode);

        final ValidatableResponse responseAppOne = ConsumersService.createConsumer(consumerModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseAppOne.statusCode(SC_OK);
        }else {
            responseAppOne.statusCode(SC_CONFLICT);
            responseAppOne.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }
    }


    private CreateConsumerModel createConsumerModel(final String consumerProfileId, final CountryCode countryCode) {

        return CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .setAddress(AddressModel.DefaultAddressModel()
                                .setCountry(countryCode)
                                .build())
                        .build())
                .build();
    }

    @AfterEach
    public void setCountriesDefaultList(){

        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        updateCountriesOnProgrammeLevel(updateProgrammeModel);

        final UpdateConsumerProfileModel updateProfileModel = UpdateConsumerProfileModel.builder()
                .allowedCountries(getAllEeaCountries())
                .hasAllowedCountries(true)
                .build();

        updateCountriesOnProfileLevel(updateProfileModel);
    }

    private void updateCountriesOnProgrammeLevel(final UpdateProgrammeModel updateProgrammeModel){
        AdminService.updateProgramme(updateProgrammeModel, programmeId, adminImpersonatedToken)
                .then()
                .statusCode(SC_OK);
    }

    private void updateCountriesOnProfileLevel(final UpdateConsumerProfileModel updateConsumerProfileModel){
        AdminService.updateConsumerProfile(updateConsumerProfileModel, adminImpersonatedToken, programmeId, consumerProfileId)
                .then()
                .statusCode(SC_OK);
    }
}
