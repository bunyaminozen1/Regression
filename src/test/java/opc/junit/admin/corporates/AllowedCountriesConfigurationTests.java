package opc.junit.admin.corporates;

import commons.models.CompanyModel;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.CountryCode;
import opc.models.admin.UpdateCorporateProfileModel;
import opc.models.admin.UpdateProgrammeModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.services.admin.AdminService;
import opc.services.multi.CorporatesService;
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
public class AllowedCountriesConfigurationTests extends BaseCorporatesSetup{

    final private static String newCorporateProfileId = "111805580511609116";

    /**
     * system level : Default
     * programme level : "MT", "IT", "BG"
     * profile level : "MT", "DE", "BE"
     * Country codes that are used for test "MT", "IT", "BG", "DE", "BE", "FR"
     * Only Corporates from MT should be allowed under applicationOne because it is defined all three levels,
     * but all Corporates should be allowed under applicationTwo since it has not programme or profile level, so it uses system level
     */
    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "IT", "BG", "DE", "BE", "FR"})
    public void Corporate_AllowedCountriesSetInProgrammeAndProfileLevelCheckOtherProgramme_Success(final CountryCode countryCode){

        updateCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "IT", "BG")).build());

        updateCountriesOnProfileLevel(UpdateCorporateProfileModel.builder().setAllowedCountries(List.of("MT", "DE", "BE")).build());

        //Create a Corporate under ApplicationOne
        final CreateCorporateModel CorporateModel = createCorporateModel(corporateProfileId, countryCode);

        final ValidatableResponse responseAppOne = CorporatesService.createCorporate(CorporateModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseAppOne.statusCode(SC_OK);
        }else {
            responseAppOne.statusCode(SC_CONFLICT);
            responseAppOne.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }

        // //Create a Corporate under ApplicationTwo that has no selected country in programme or profile level

        final CreateCorporateModel CorporateModelAppTwo = createCorporateModel(applicationTwo.getCorporatesProfileId(), countryCode);
        CorporatesService.createCorporate(CorporateModelAppTwo, applicationTwo.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * system level : Default
     * programme level : Not Defined
     * profile level : "MT", "DE", "BE"
     * Country codes that are used for test "MT", "IT", "BG", "FR"
     * Only Corporates from MT should be allowed under existing profile, but all Corporates should be allowed under new profile
     * since there is no defined country inside it, so it uses system level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "IT", "BG", "FR"})
    public void Corporate_AllowedCountriesSetInProfileLevelCheckOtherProfile_Success(final CountryCode countryCode){

        updateCountriesOnProfileLevel(UpdateCorporateProfileModel.builder().setAllowedCountries(List.of("MT", "DE", "BE")).build());

        final CreateCorporateModel CorporateModel = createCorporateModel(corporateProfileId, countryCode);

        final ValidatableResponse responseExistingProfile= CorporatesService.createCorporate(CorporateModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseExistingProfile.statusCode(SC_OK);
        }else {
            responseExistingProfile.statusCode(SC_CONFLICT);
            responseExistingProfile.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }

        // ApplicationTwo that has no selected country in programme or profile level

        final CreateCorporateModel newCorporateModel = createCorporateModel(newCorporateProfileId, countryCode);
        CorporatesService.createCorporate(newCorporateModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    /**
     * system level : Default
     * programme level : Not Defined
     * profile level : "MT", "IT", "BG"
     * Country codes that are used for test "MT", "DE", "BE"
     * Only Corporates from MT should be allowed because it is defined in profile level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "DE", "BE"})
    public void Corporate_AllowedCountriesSetInProfileLevelNotDefinedInProgrammeLevel_Success(final CountryCode countryCode){

        updateCountriesOnProfileLevel(UpdateCorporateProfileModel.builder().setAllowedCountries(List.of("MT", "IT", "BG")).build());

        final CreateCorporateModel corporateModel = createCorporateModel(corporateProfileId, countryCode);

        final ValidatableResponse responseAppOne = CorporatesService.createCorporate(corporateModel, secretKey, Optional.empty()).then();

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
     * Only Corporates from MT should be allowed because it is defined in programme level
     */

    @ParameterizedTest
    @EnumSource(value = CountryCode.class, names = {"MT", "DE", "BE"})
    public void Corporate_AllowedCountriesSetInProgrammeLevelNotDefinedInProfileLevel_Success(final CountryCode countryCode){

        updateCountriesOnProgrammeLevel(UpdateProgrammeModel.builder().setCountry(List.of("MT", "IT", "BG")).build());

        final CreateCorporateModel corporateModel = createCorporateModel(corporateProfileId, countryCode);

        final ValidatableResponse responseAppOne = CorporatesService.createCorporate(corporateModel, secretKey, Optional.empty()).then();

        if (countryCode.name().equals("MT")){
            responseAppOne.statusCode(SC_OK);
        }else {
            responseAppOne.statusCode(SC_CONFLICT);
            responseAppOne.body("errorCode", equalTo("COUNTRY_UNSUPPORTED"));
        }
    }


    private CreateCorporateModel createCorporateModel(final String corporateProfileId, final CountryCode countryCode) {

        return CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setRegistrationCountry(countryCode.name())
                    .build())
                .build();
    }

    @AfterEach
    public void setCountriesEmptyList(){

        final UpdateProgrammeModel updateProgrammeModel = UpdateProgrammeModel.builder()
                .setCountry(getAllEeaCountries())
                .setHasCountry(true)
                .build();

        updateCountriesOnProgrammeLevel(updateProgrammeModel);

        final UpdateCorporateProfileModel updateProfileModel = UpdateCorporateProfileModel.builder()
                .setAllowedCountries(getAllEeaCountries())
                .setHasAllowedCountries(true)
                .build();

        updateCountriesOnProfileLevel(updateProfileModel);
    }

    private void updateCountriesOnProgrammeLevel(final UpdateProgrammeModel updateProgrammeModel){
        AdminService.updateProgramme(updateProgrammeModel, programmeId, impersonatedAdminToken)
                .then()
                .statusCode(SC_OK);
    }

    private void updateCountriesOnProfileLevel(final UpdateCorporateProfileModel updateCorporateProfileModel){
        AdminService.updateCorporateProfile(updateCorporateProfileModel, impersonatedAdminToken, programmeId, corporateProfileId)
                .then()
                .statusCode(SC_OK);
    }
}
