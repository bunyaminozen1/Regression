package opc.services.multi;

import io.restassured.response.Response;
import opc.enums.opc.AcceptedResponse;
import opc.models.multi.managedcards.*;
import commons.services.BaseService;

import java.util.Map;
import java.util.Optional;

public class ManagedCardsService extends BaseService {

    public static Response createManagedCard(final CreateManagedCardModel createManagedCardModel,
                                             final String secretKey,
                                             final String token,
                                             final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(createManagedCardModel, secretKey, token, idempotencyRef)
                .when()
                .post("/multi/managed_cards");
    }

    public static Response getManagedCards(final String secretKey,
                                           final Optional<Map<String, Object>> filters,
                                           final String token){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .when()
                .get("/multi/managed_cards");
    }

    public static Response getManagedCard(final String secretKey,
                                          final String managedCardId,
                                          final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/managed_cards/{id}");
    }

    public static Response patchManagedCard(final PatchManagedCardModel patchManagedCardModel,
                                            final String secretKey,
                                            final String managedCardId,
                                            final String token){
        return getBodyApiKeyAuthenticationRequest(patchManagedCardModel, secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .patch("/multi/managed_cards/{id}");
    }

    public static Response blockManagedCard(final String secretKey,
                                            final String managedCardId,
                                            final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/block");
    }

    public static Response unblockManagedCard(final String secretKey,
                                              final String managedCardId,
                                              final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/unblock");
    }

    public static Response removeManagedCard(final String secretKey,
                                             final String managedCardId,
                                             final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/remove");
    }

    public static Response getManagedCardStatement(final String secretKey,
                                                   final String managedCardId,
                                                   final String token,
                                                   final Optional<Map<String, Object>> filters,
                                                   final AcceptedResponse acceptedResponse){
        return assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters)
                .header("accept", acceptedResponse.getAccept())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/managed_cards/{id}/statement");
    }

    public static Response getManagedCardStatement(final String secretKey,
                                                      final String managedCardId,
                                                      final String token,
                                                      final Optional<Map<String, Object>> filters,
                                                      final Optional<Map<String, Object>> headers){
        return assignHeaderParams(assignQueryParams(getApiKeyAuthenticationRequest(secretKey, token, Optional.empty()), filters), headers)
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/managed_cards/{id}/statement");
    }

    public static Response assignManagedCard(final AssignManagedCardModel assignManagedCardModel,
                                             final String secretKey,
                                             final String token){
        return getBodyApiKeyAuthenticationRequest(assignManagedCardModel, secretKey, token, Optional.empty())
                .when()
                .post("/multi/managed_cards/assign");
    }

    public static Response getManagedCardsSpendRules(final String secretKey,
                                                     final String managedCardId,
                                                     final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/managed_cards/{id}/spend_rules");
    }

    public static Response postManagedCardsSpendRules(final SpendRulesModel spendRulesModel,
                                                      final String secretKey,
                                                      final String managedCardId,
                                                      final String token,
                                                      final Optional<String> idempotencyRef){
        return getBodyApiKeyAuthenticationRequest(spendRulesModel, secretKey, token, idempotencyRef)
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/spend_rules");
    }

    public static Response patchManagedCardsSpendRules(final SpendRulesModel spendRulesModel,
                                                       final String secretKey,
                                                       final String managedCardId,
                                                       final String token){
        return getBodyApiKeyAuthenticationRequest(spendRulesModel, secretKey, token)
                .pathParam("id", managedCardId)
                .when()
                .patch("/multi/managed_cards/{id}/spend_rules");
    }

    public static Response deleteManagedCardsSpendRules(final String secretKey,
                                                        final String managedCardId,
                                                        final String token){
        return getApiKeyAuthenticationRequest(secretKey, token)
                .pathParam("id", managedCardId)
                .when()
                .delete("/multi/managed_cards/{id}/spend_rules");
    }

    public static Response upgradeManagedCardToPhysical(final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel,
                                                        final String secretKey,
                                                        final String managedCardId,
                                                        final String token){
        return getBodyApiKeyAuthenticationRequest(upgradeToPhysicalCardModel, secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical");
    }

    public static Response activatePhysicalCard(final ActivatePhysicalCardModel activatePhysicalCardModel,
                                                final String secretKey,
                                                final String managedCardId,
                                                final String token){
        return getBodyApiKeyAuthenticationRequest(activatePhysicalCardModel, secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/activate");
    }

    public static Response getPhysicalCardPin(final String secretKey,
                                              final String managedCardId,
                                              final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .get("/multi/managed_cards/{id}/physical/pin");
    }

    public static Response unblockPhysicalCardPin(final String secretKey,
                                                  final String managedCardId,
                                                  final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .patch("/multi/managed_cards/{id}/physical/pin/unblock");
    }

    public static Response replaceDamagedCard(final ReplacePhysicalCardModel replacePhysicalCardModel,
                                              final String secretKey,
                                              final String managedCardId,
                                              final String token){
        return getBodyApiKeyAuthenticationRequest(replacePhysicalCardModel, secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/replace_damaged");
    }

    public static Response reportLostCard(final String secretKey,
                                          final String managedCardId,
                                           final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/report_lost");
    }

    public static Response reportStolenCard(final String secretKey,
                                            final String managedCardId,
                                            final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/report_stolen");
    }

    public static Response replaceLostOrStolenCard(final ReplacePhysicalCardModel replacePhysicalCardModel,
                                                   final String secretKey,
                                                   final String managedCardId,
                                                   final String token){
        return getBodyApiKeyAuthenticationRequest(replacePhysicalCardModel, secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/replace_lost_stolen");
    }

    public static Response resetContactlessLimit(final String secretKey,
                                                 final String managedCardId,
                                                 final String token){
        return getApiKeyAuthenticationRequest(secretKey, token, Optional.empty())
                .pathParam("id", managedCardId)
                .when()
                .post("/multi/managed_cards/{id}/physical/contactless_limit/reset");
    }
}
