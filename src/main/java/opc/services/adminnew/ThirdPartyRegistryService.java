package opc.services.adminnew;

import io.restassured.response.Response;
import opc.models.admin.CreateServiceTypeModel;
import opc.models.admin.CreateThirdPartyRegistryModel;
import opc.models.admin.UpdateServiceTypeModel;
import opc.models.admin.UpdateThirdPartyRegistryModel;
import commons.services.BaseService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class ThirdPartyRegistryService extends BaseService {
    public static Response createThirdPartyRegistry(final CreateThirdPartyRegistryModel createThirdPartyRegistryModel,
                                                    final String token) {
        return getBodyAuthenticatedRequest(createThirdPartyRegistryModel, token)
                .when()
                .body(createThirdPartyRegistryModel)
                .post("admin_new/third_party_registry");
    }

    public static Response getThirdPartyRegistries(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("admin_new/third_party_registry");
    }

    public static Response getThirdPartyRegistriesPagination(final String token,
                                                             final Integer limit,
                                                             final Integer offset) {
        return getAuthenticatedRequest(token)
                .param("limit", limit)
                .param("offset", offset)
                .when()
                .get("admin_new/third_party_registry");
    }

    public static Response getThirdPartyRegistriesFiltered(final String token,
                                                           final Pair<String, String> param) {
        return getAuthenticatedRequest(token)
                .param(param.getKey(), param.getValue())
                .when()
                .get("admin_new/third_party_registry");
    }

    public static Response getThirdPartyRegistry(final String token,
                                                 final String providerKey) {
        return getAuthenticatedRequest(token)
                .pathParam("provider_key", providerKey)
                .when()
                .get("admin_new/third_party_registry/{provider_key}");
    }

    public static Response updateThirdPartyRegistry(final UpdateThirdPartyRegistryModel updateThirdPartyRegistryModel,
                                                    final String token,
                                                    final String providerKey) {
        return getBodyAuthenticatedRequest(updateThirdPartyRegistryModel, token)
                .pathParam("provider_key", providerKey)
                .when()
                .body(updateThirdPartyRegistryModel)
                .put("admin_new/third_party_registry/{provider_key}");
    }

    public static Response deleteThirdPartyRegistry(final String token,
                                                    final String providerKey) {
        return getAuthenticatedRequest(token)
                .pathParam("provider_key", providerKey)
                .when()
                .delete("admin_new/third_party_registry/{provider_key}");
    }

    public static Response createServiceType(final CreateServiceTypeModel createServiceTypeModel,
                                             final String token,
                                             final String providerKey) {
        return getBodyAuthenticatedRequest(createServiceTypeModel, token)
                .pathParam("provider_key", providerKey)
                .when()
                .body(createServiceTypeModel)
                .post("admin_new/third_party_registry/{provider_key}/services");
    }

    public static Response updateServiceType(final UpdateServiceTypeModel updateServiceTypeModel,
                                             final String token,
                                             final String providerKey,
                                             final String serviceKey) {
        return getBodyAuthenticatedRequest(updateServiceTypeModel, token)
                .pathParam("provider_key", providerKey)
                .pathParam("service_key", serviceKey)
                .when()
                .body(updateServiceTypeModel)
                .put("admin_new/third_party_registry/{provider_key}/services/{service_key}");
    }


    public static Response deleteServiceType(final String token,
                                             final String providerKey,
                                             final String serviceKey) {
        return getAuthenticatedRequest(token)
                .pathParam("provider_key", providerKey)
                .pathParam("service_key", serviceKey)
                .when()
                .delete("admin_new/third_party_registry/{provider_key}/services/{service_key}");
    }

    public static Response getThirdPartyProviderServices(
            final String token,
            final String providerKey,
            final String serviceType,
            final String serviceId,
            final String status,
            final Integer limit,
            final Integer offset) {

        Map<String, Object> queryParams = new HashMap<>();

        if(limit != null) {
            queryParams.put("limit", limit);
        }

        if(offset != null) {
            queryParams.put("offset", offset);
        }

        if(serviceType != null) {
            queryParams.put("serviceType", serviceType);
        }

        if(serviceId != null) {
            queryParams.put("serviceId", serviceId);
        }

        if(status != null) {
            queryParams.put("status", status);
        }

        return getAuthenticatedRequest(token)
                .pathParam("provider_key", providerKey)
                .queryParams(queryParams)
                .when()
                .get("admin_new/third_party_registry/{provider_key}/services");
    }

}
