package opc.junit.adminnew.thirdpartyregistry;

import commons.enums.Currency;
import io.restassured.response.Response;
import opc.enums.opc.CardBrand;
import opc.enums.opc.CardModeThirdPartyRegistry;
import opc.enums.opc.IdentityType;
import opc.enums.opc.InstrumentType;
import opc.enums.opc.KybLevel;
import opc.enums.opc.KycLevel;
import opc.enums.opc.ServiceType;
import opc.enums.opc.ServiceTypeCategory;
import opc.enums.opc.TransactionType;
import opc.junit.helpers.adminnew.AdminHelper;
import opc.models.admin.CreateServiceTypeModel;
import opc.models.admin.CreateThirdPartyRegistryModel;
import opc.models.admin.GetThirdPartyRegistriesResponseModel;
import opc.models.admin.UpdateServiceTypeModel;
import opc.models.admin.UpdateThirdPartyRegistryModel;
import opc.services.adminnew.ThirdPartyRegistryService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static opc.junit.helpers.adminnew.AdminHelper.createThirdPartyRegistry;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

public class ThirdPartyRegistryTests extends BaseThirdPartyRegistrySetup {

    @Test
    public void CreateThirdPartyRegistry_Success() {

        final CreateThirdPartyRegistryModel createThirdPartyRegistryModel = CreateThirdPartyRegistryModel.DefaultCreateThirdPartyRegistryModel();

        ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, adminRootUserToken)
                .then()
                .statusCode(SC_CREATED)
                .body("providerKey", equalTo(createThirdPartyRegistryModel.getProviderKey()))
                .body("providerName", equalTo(createThirdPartyRegistryModel.getProviderName()));
    }

    @Test
    public void CreateThirdPartyRegistry_ExistingProviderKey_Conflict() {

        final CreateThirdPartyRegistryModel createThirdPartyRegistryModel = CreateThirdPartyRegistryModel.DefaultCreateThirdPartyRegistryModel();

        createThirdPartyRegistryModel.setProviderKey(createThirdPartyRegistry().jsonPath().get("providerKey"));

        ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, adminRootUserToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THIRD_PARTY_PROVIDER_KEY_ALREADY_EXISTS"));
    }

    @Test
    public void CreateThirdPartyRegistry_InvalidData_BadRequest() {

        final CreateThirdPartyRegistryModel createThirdPartyRegistryModel = CreateThirdPartyRegistryModel.DefaultCreateThirdPartyRegistryModel();

        createThirdPartyRegistryModel.setProviderKey("--as");

        ThirdPartyRegistryService.createThirdPartyRegistry(createThirdPartyRegistryModel, adminRootUserToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.providerKey: must match \"^[a-zA-Z0-9_]+$\""));
    }

    @Test
    public void GetAllThirdPartyRegistry_Success() {

        AdminHelper.createThirdPartyRegistryWithService();

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistries(adminRootUserToken)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        int count = Math.min(response.getCount(), 100);

        if (response.getCount() > 100) {
            assertEquals(response.getProviders().size(), 100);
        } else {
            assertEquals(response.getProviders().size(), count);
        }
    }

    @Test
    public void GetAllThirdPartyRegistryPaginationLimit_Success() {

        IntStream.range(0, 10).forEach(i -> AdminHelper.createThirdPartyRegistryWithService());

        final Integer limit = 10;

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistriesPagination(adminRootUserToken, limit, 0)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        assertEquals(response.getProviders().size(), response.getResponseCount());
    }

    @Test
    public void GetAllThirdPartyRegistryPaginationOffset_Success() {

        IntStream.range(0, 6).forEach(i -> AdminHelper.createThirdPartyRegistryWithService());

        final int limit = 5;
        final int offset = 2;

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistriesPagination(adminRootUserToken, limit, offset)
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        assertEquals(response.getProviders().size(), limit);
    }

    @Test
    public void GetAllThirdPartyRegistryFilteredProviderName_Success() {

        AdminHelper.createThirdPartyRegistryWithService();

        final String providerName = ThirdPartyRegistryService.getThirdPartyRegistries(adminRootUserToken)
                .jsonPath()
                .get("providers[0].providerName");

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistriesFiltered(adminRootUserToken, Pair.of("providerName", providerName.replace(" ", "%20")))
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        assertEquals(response.getCount(), 1);
        assertEquals(response.getResponseCount(), 1);
    }

    @Test
    public void GetAllThirdPartyRegistryFilteredService_Success() {

        final String service =
                AdminHelper.createThirdPartyRegistryWithService().jsonPath().getString("serviceType");

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistriesFiltered(adminRootUserToken, Pair.of("service", service))
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        assertEquals(response.getProviders().size(), response.getResponseCount());
    }

    @Test
    public void GetAllThirdPartyRegistryFilteredCategory_Success() {

        final String category =
                AdminHelper.createThirdPartyRegistryWithService().jsonPath().getString("category");

        final GetThirdPartyRegistriesResponseModel response = ThirdPartyRegistryService.getThirdPartyRegistriesFiltered(adminRootUserToken, Pair.of("category", category))
                .then()
                .statusCode(SC_OK)
                .extract()
                .as(GetThirdPartyRegistriesResponseModel.class);

        assertEquals(response.getProviders().size(), response.getResponseCount());
    }

    @Test
    public void GetThirdPartyRegistry_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_OK)
                .body("providerKey", equalTo(providerKey))
                .body("providerName", notNullValue());
    }

    @Test
    public void GetThirdPartyRegistry_NonExistent_NotFound() {
        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, "UNKNOWN")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpdateThirdPartyRegistry_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final UpdateThirdPartyRegistryModel updateThirdPartyRegistryModel = UpdateThirdPartyRegistryModel.UpdateThirdPartyRegistryModel()
                .build();

        updateThirdPartyRegistryModel.setProviderName("Updated Name (Automation)");

        ThirdPartyRegistryService.updateThirdPartyRegistry(updateThirdPartyRegistryModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_OK)
                .body("providerKey", equalTo(providerKey))
                .body("providerName", equalTo("Updated Name (Automation)"));
    }

    @Test
    public void UpdateThirdPartyRegistry_NonExistent_NotFound() {
        final UpdateThirdPartyRegistryModel updateThirdPartyRegistryModel = UpdateThirdPartyRegistryModel.UpdateThirdPartyRegistryModel()
                .build();

        ThirdPartyRegistryService.updateThirdPartyRegistry(updateThirdPartyRegistryModel, adminRootUserToken, "UNKNOWN")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeleteThirdPartyRegistry_NoServices_Success() {
        String providerKey = createThirdPartyRegistry().jsonPath().get("providerKey");

        ThirdPartyRegistryService.deleteThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void DeleteThirdPartyRegistry_WithInactiveServices_Success() {
        String providerKey = createThirdPartyRegistry().jsonPath().get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .active(false)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.deleteThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void DeleteThirdPartyRegistry_WithActiveServices_Conflict() {
        String providerKey = createThirdPartyRegistry().jsonPath().get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .active(true)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.deleteThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THIRD_PARTY_PROVIDER_HAS_SERVICE_IN_USE"));
    }

    @Test
    public void DeleteThirdPartyRegistry_NonExistent_NotFound() {
        ThirdPartyRegistryService.deleteThirdPartyRegistry(adminRootUserToken, "UNKNOWN")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetThirdPartyRegistry_WhenDeleted_NotFound() {
        String providerKey = createThirdPartyRegistry().jsonPath().get("providerKey");

        ThirdPartyRegistryService.deleteThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreateServiceType_BINSponsor_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("serviceId", notNullValue())
                .body("active", equalTo(true))
                .body("cardBrand", equalTo(createServiceTypeModel.getCardBrand().name()))
                .body("binCurrency", equalTo(createServiceTypeModel.getBinCurrency().name()))
                .body("cardMode", equalTo(createServiceTypeModel.getCardMode().name()))
                .body("cardProcessingServiceId", equalTo("1"))
                .body("identityType", equalTo(createServiceTypeModel.getIdentityType().name()))
                .body("serviceId", notNullValue());

    }


    @Test
    public void CreateServiceType_BINSponsor_inactive_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .active(false)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("active", equalTo(false));
    }

    @Test
    public void CreateServiceType_EMI_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.EMI_LICENSE).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("identityTypes[0]", equalTo(IdentityType.CONSUMER.toString()))
                .body("identityTypes[1]", equalTo(IdentityType.CORPORATE.toString()))
                .body("category", equalTo(ServiceTypeCategory.IDENTITIES.name()))
                .body("serviceId", notNullValue());
    }

    @Test
    public void CreateServiceType_EMI_inactive_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.EMI_LICENSE).build();
        createServiceTypeModel.setActive(false);
        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("active", equalTo(false));
    }

    @Test
    public void CreateServiceType_CardBureau_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.CARDBUREAU_SERVICE)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("cardLevelClassification", equalTo(createServiceTypeModel.getCardLevelClassification().name()))
                .body("cardType", equalTo(createServiceTypeModel.getCardTypes().name()))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("deliveryAddressCountry", equalTo(createServiceTypeModel.getDeliveryAddressCountry().name()))
                .body("deliveryMethod", equalTo(createServiceTypeModel.getDeliveryMethod().name()))
                .body("maximumBatchSize", equalTo(createServiceTypeModel.getMaximumBatchSize()))
                .body("minimumBatchSize", equalTo(createServiceTypeModel.getMinimumBatchSize()))
                .body("serviceId", notNullValue());
    }

    @Test
    public void CreateServiceType_IbanProvider_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.IBAN_SERVICE).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("accountCurrencies", equalTo(List.of(createServiceTypeModel.getAccountCurrencies().get(0).toString(), createServiceTypeModel.getAccountCurrencies().get(1).toString())))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("channelProviders", equalTo(createServiceTypeModel.getChannelProviders()))
                .body("identityType", equalTo(createServiceTypeModel.getIdentityType().toString()))
                .body("paymentRails", equalTo(List.of(createServiceTypeModel.getPaymentRails().get(0).toString(), createServiceTypeModel.getPaymentRails().get(1).toString(), createServiceTypeModel.getPaymentRails().get(2).toString())))
                .body("virtualIban", equalTo(true))
                .body("serviceId", notNullValue());
    }

    @Test
    public void CreateServiceType_DigitalWallet_Success() {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.DIGITALWALLET_SERVICE)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("active", equalTo(true))
                .body("manualProvisioning", equalTo(true))
                .body("pushProvisioning", equalTo(true))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("serviceId", notNullValue());

    }

    @Test
    public void CreateServiceType_DigitalWalletSetProvisioningFieldsFalse_Success() {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.DIGITALWALLET_SERVICE)
                .manualProvisioning(false)
                .pushProvisioning(false)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("active", equalTo(true))
                .body("manualProvisioning", equalTo(false))
                .body("pushProvisioning", equalTo(false))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("serviceId", notNullValue());
    }


    @Test
    public void CreateServiceType_CardProcessing_Success() {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");


        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.CARDPROCESSING_SERVICE)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().name()))
                .body("category", equalTo(ServiceTypeCategory.INSTRUMENTS.name()))
                .body("active", equalTo(true))
                .body("serviceId", notNullValue())
                .body("cardFundingType", equalTo(List.of(createServiceTypeModel.getCardFundingType().get(0).name(), createServiceTypeModel.getCardFundingType().get(1).name())))
                .body("cardBureauServiceId", equalTo("1"))
                .body("digitalWalletServiceId", equalTo("1"))
                .body("transactionAmountLimit", equalTo(1000))
                .body("cardVelocity", equalTo(100))
                .body("cardValidity", equalTo(12));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void GetProviderServices_Paginated_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, null, null, 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("services.serviceType", equalTo(List.of(serviceType.name())));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void GetProviderServices_Paginated_by_ExistingServiceType_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, serviceType.name(), null, null, 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("services.serviceType", equalTo(List.of(serviceType.name())));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void GetProviderServices_Paginated_by_NonExistingServiceType_EmptyResponse(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, "UNKNOWN", null, null, 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0))
                .body("services", equalTo(null));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void GetProviderServices_Paginated_by_ExistingServiceId_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType).build();

        final String serviceId = ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .jsonPath()
                .get("serviceId");

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, serviceId, null, 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("services.serviceId", equalTo(List.of(serviceId)));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void GetProviderServices_Paginated_by_NonExistingServiceId_EmptyResponse(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, "111", null, 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0))
                .body("services", equalTo(null));

    }

    @Test
    public void CreateServiceType_NonExistentTPR_NotFound() {
        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.getRandomServiceType())
                .cardBrand(CardBrand.VISA)
                .binCurrency(Currency.EUR)
                .identityTypes(List.of(IdentityType.CORPORATE, IdentityType.CONSUMER))
                .cardMode(CardModeThirdPartyRegistry.DEBIT_MODE)
                .cardTypes(InstrumentType.getRandomInstrumentType())
                .channelProviders(List.of("channelProviders"))
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, "UNKNOWN")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreateServiceType_ExistingID_Created() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.getRandomServiceType())
                .cardBrand(CardBrand.VISA)
                .binCurrency(Currency.EUR)
                .identityTypes(List.of(IdentityType.CORPORATE, IdentityType.CONSUMER))
                .cardMode(CardModeThirdPartyRegistry.DEBIT_MODE)
                .cardTypes(InstrumentType.getRandomInstrumentType())
                .channelProviders(List.of("channelProviders"))
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);
    }

    @Test
    public void CreateServiceType_ExistingID_DifferentTPR_Success() {
        final String providerKey1 = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final String providerKey2 = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .cardBrand(CardBrand.VISA)
                .binCurrency(Currency.EUR)
                .identityType(IdentityType.CORPORATE)
                .cardMode(CardModeThirdPartyRegistry.DEBIT_MODE)
                .channelProviders(List.of("channelProviders"))
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey1)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().toString()))
                .body("active", equalTo(true))
                .body("cardBrand", equalTo(createServiceTypeModel.getCardBrand().name()))
                .body("binCurrency", equalTo(createServiceTypeModel.getBinCurrency().name()))
                .body("cardMode", equalTo(createServiceTypeModel.getCardMode().name()))
                .body("cardProcessingServiceId", equalTo("1"))
                .body("identityType", equalTo(createServiceTypeModel.getIdentityType().name()))
                .body("serviceId", notNullValue());

        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, providerKey1)
                .then()
                .statusCode(SC_OK)
                .body("providerKey", equalTo(providerKey1))
                .body("services", equalTo(List.of(createServiceTypeModel.getServiceType().toString())));

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey2)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(createServiceTypeModel.getServiceType().toString()))
                .body("active", equalTo(true))
                .body("cardBrand", equalTo(createServiceTypeModel.getCardBrand().name()))
                .body("binCurrency", equalTo(createServiceTypeModel.getBinCurrency().name()))
                .body("cardMode", equalTo(createServiceTypeModel.getCardMode().name()))
                .body("cardProcessingServiceId", equalTo("1"))
                .body("identityType", equalTo(createServiceTypeModel.getIdentityType().name()))
                .body("serviceId", notNullValue());

        ThirdPartyRegistryService.getThirdPartyRegistry(adminRootUserToken, providerKey2)
                .then()
                .statusCode(SC_OK)
                .body("providerKey", equalTo(providerKey2))
                .body("services", equalTo(List.of(createServiceTypeModel.getServiceType().toString())));
    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void UpdateServiceType_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .active(false)
                .build();

        final String serviceId = ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .jsonPath()
                .get("serviceId");

        UpdateServiceTypeModel updateServiceTypeModel = UpdateServiceTypeModel.defaultUpdateServiceTypeModel()
                .active(true)
                .build();

        ThirdPartyRegistryService.updateServiceType(updateServiceTypeModel, adminRootUserToken, providerKey, serviceId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, serviceType.name(), serviceId, null, 1, 0)
                .then()
                .statusCode(SC_OK)
                .body("services.serviceId", equalTo(List.of(serviceId)))
                .body("services.active", equalTo(List.of(true)));

        UpdateServiceTypeModel updateServiceTypeModelInactive = UpdateServiceTypeModel.defaultUpdateServiceTypeModel()
                .active(false)
                .build();

        ThirdPartyRegistryService.updateServiceType(updateServiceTypeModelInactive, adminRootUserToken, providerKey, serviceId)
                .then()
                .statusCode(SC_NO_CONTENT);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, serviceType.name(), serviceId, null, 1, 0)
                .then()
                .statusCode(SC_OK)
                .body("services.serviceId", equalTo(List.of(serviceId)))
                .body("services.active", equalTo(List.of(false)));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void FilterServiceType_byStatus_ACTIVE_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .active(true)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, null, "ACTIVE", 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("services.active", equalTo(List.of(true)));

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, null, "INACTIVE", 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount",equalTo(0))
                .body("services", equalTo(null));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void FilterServiceType_byStatus_INACTIVE_Success(final ServiceType serviceType) {

        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .active(false)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED);

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, null, "INACTIVE", 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("services.active", equalTo(List.of(false)));

        ThirdPartyRegistryService.getThirdPartyProviderServices(adminRootUserToken, providerKey, null, null, "ACTIVE", 100, 0)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount",equalTo(0))
                .body("services", equalTo(null));

    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void DeleteServiceType_Success(final ServiceType serviceType) {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .active(false)
                .build();

        final String serviceId = ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .jsonPath()
                .get("serviceId");

        ThirdPartyRegistryService.deleteServiceType(adminRootUserToken, providerKey, serviceId)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @ParameterizedTest
    @EnumSource(value = ServiceType.class, mode = EXCLUDE, names = {"UNKNOWN"})
    public void DeleteServiceType_Active_Conflict(final ServiceType serviceType) {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.defaultCreateServiceTypeModel(serviceType)
                .active(true)
                .build();

        final String serviceId = ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .jsonPath()
                .get("serviceId");

        ThirdPartyRegistryService.deleteServiceType(adminRootUserToken, providerKey, serviceId)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void DeleteServiceType_NonExistent_Conflict() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        String serviceId = "11111";

        ThirdPartyRegistryService.deleteServiceType(adminRootUserToken, providerKey, serviceId)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void CreateServiceType_WithoutRequiredFields_BadRequest() {
        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.commonCreateServiceTypeWithoutRequiredModel(ServiceType.EMI_LICENSE).build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, ServiceType.EMI_LICENSE.toString())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.active: must not be null"))
                .body("_embedded.errors[1].message", equalTo("request.identityTypes: must not be null"));
    }

    @Test
    public void CreateServiceType_AllFieldsInModel_Success() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        List<IdentityType> identityTypes = new ArrayList<>();
        identityTypes.add(IdentityType.CONSUMER);

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.commonCreateServiceTypeModel(ServiceType.EMI_LICENSE)
                .identityTypes(identityTypes)
                .kycLevels(KycLevel.KYC_LEVEL_2)
                .kybLevels(KybLevel.FULL_KYB_CHECK)
                .countryOfResidence("MT")
                .countryOfIncorporation("US")
                .transactionType(TransactionType.getRandomTransactionType().toString())
                .currencies(Currency.getRandomWithExcludedCurrency(Currency.GBP))
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(ServiceType.EMI_LICENSE.toString()))
                .body("active", equalTo(true))
                .body("category", equalTo(ServiceTypeCategory.IDENTITIES.name()))
                .body("countryOfIncorporation[0]", equalTo(createServiceTypeModel.getCountryOfIncorporation()))
                .body("countryOfResidence[0]", equalTo(createServiceTypeModel.getCountryOfResidence()))
                .body("currencies[0]", equalTo(createServiceTypeModel.getCurrencies().toString()))
                .body("identityTypes[0]", equalTo(createServiceTypeModel.getIdentityTypes().get(0).toString()))
                .body("kybLevels[0]", equalTo(createServiceTypeModel.getKybLevels().toString()))
                .body("kycLevels[0]", equalTo(createServiceTypeModel.getKycLevels().toString()))
                .body("transactionType[0]", equalTo(createServiceTypeModel.getTransactionType()));
    }

    @ParameterizedTest
    @EnumSource(value = TransactionType.class)
    public void CreateServiceType_DifferentTransactionTypes_Success(final TransactionType transactionType) {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        List<IdentityType> identityTypes = new ArrayList<>();
        identityTypes.add(IdentityType.CORPORATE);

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.commonCreateServiceTypeModel(ServiceType.EMI_LICENSE)
                .identityTypes(identityTypes)
                .kycLevels(KycLevel.KYC_LEVEL_2)
                .kybLevels(KybLevel.FULL_KYB_CHECK)
                .countryOfResidence("MT")
                .countryOfIncorporation("US")
                .transactionType(transactionType.toString())
                .currencies(Currency.getRandomWithExcludedCurrency(Currency.GBP))
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_CREATED)
                .body("serviceType", equalTo(ServiceType.EMI_LICENSE.toString()))
                .body("active", equalTo(true))
                .body("category", equalTo(ServiceTypeCategory.IDENTITIES.name()))
                .body("countryOfIncorporation[0]", equalTo(createServiceTypeModel.getCountryOfIncorporation()))
                .body("countryOfResidence[0]", equalTo(createServiceTypeModel.getCountryOfResidence()))
                .body("currencies[0]", equalTo(createServiceTypeModel.getCurrencies().toString()))
                .body("identityTypes[0]", equalTo(createServiceTypeModel.getIdentityTypes().get(0).toString()))
                .body("kybLevels[0]", equalTo(createServiceTypeModel.getKybLevels().toString()))
                .body("kycLevels[0]", equalTo(createServiceTypeModel.getKycLevels().toString()))
                .body("transactionType[0]", equalTo(createServiceTypeModel.getTransactionType()));
    }

    @Test
    public void CreateServiceType_CountryLongerThenTwoCharacters_BadRequest() {
        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.commonCreateServiceTypeModel(ServiceType.EMI_LICENSE)
                .countryOfResidence("MTA")
                .countryOfIncorporation("USA")
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, ServiceType.EMI_LICENSE.toString())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.countryOfIncorporation: size must be between 2 and 2"))
                .body("_embedded.errors[1].message", equalTo("request.countryOfResidence: size must be between 2 and 2"))
                .body("_embedded.errors[2].message", equalTo("request.identityTypes: must not be null"));
    }

    @Test
    public void CreateServiceType_RequiredFieldsBinSponsor_BadRequest() {
        final String providerKey = createThirdPartyRegistry()
                .jsonPath()
                .get("providerKey");

        final CreateServiceTypeModel createServiceTypeModel = CreateServiceTypeModel.commonCreateServiceTypeModel(ServiceType.BIN_SPONSORSHIP)
                .build();

        ThirdPartyRegistryService.createServiceType(createServiceTypeModel, adminRootUserToken, providerKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.binCountry: must not be blank"))
                .body("_embedded.errors[1].message", equalTo("request.binCurrency: must not be blank"))
                .body("_embedded.errors[2].message", equalTo("request.cardBrand: must not be null"))
                .body("_embedded.errors[3].message", equalTo("request.cardMode: must not be null"))
                .body("_embedded.errors[4].message", equalTo("request.cardProcessingServiceId: must not be blank"))
                .body("_embedded.errors[5].message", equalTo("request.identityType: must not be null"));
    }
}
