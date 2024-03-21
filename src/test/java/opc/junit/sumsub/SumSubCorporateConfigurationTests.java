package opc.junit.sumsub;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.ApplicantLevelSumSub;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.enums.opc.KybState;
import opc.enums.sumsub.IdDocType;
import opc.enums.sumsub.RejectLabels;
import opc.enums.sumsub.ReviewRejectType;
import opc.enums.sumsub.SumSubApplicantState;
import opc.junit.database.SumsubDatabaseHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.sumsub.SumSubHelper;
import opc.models.admin.RegisteredCountriesContextModel;
import opc.models.admin.RegisteredCountriesDimension;
import opc.models.admin.RegisteredCountriesSetCountriesModel;
import opc.models.admin.RegisteredCountriesSetModel;
import opc.models.admin.RegisteredCountriesValue;
import opc.models.admin.SetApplicantLevelModel;
import opc.models.admin.SetDimensionValueModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.sumsub.AddBeneficiaryModel;
import opc.models.sumsub.IdentityDetailsModel;
import opc.models.sumsub.SumSubApplicantDataModel;
import opc.services.adminnew.AdminService;
import opc.services.multi.CorporatesService;
import opc.services.secure.SecureService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static opc.enums.sumsub.IdDocType.ARTICLES_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.CERTIFICATE_OF_INCORPORATION;
import static opc.enums.sumsub.IdDocType.COMPANY_POA;
import static opc.enums.sumsub.IdDocType.INFORMATION_STATEMENT;
import static opc.enums.sumsub.IdDocType.SHAREHOLDER_REGISTRY;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SumSubCorporateConfigurationTests extends BaseSumSubSetup {

    final private static List<SetApplicantLevelModel> applicantLevelModels = new ArrayList<>();
    private static RegisteredCountriesContextModel registeredCountriesContextModel;

    @AfterEach
    public void deleteConfiguration(){

        for (SetApplicantLevelModel applicantLevelModel : applicantLevelModels) {
            opc.services.admin.AdminService.deleteCorporateLevelConfiguration(applicantLevelModel, adminToken)
                    .then()
                    .statusCode(SC_OK);
        }
    }

    /**
     * With new changes, we can configure a Sumsub applicant level for a specific corporate or consumer type
     * on tenant or programme level.This test configures a valid(exist in SumSub) applicant level for LLC company
     * and checks applicant level on SumSub.
     */

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, mode = EnumSource.Mode.EXCLUDE, names = { "NON_PROFIT_ORGANISATION"})
    public void Corporate_DynamicMappingSumSubApplicantLevelWithoutConfiguration_Success(final CompanyType companyType){

        final IdentityDetailsModel weavrIdentity = createWeavrIdentity(corporateProfileId, companyType, CountryCode.MT, secretKey, sharedKey);

        checkSumsubLevel(weavrIdentity, companyType.getLevelName());

    }
    @Test
    public void Corporate_DynamicMappingSumSubApplicantLevelForProgramme_Success(){

        final String applicantLevel = ApplicantLevelSumSub.SOLE_TRADER.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for application two
        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelProgramme(
                applicantLevel, applicationTwo.getProgrammeId(), companyType.name());

        setDimension(setApplicantLevelModel);
        applicantLevelModels.add(setApplicantLevelModel);

        //Create a corporate under appTwo
        final IdentityDetailsModel weavrIdentityAppTwo = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());

        checkSumsubLevel(weavrIdentityAppTwo, applicantLevel);


        // Creating a corporate under appOne
        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);

        checkSumsubLevel(weavrIdentityAppOne, companyType.getLevelName());
    }
//
    @Test
    public void Corporate_DynamicMappingSumSubApplicantLevelForIdentity_Success(){

        final String applicantLevel = ApplicantLevelSumSub.PUBLIC_LIMITED_COMPANY.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for a specific corporate
        final CreateCorporateModel firstCreateCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> firstCorporate = CorporatesHelper.createAuthenticatedCorporate(firstCreateCorporateModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelForIdentity(
                applicantLevel, programmeId, firstCorporate.getLeft(), companyType.name());

        setDimension(setApplicantLevelModel);
        applicantLevelModels.add(setApplicantLevelModel);

        final String firstKycReferenceId = CorporatesHelper.startKyb(secretKey, firstCorporate.getRight());

        final IdentityDetailsModel firstWeavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, firstCorporate.getRight(), firstKycReferenceId).getParams();

        checkSumsubLevel(firstWeavrIdentity, applicantLevel);

        // Creating a corporate without configuration

        final IdentityDetailsModel secondWeavrIdentity = createWeavrIdentity(corporateProfileId, companyType, CountryCode.MT, secretKey, sharedKey);

        checkSumsubLevel(secondWeavrIdentity, companyType.getLevelName());
    }

    @Test
    public void Corporate_SumSubShareholdersApplicantLevelForIdentity_Success(){

        final String applicantLevel = ApplicantLevelSumSub.SHAREHOLDERS.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for a specific corporate
        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.DE);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelForIdentity(
                applicantLevel, programmeId, corporate.getLeft(), companyType.name());

        setDimension(setApplicantLevelModel);
        applicantLevelModels.add(setApplicantLevelModel);

        final String firstKycReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), firstKycReferenceId).getParams();

        checkSumsubLevel(weavrIdentity, applicantLevel);
    }

    @Test
    public void Corporate_DynamicMappingSumSubApplicantLevelForTenant_Success(){

        final String applicantLevel = ApplicantLevelSumSub.SOLE_TRADER.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelForTenant(
                applicantLevel, nonFpsEnabledTenant.getInnovatorId(), companyType.name());

        setDimension(setApplicantLevelModel);
        applicantLevelModels.add(setApplicantLevelModel);

        // Create a corporate under nonFpsEnabledTenant
        final IdentityDetailsModel weavrIdentity = createWeavrIdentity(nonFpsEnabledTenant.getCorporatesProfileId(),
                companyType, CountryCode.MT, nonFpsEnabledTenant.getSecretKey(), nonFpsEnabledTenant.getSharedKey());

        checkSumsubLevel(weavrIdentity, applicantLevel);

        // Create a corporate with application One

        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId, companyType, CountryCode.MT, secretKey, sharedKey);

        checkSumsubLevel(weavrIdentityAppOne, companyType.getLevelName());
    }

    @Test
    public void Corporate_DynamicMappingSumSubApplicantLevelMultipleConfigurations_Success(){

        final CompanyType companyType = CompanyType.LLC;

        final String levelForTenantSemi = CompanyType.SOLE_TRADER.getLevelName();
        final String levelForAppTwo = CompanyType.SOLE_TRADER.getLevelName();
        final String levelForAppOne = CompanyType.PUBLIC_LIMITED_COMPANY.getLevelName();
        final String levelForCorporate = CompanyType.NON_PROFIT_ORGANISATION.getLevelName();

        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        // Set config for SemiTenant
        final SetApplicantLevelModel setApplicantLevelModelTenantSemi = SetApplicantLevelModel.setCorporateApplicantLevelForTenant(
                levelForTenantSemi, semiPasscodeApp.getInnovatorId(), companyType.name());
        setDimension(setApplicantLevelModelTenantSemi);

        // Set config for AppTwo
        final SetApplicantLevelModel setApplicantLevelModelAppTwo = SetApplicantLevelModel.setCorporateApplicantLevelProgramme(
                levelForAppTwo, applicationTwo.getProgrammeId(), companyType.name());
        setDimension(setApplicantLevelModelAppTwo);

        // Set config for AppOne
        final SetApplicantLevelModel setApplicantLevelModelAppOne = SetApplicantLevelModel.setCorporateApplicantLevelProgramme(
                levelForAppOne, programmeId, companyType.name());
        setDimension(setApplicantLevelModelAppOne);

        // Set config for consumer under AppOne
        final SetApplicantLevelModel setApplicantLevelModelIdentity = SetApplicantLevelModel.setCorporateApplicantLevelForIdentity(
                levelForCorporate, programmeId, corporate.getLeft(), companyType.name());
        setDimension(setApplicantLevelModelIdentity);

        applicantLevelModels.addAll(List.of(setApplicantLevelModelTenantSemi, setApplicantLevelModelAppTwo, setApplicantLevelModelAppOne, setApplicantLevelModelIdentity));

        // Create a corporate under nonFpsTenant (no configuration, default value)
        final IdentityDetailsModel weavrIdentityNonFps = createWeavrIdentity(nonFpsEnabledTenant.getCorporatesProfileId(),
                companyType, CountryCode.MT, nonFpsEnabledTenant.getSecretKey(), nonFpsEnabledTenant.getSharedKey());
        checkSumsubLevel(weavrIdentityNonFps, companyType.getLevelName());

        // create an identity under semiPasscodeApp
        final IdentityDetailsModel weavrIdentityAppSemiPasscode = createWeavrIdentity(semiPasscodeApp.getCorporatesProfileId(),
                companyType, CountryCode.MT, semiPasscodeApp.getSecretKey(), semiPasscodeApp.getSharedKey());
        checkSumsubLevel(weavrIdentityAppSemiPasscode, levelForTenantSemi);

        // create an identity under semiScaSendsApp
        final IdentityDetailsModel weavrIdentityAppSemiScaSends = createWeavrIdentity(semiScaSendsApp.getCorporatesProfileId(),
                companyType, CountryCode.MT, semiScaSendsApp.getSecretKey(), semiScaSendsApp.getSharedKey());
        checkSumsubLevel(weavrIdentityAppSemiScaSends, levelForTenantSemi);

        // create an identity under appThree of tenant 10
        final IdentityDetailsModel weavrIdentityAppThree = createWeavrIdentity(applicationThree.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationThree.getSecretKey(), applicationThree.getSharedKey());
        checkSumsubLevel(weavrIdentityAppThree, companyType.getLevelName());

        // create an identity under appOne of tenant 10 ( should use levelAppOne)
        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOne, levelForAppOne);

        // create an identity under appTwo of tenant 10 ( should use levelAppTwo)
        final IdentityDetailsModel weavrIdentityAppTwo = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwo, levelForAppTwo);

        // check corporate level
        final String kycReferenceIdCorporate = CorporatesHelper.startKyb(secretKey, corporate.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kycReferenceIdCorporate).getParams();
        checkSumsubLevel(weavrIdentity, levelForCorporate);
    }

    /**
     * This test configures an invalid(non-exist on SumSub) applicant level for a valid corporation type. Sumsub returns 404,
     * but internally we get 503 and following message in the logs
     * "Failed to create Sum Sub applicant for externalUserId: {} and level {} may it be a configuration mismatch"
     */
    @Test
    public void Corporate_InvalidSumSubApplicantLevel_ServiceUnavailable() {

        final CompanyType companyType = CompanyType.PUBLIC_LIMITED_COMPANY;
        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.setCorporateApplicantLevelForIdentity(
                "company-random", programmeId, corporate.getLeft(), companyType.name());

        setDimension(setApplicantLevelModel);

        applicantLevelModels.add(setApplicantLevelModel);

        CorporatesService.startCorporateKyb(secretKey, corporate.getRight())
                .then()
                .statusCode(SC_SERVICE_UNAVAILABLE);
    }

    @Test
    public void Corporate_SumSubApplicantLevelOnRegistrationCountryForProgramme_Success(){
        final String applicantLevel = ApplicantLevelSumSub.SOLE_TRADER.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for application two
        final SetApplicantLevelModel setApplicantLevelModel = createCountryBasedModel
                (applicantLevel, "PROGRAMME_ID", applicationTwo.getProgrammeId(), companyType);
        setDimension(setApplicantLevelModel);

        applicantLevelModels.add(setApplicantLevelModel);

        final IdentityDetailsModel weavrIdentityAppTwo = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwo, applicantLevel);

        // Creating an LLC under application Two from IT

        final IdentityDetailsModel weavrIdentityItaly = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.IT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityItaly, companyType.getLevelName());

        // Creating a corporate with application One
        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOne, companyType.getLevelName());
    }

    @Test
    public void Corporate_SumSubApplicantLevelOnRegistrationCountryForIdentity_Success(){

        final String applicantLevel = ApplicantLevelSumSub.PUBLIC_LIMITED_COMPANY.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for a specific corporate
        final CreateCorporateModel firstCreateCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> firstCorporate = CorporatesHelper.createAuthenticatedCorporate(firstCreateCorporateModel, secretKey);

        final SetApplicantLevelModel setApplicantLevelModel = SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(applicantLevel)
                .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                        new SetDimensionValueModel("CORPORATE_ID", firstCorporate.getLeft()),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()),
                        new SetDimensionValueModel("COUNTRY", CountryCode.MT.name())))
                .build();

        setDimension(setApplicantLevelModel);

        applicantLevelModels.add(setApplicantLevelModel);

        final String firstKycReferenceId = CorporatesHelper.startKyb(secretKey, firstCorporate.getRight());

        final IdentityDetailsModel firstWeavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, firstCorporate.getRight(), firstKycReferenceId).getParams();

        checkSumsubLevel(firstWeavrIdentity, applicantLevel);

        // Creating a corporate without configuration
        final IdentityDetailsModel secondWeavrIdentity = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);

        checkSumsubLevel(secondWeavrIdentity, companyType.getLevelName());

    }

    @Test
    public void Corporate_SumSubApplicantLevelOnRegistrationCountryForTenant_Success(){

        final String applicantLevel = ApplicantLevelSumSub.PUBLIC_LIMITED_COMPANY.getLevelName();
        final CompanyType companyType = CompanyType.LLC;

        final SetApplicantLevelModel setApplicantLevelModel = createCountryBasedModel
                (applicantLevel, "TENANT_ID", nonFpsEnabledTenant.getInnovatorId(), companyType);

        setDimension(setApplicantLevelModel);
        applicantLevelModels.add(setApplicantLevelModel);

        final IdentityDetailsModel weavrIdentityMalta = createWeavrIdentity(nonFpsEnabledTenant.getCorporatesProfileId(),
                companyType, CountryCode.MT, nonFpsEnabledTenant.getSecretKey(), nonFpsEnabledTenant.getSharedKey());
        checkSumsubLevel(weavrIdentityMalta, applicantLevel);

        //create a corporate under the same tenant but from IT
        final IdentityDetailsModel weavrIdentityItaly = createWeavrIdentity(nonFpsEnabledTenant.getCorporatesProfileId(),
                companyType, CountryCode.IT, nonFpsEnabledTenant.getSecretKey(), nonFpsEnabledTenant.getSharedKey());
        checkSumsubLevel(weavrIdentityItaly, companyType.getLevelName());

        // Creating a corporate with application One
        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT,secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOne, companyType.getLevelName());
    }

    @Test
    public void Corporate_SumSubApplicantLevelOnRegistrationCountryMultipleConfigurations_Success(){

        final CompanyType companyType = CompanyType.LLC;

        final String levelForTenantSemi = CompanyType.SOLE_TRADER.getLevelName();
        final String levelForAppTwo = CompanyType.SOLE_TRADER.getLevelName();
        final String levelForAppOne = CompanyType.PUBLIC_LIMITED_COMPANY.getLevelName();
        final String levelForCorporate = CompanyType.NON_PROFIT_ORGANISATION.getLevelName();

        //create a corporate and set a dimension for it
        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final SetApplicantLevelModel setLevelForIdentity = SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(levelForCorporate)
                .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                        new SetDimensionValueModel("CORPORATE_ID", corporate.getLeft()),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()),
                        new SetDimensionValueModel("COUNTRY", CountryCode.MT.name())))
                .build();
        setDimension(setLevelForIdentity);

        // Set config for SemiTenant
        final SetApplicantLevelModel setLevelForTenantSemi = createCountryBasedModel
                (levelForTenantSemi, "TENANT_ID", semiPasscodeApp.getInnovatorId(), companyType);
        setDimension(setLevelForTenantSemi);

        // Set config for AppTwo
        final SetApplicantLevelModel setLevelForAppTwo = createCountryBasedModel
                (levelForAppTwo, "PROGRAMME_ID", applicationTwo.getProgrammeId(), companyType);
        setDimension(setLevelForAppTwo);

        // Set config for AppOne
        final SetApplicantLevelModel setLevelForAppOne = createCountryBasedModel
                (levelForAppOne, "PROGRAMME_ID", programmeId, companyType);
        setDimension(setLevelForAppOne);

        applicantLevelModels.addAll(List.of(setLevelForTenantSemi, setLevelForAppTwo, setLevelForAppOne, setLevelForIdentity));

        // Create a corporate under nonFpsTenant (no configuration, default value)
        final IdentityDetailsModel weavrIdentityDefaultNonFps = createWeavrIdentity(nonFpsEnabledTenant.getCorporatesProfileId(),
                companyType, CountryCode.MT, nonFpsEnabledTenant.getSecretKey(), nonFpsEnabledTenant.getSharedKey());
        checkSumsubLevel(weavrIdentityDefaultNonFps, companyType.getLevelName());

        // create an identity under semiPasscodeApp from DE(should use levelForTenantSemi)
        final IdentityDetailsModel weavrIdentityPasscodeMalta = createWeavrIdentity(semiPasscodeApp.getCorporatesProfileId(),
                companyType, CountryCode.MT, semiPasscodeApp.getSecretKey(), semiPasscodeApp.getSharedKey());
        checkSumsubLevel(weavrIdentityPasscodeMalta, levelForTenantSemi);

        // create an identity under semiPasscodeApp from IT (should use default one)
        final IdentityDetailsModel weavrIdentityPasscodeItaly = createWeavrIdentity(semiPasscodeApp.getCorporatesProfileId(),
                companyType, CountryCode.IT, semiPasscodeApp.getSecretKey(), semiPasscodeApp.getSharedKey());
        checkSumsubLevel(weavrIdentityPasscodeItaly, companyType.getLevelName());

        // create an identity under semiScaSendsApp from DE (should use levelForTenantSemi)
        final IdentityDetailsModel weavrIdentityScaMalta = createWeavrIdentity(semiScaSendsApp.getCorporatesProfileId(),
                companyType, CountryCode.MT, semiScaSendsApp.getSecretKey(), semiScaSendsApp.getSharedKey());
        checkSumsubLevel(weavrIdentityScaMalta, levelForTenantSemi);

        // create an identity under semiScaSendsApp from IT (should use default one)
        final IdentityDetailsModel weavrIdentityScaItaly = createWeavrIdentity(semiScaSendsApp.getCorporatesProfileId(),
                companyType, CountryCode.IT, semiScaSendsApp.getSecretKey(), semiScaSendsApp.getSharedKey());
        checkSumsubLevel(weavrIdentityScaItaly, companyType.getLevelName());

        // create an identity under appThree of tenant 10(should use default one)
        final IdentityDetailsModel weavrIdentityAppThree = createWeavrIdentity(applicationThree.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationThree.getSecretKey(), applicationThree.getSharedKey());
        checkSumsubLevel(weavrIdentityAppThree, companyType.getLevelName());

        // create an identity under appOne of tenant 10 from DE ( should use levelAppOne)
        final IdentityDetailsModel weavrIdentityAppOneMalta = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOneMalta, levelForAppOne);

        // create an identity under appOne of tenant 10 from IT( should use default one)
        final IdentityDetailsModel weavrIdentityAppOneItaly = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.IT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOneItaly, companyType.getLevelName());

        // create an identity under appTwo of tenant 10 from DE ( should use levelAppTwo)
        final IdentityDetailsModel weavrIdentityAppTwoMalta = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwoMalta, levelForAppTwo);

        // create an identity under appTwo of tenant 10 from IT ( should use default one)
        final IdentityDetailsModel weavrIdentityAppTwoItaly = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.IT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwoItaly, companyType.getLevelName());

        // check corporate level( should use level for corporate)
        final String kycReferenceIdCorporate = CorporatesHelper.startKyb(secretKey, corporate.getRight());
        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kycReferenceIdCorporate).getParams();
        checkSumsubLevel(weavrIdentity, levelForCorporate);
    }

    @ParameterizedTest
    @EnumSource(value = CompanyType.class, mode = EnumSource.Mode.INCLUDE,
            names = { "SOLE_TRADER", "LLC", "LIMITED_LIABILITY_PARTNERSHIP", "PUBLIC_LIMITED_COMPANY"})
    public void Corporate_SumSubApplicantLevelOnRegistrationCountryCheckCompanyTypes_Success(final CompanyType companyType){

        final String applicantLevel = ApplicantLevelSumSub.LLC.getLevelName();

        //We set corporate level for MT
        final SetApplicantLevelModel setApplicantLevelModel = createCountryBasedModel
                (applicantLevel, "PROGRAMME_ID", programmeId, companyType);
        setDimension(setApplicantLevelModel);

        applicantLevelModels.add(setApplicantLevelModel);

        //Create a corporate from MT
        final IdentityDetailsModel weavrIdentityGermany = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.MT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityGermany, applicantLevel);

        // Creating a corporate from IT
        final IdentityDetailsModel weavrIdentityAppOne = createWeavrIdentity(corporateProfileId,
                companyType, CountryCode.IT, secretKey, sharedKey);
        checkSumsubLevel(weavrIdentityAppOne, companyType.getLevelName());
    }

    @Test
    public void Corporate_SumSubDimensionGetTranslationWithLanguageDimension_Success(){

        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for application one with language en
        final Map<String, String> translationEnglish = setTranslationDimension("English");
        final SetApplicantLevelModel dimensionEnglish = createDimensionWithLanguage(companyType, translationEnglish, "en");
        setDimension(dimensionEnglish);

        //We set configuration for application one with language en
        final Map<String, String> translationGerman = setTranslationDimension("German");
        final SetApplicantLevelModel dimensionGerman = createDimensionWithLanguage(companyType, translationGerman, "de");
        setDimension(dimensionGerman);

        applicantLevelModels.addAll(List.of(dimensionEnglish, dimensionGerman));

        final CreateCorporateModel corporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);

        final String kycReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        //Filter with English language
        final Map<String, String> filterWithEnglish = Map.of("language", "en");

        final Map<String, String> responseEnglishTranslation = SecureService.getIdentityDetailsWithBody(filterWithEnglish, sharedKey, corporate.getRight(), kycReferenceId)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("translation");

        assertEquals(responseEnglishTranslation, translationEnglish);

        //Filter with German language
        final Map<String, String> filterWithGerman = Map.of("language", "de");

        final Map<String, String> responseGermanTranslation = SecureService.getIdentityDetailsWithBody(filterWithGerman, sharedKey, corporate.getRight(), kycReferenceId)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("translation");

        assertEquals(responseGermanTranslation, translationGerman);
    }

    @Test
    public void Corporate_SumSubDimensionGetTranslationWithoutLanguageDimension_Success(){

        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for application one with language en
        final Map<String, String> translation = setTranslationDimension("Random");
        final SetApplicantLevelModel applicantLevelModel = SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(companyType.getLevelName())
                .json_value(translation)
                .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()),
                        new SetDimensionValueModel("COUNTRY", CountryCode.MT.name()))).build();

        setDimension(applicantLevelModel);
        applicantLevelModels.add(applicantLevelModel);

        final CreateCorporateModel corporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);
        final String kycReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final Map<String, String> responseTranslation = SecureService.getIdentityDetails(sharedKey, corporate.getRight(), kycReferenceId)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("translation");

        assertEquals(responseTranslation, translation);
    }

    @Test
    public void Corporate_SumSubDimensionGetTranslationWithoutCountryDimension_Success(){

        final CompanyType companyType = CompanyType.LLC;

        //We set configuration for application one with language en
        final Map<String, String> translation = setTranslationDimension("Random");
        final SetApplicantLevelModel applicantLevelModel = SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(companyType.getLevelName())
                .json_value(translation)
                .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()))).build();

        setDimension(applicantLevelModel);
        applicantLevelModels.add(applicantLevelModel);

        final CreateCorporateModel corporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateModel, secretKey);
        final String kycReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final Map<String, String> responseTranslation = SecureService.getIdentityDetails(sharedKey, corporate.getRight(), kycReferenceId)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("translation");

        assertEquals(responseTranslation, translation);
    }

    /**
     * With the new dimension we would be able to specify allowed countries based on the company type as well
     */
    @Test
    public void CreateCorporate_SetRegistrationCountriesForProgrammeLevel_Success(){

        final CompanyType companyType = CompanyType.SOLE_TRADER;

        //set dimension for app2 that allow SOLE_TRADER from only GB and IT
        registeredCountriesContextModel = RegisteredCountriesContextModel.builder().dimension(
                List.of(new RegisteredCountriesDimension("CorporateTypeDimension", companyType.name()),
                        new RegisteredCountriesDimension("TenantIdDimension", applicationTwo.getInnovatorId()),
                        new RegisteredCountriesDimension("ProgrammeId", applicationTwo.getProgrammeId()))).build();

        updateRegistrationCountries(registeredCountriesContextModel);

        //attempt to create SOLE_TRADER from BE under app2
        createCorporate(applicationTwo, CountryCode.BE, companyType)
                .statusCode(SC_CONFLICT)
                .body("errorCode",equalTo("COUNTRY_UNSUPPORTED"));

        //attempt to create SOLE_TRADER from IT under app2
        final IdentityDetailsModel weavrIdentityAppTwoIT = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.IT, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwoIT, companyType.getLevelName());

        //attempt to create SOLE_TRADER from GB under app2
        final IdentityDetailsModel weavrIdentityAppTwoGB = createWeavrIdentity(applicationTwo.getCorporatesProfileId(),
                companyType, CountryCode.GB, applicationTwo.getSecretKey(), applicationTwo.getSharedKey());
        checkSumsubLevel(weavrIdentityAppTwoGB, companyType.getLevelName());

        //attempt to create SOLE_TRADER from DE under app1
        final IdentityDetailsModel weavrIdentityAppOneDE = createWeavrIdentity(applicationOne.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationOne.getSecretKey(), applicationOne.getSharedKey());
        checkSumsubLevel(weavrIdentityAppOneDE, companyType.getLevelName());
    }

    @Test
    public void CreateCorporate_SetRegistrationCountriesForTenantLevel_Success(){

        final CompanyType companyType = CompanyType.SOLE_TRADER;

        //set dimension for semi tenant that allow SOLE_TRADER from only GB and IT
        registeredCountriesContextModel = RegisteredCountriesContextModel.builder().dimension(
                List.of(new RegisteredCountriesDimension("CorporateTypeDimension", companyType.name()),
                        new RegisteredCountriesDimension("TenantIdDimension", semiPasscodeApp.getInnovatorId()))).build();

        updateRegistrationCountries(registeredCountriesContextModel);

        //attempt to create SOLE_TRADER from DE under semiPasscodeApp of SemiTenant
        createCorporate(semiPasscodeApp, CountryCode.MT, companyType)
                .statusCode(SC_CONFLICT)
                .body("errorCode",equalTo("COUNTRY_UNSUPPORTED"));

        //attempt to create SOLE_TRADER from DE under semiScaSendsApp of SemiTenant
        createCorporate(semiScaSendsApp, CountryCode.MT, companyType)
                .statusCode(SC_CONFLICT)
                .body("errorCode",equalTo("COUNTRY_UNSUPPORTED"));

        //attempt to create SOLE_TRADER from IT under semiPasscodeApp of SemiTenant
        final IdentityDetailsModel weavrIdentitySemiPasscodeIT = createWeavrIdentity(semiPasscodeApp.getCorporatesProfileId(),
                companyType, CountryCode.IT, semiPasscodeApp.getSecretKey(), semiPasscodeApp.getSharedKey());
        checkSumsubLevel(weavrIdentitySemiPasscodeIT, companyType.getLevelName());

        //attempt to create SOLE_TRADER from GB under semiScaSendsApp of SemiTenant
        final IdentityDetailsModel weavrIdentitySemiScaGB = createWeavrIdentity(semiScaSendsApp.getCorporatesProfileId(),
                companyType, CountryCode.GB, semiScaSendsApp.getSecretKey(), semiScaSendsApp.getSharedKey());
        checkSumsubLevel(weavrIdentitySemiScaGB, companyType.getLevelName());

        //attempt to create SOLE_TRADER from DE under app1 of MultiAutomation Tenant
        final IdentityDetailsModel weavrIdentityAppOneDE = createWeavrIdentity(applicationOne.getCorporatesProfileId(),
                companyType, CountryCode.MT, applicationOne.getSecretKey(), applicationOne.getSharedKey());
        checkSumsubLevel(weavrIdentityAppOneDE, companyType.getLevelName());
    }

    /**
     * We have a new applicant level on sumsub named "corporate-reduced-docs-workflow-start". If an applicant is created with
     * this level on sumsub, we ignore pending and approved callbacks because workflow is not completed yet. If we have rejected
     * callback we handle it as is before
     */
    @Test
    public void Corporate_WorkflowStartingApprovedThenApprovedWithFinalReview_Success() throws SQLException {

        final CompanyType companyType = CompanyType.LLC;
        final String applicantLevel = "corporate-reduced-docs-workflow-start";

        final SetApplicantLevelModel setApplicantLevel = createCountryBasedModel
                (applicantLevel, "PROGRAMME_ID", programmeId, companyType);
        setDimension(setApplicantLevel);
        applicantLevelModels.add(setApplicantLevel);

        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId());

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                        .setShareSize(50).build();

        final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
                applicantData.getId(),
                companyType.name(),
                addBeneficiaryModel);
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                        .setShareSize(50).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
                representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

        final Map<String, String> corporateBeforeCompleted = SumsubDatabaseHelper.getCorporateTenant(corporate.getLeft()).get(0);
        assertEquals("1", corporateBeforeCompleted.get("workflow_enable"));
        assertEquals(KybState.INITIATED.name(), corporateBeforeCompleted.get("status"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.changeApplicantLevel(applicantData.getId(), companyType.getLevelName());
        SumSubHelper.setWorkflowApplicantInApprovedState(weavrIdentity.getExternalUserId());

        final Map<String, String> corporateAfterCompleted = SumsubDatabaseHelper.getCorporateTenant(corporate.getLeft()).get(0);
        assertEquals("0", corporateAfterCompleted.get("workflow_enable"));
        assertEquals(KybState.APPROVED.name(), corporateAfterCompleted.get("status"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.APPROVED.name());
    }

    @Test
    public void Corporate_WorkflowStartingApprovedThenRejectedWithFinalReview_Success() throws SQLException {

        final CompanyType companyType = CompanyType.LLC;
        final String applicantLevel = "corporate-reduced-docs-workflow-start";

        final SetApplicantLevelModel setApplicantLevel = createCountryBasedModel
                (applicantLevel, "PROGRAMME_ID", programmeId, companyType);
        setDimension(setApplicantLevel);
        applicantLevelModels.add(setApplicantLevel);

        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId)
                        .getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                                weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(),
                weavrIdentity.getExternalUserId());

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, CERTIFICATE_OF_INCORPORATION, ARTICLES_OF_INCORPORATION,
                SHAREHOLDER_REGISTRY, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(),
                applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director"))
                        .setShareSize(50).build();

        final JsonPath beneficiary = SumSubHelper.addBeneficiaryWithQuestionnaire(weavrIdentity.getAccessToken(),
                applicantData.getId(),
                companyType.name(),
                addBeneficiaryModel);
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel)
                        .setShareSize(50).build();

        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(),
                        addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, createCorporateModel,
                representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.pending);

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        SumSubHelper.setApplicantInApprovedState(weavrIdentity.getExternalUserId(), applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId(), SumSubApplicantState.completed);

        final Map<String, String> corporateBeforeCompleted = SumsubDatabaseHelper.getCorporateTenant(corporate.getLeft()).get(0);
        assertEquals("1", corporateBeforeCompleted.get("workflow_enable"));
        assertEquals(KybState.INITIATED.name(), corporateBeforeCompleted.get("status"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.changeApplicantLevel(applicantData.getId(), companyType.getLevelName());

        final List<String> rejectLabels = List.of(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());

        SumSubHelper.setWorkflowApplicantInRejectState(rejectLabels, ReviewRejectType.EXTERNAL,
                Optional.of("Issue with verification."), applicantData.getId(), weavrIdentity.getExternalUserId());

        final Map<String, String> corporateAfterCompleted = SumsubDatabaseHelper.getCorporateTenant(corporate.getLeft()).get(0);
        assertEquals("0", corporateAfterCompleted.get("workflow_enable"));
        assertEquals(KybState.REJECTED.name(), corporateAfterCompleted.get("status"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());
    }

    @Test
    public void Corporate_WorkflowStartingRejected_Success() {

        final CompanyType companyType = CompanyType.LLC;
        final String applicantLevel = "corporate-reduced-docs-workflow-start";

        final SetApplicantLevelModel setApplicantLevel = createCountryBasedModel
                (applicantLevel, "PROGRAMME_ID", programmeId, companyType);
        setDimension(setApplicantLevel);
        applicantLevelModels.add(setApplicantLevel);

        final CreateCorporateModel createCorporateModel = createCorporateModel(corporateProfileId, companyType, CountryCode.MT);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String kybReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        final IdentityDetailsModel weavrIdentity =
                SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kybReferenceId).getParams();

        final SumSubApplicantDataModel applicantData =
                SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                        .as(SumSubApplicantDataModel.class);

        SumSubHelper.submitCompanyInformation(weavrIdentity.getAccessToken(), applicantData);

        final List<IdDocType> requiredDocuments = Arrays.asList(COMPANY_POA, INFORMATION_STATEMENT);
        SumSubHelper.uploadRequiredDocuments(requiredDocuments, weavrIdentity.getAccessToken(), applicantData.getId());

        SumSubHelper.uploadQuestionnaire(weavrIdentity.getAccessToken(), applicantData.getId(),
                SumSubHelper.buildDefaultCorporateQuestionnaire(applicantData.getId())
        );

        final AddBeneficiaryModel addBeneficiaryModel =
                AddBeneficiaryModel.defaultAddBeneficiaryModel(Collections.singletonList("director")).build();
        final JsonPath beneficiary = SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addBeneficiaryModel).jsonPath();
        final String beneficiaryId = beneficiary.get("applicantId");
        final String beneficiaryExternalUserId = beneficiary.get("applicant.externalUserId");

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final AddBeneficiaryModel addRepresentativeModel =
                AddBeneficiaryModel.rootUserAddRepresentativeModel(createCorporateModel).build();
        final JsonPath representative =
                SumSubHelper.addBeneficiary(weavrIdentity.getAccessToken(), applicantData.getId(), addRepresentativeModel).jsonPath();
        final String representativeId = representative.get("applicantId");
        final String representativeExternalUserId = representative.get("applicant.externalUserId");

        SumSubHelper.approveRepresentative(companyType, representativeId, representativeExternalUserId, UBO_DIRECTOR_QUESTIONNAIRE_ID);

        weavrIdentity.setAccessToken(SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(), companyType.name()));

        final Pair<String, String> director =
                SumSubHelper.addDirector(companyType, weavrIdentity.getAccessToken(), applicantData.getId());

        weavrIdentity.setAccessToken(
                SumSubHelper.generateCorporateAccessToken(weavrIdentity.getExternalUserId(),
                        companyType.name()));

        SumSubHelper.setApplicantInPendingState(weavrIdentity, applicantData.getId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("pending"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.INITIATED.name());

        SumSubHelper.approveBeneficiary(beneficiaryId, beneficiaryExternalUserId, companyType);
        SumSubHelper.approveDirector(director.getLeft(), director.getRight());

        final List<String> rejectLabels = List.of(RejectLabels.ADDITIONAL_DOCUMENT_REQUIRED.name());
        SumSubHelper
                .setApplicantInRejectState(rejectLabels, ReviewRejectType.EXTERNAL, Optional.of("Issue with verification."),
                        applicantData.getId(), weavrIdentity.getExternalUserId());
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("completed"));

        CorporatesHelper.verifyCorporateState(corporate.getLeft(), KybState.REJECTED.name());
    }

    private CreateCorporateModel createCorporateModel(final String profileId, final CompanyType companyType, final CountryCode countryCode) {

        return CreateCorporateModel.DefaultCreateCorporateModel(profileId)
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setType(companyType.name())
                        .setRegistrationCountry(countryCode.name()).build())
                .build();

    }

    private IdentityDetailsModel createWeavrIdentity(final String profileId,
                                     final CompanyType companyType,
                                     final CountryCode countryCode,
                                     final String secretKey,
                                     final String sharedKey){

        final CreateCorporateModel createCorporateModel = createCorporateModel(profileId, companyType, countryCode);
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final String kycReferenceId = CorporatesHelper.startKyb(secretKey, corporate.getRight());

        return SumSubHelper.getWeavrIdentityDetails(sharedKey, corporate.getRight(), kycReferenceId).getParams();
    }

    private void checkSumsubLevel(final IdentityDetailsModel weavrIdentity, final String levelName ){
        SumSubHelper.getApplicantData(weavrIdentity.getAccessToken(), weavrIdentity.getExternalUserId())
                .then()
                .statusCode(SC_OK)
                .body("review.reviewStatus", equalTo("init"))
                .body("review.levelName", equalTo(levelName));
    }

    private void setDimension(final SetApplicantLevelModel model){
        opc.services.admin.AdminService.createCorporateLevelConfiguration(model, adminToken)
                .then()
                .statusCode(SC_OK);
    }

    private SetApplicantLevelModel createCountryBasedModel(final String applicantLevel,
                                                           final String dimensionKey,
                                                           final String dimensionValue,
                                                           final CompanyType companyType){

        return SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(applicantLevel)
                .dimensionValues(List.of(new SetDimensionValueModel(dimensionKey, dimensionValue),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()),
                        new SetDimensionValueModel("COUNTRY", CountryCode.MT.name())))
                .build();
    }

    private SetApplicantLevelModel createDimensionWithLanguage(final CompanyType companyType,
                                                               final Map<String, String> translation,
                                                               final String language){
        return SetApplicantLevelModel.builder()
                .propertyKey("CORPORATE_LEVEL")
                .propertyValue(companyType.getLevelName())
                .json_value(translation)
                .dimensionValues(List.of(new SetDimensionValueModel("PROGRAMME_ID", programmeId),
                        new SetDimensionValueModel("CORPORATE_TYPE", companyType.name()),
                        new SetDimensionValueModel("COUNTRY", CountryCode.MT.name()),
                        new SetDimensionValueModel("LANGUAGE", language)))
                .build();
    }

    private Map<String, String> setTranslationDimension(final String language){
        return Map.of("dimension", "translation",
                      "language", language);
    }

    private ValidatableResponse createCorporate(final ProgrammeDetailsModel programme,
                                                final CountryCode countryCode,
                                                final CompanyType companyType){

        final CreateCorporateModel corporateModel = CreateCorporateModel.DefaultCreateCorporateModel(programme.getCorporatesProfileId())
                .setCompany(CompanyModel.defaultCompanyModel()
                        .setRegistrationCountry(countryCode.name())
                        .setType(companyType.name()).build())
                .build();

        return CorporatesService.createCorporate(corporateModel, programme.getSecretKey(), Optional.empty()).then();
    }

    private void updateRegistrationCountries(final RegisteredCountriesContextModel model){
        final RegisteredCountriesSetCountriesModel setCountriesModel= RegisteredCountriesSetCountriesModel.builder().context(model)
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel(List.of("IT", "GB"))).build()).build();

        AdminService.updateCompanyRegistrationCountries(adminToken, setCountriesModel)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @AfterEach
    public void setRegistrationCountriesDefault(){

        final List<String> countries =
                Arrays.stream(CountryCode.values())
                        .filter(x -> !x.equals(CountryCode.AF))
                        .map(CountryCode::name)
                        .collect(Collectors.toList());

        final RegisteredCountriesSetCountriesModel setCountriesModel= RegisteredCountriesSetCountriesModel.builder().context(registeredCountriesContextModel)
                .set(RegisteredCountriesSetModel.builder().value(
                        RegisteredCountriesValue.defaultValueModel(countries)).build()).build();

        AdminService.updateCompanyRegistrationCountries(adminToken, setCountriesModel)
                .then()
                .statusCode(SC_NO_CONTENT);
    }
}
