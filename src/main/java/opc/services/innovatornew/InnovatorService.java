package opc.services.innovatornew;

import commons.services.BaseService;
import io.restassured.response.Response;
import opc.models.admin.OwtProgrammes;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ConsumeInviteInnovatorModel;
import opc.models.innovator.ConsumersFilterModel;
import opc.models.innovator.CorporatesFilterModel;
import opc.models.innovator.CreateManagedCardsProfileV2Model;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.GetProgrammeKeysModel;
import opc.models.innovator.InnovatorRoleModel;
import opc.models.innovator.InviteInnovatorUserModel;
import opc.models.innovator.PasswordConstraintsModel;
import opc.models.innovator.UpdateConsumerModel;
import opc.models.innovator.UpdateCorporateModel;
import opc.models.innovator.UpdateCorporateUserModel;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.innovator.ValidateInnovatorInvite;
import opc.models.multi.passwords.LostPasswordStartModel;

import java.util.Optional;

public class InnovatorService extends BaseService {

    public static Response getConsumers(final ConsumersFilterModel consumersFilterModel,
                                        final String token) {
        return getBodyAuthenticatedRequest(consumersFilterModel, token)
                .when()
                .post("/innovator_new/consumers/find");
    }

    public static Response getConsumer(final String consumerId,
                                       final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .get("/innovator_new/consumers/{id}");
    }

    public static Response updateConsumer(final UpdateConsumerModel updateConsumerModel,
                                          final String token,
                                          final String consumerId) {
        return getBodyAuthenticatedRequest(updateConsumerModel, token)
                .pathParam("id", consumerId)
                .when()
                .patch("/innovator_new/consumers/{id}");
    }

    public static Response activateConsumer(final ActivateIdentityModel activateIdentityModel,
                                            final String consumerId,
                                            final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", consumerId)
                .when()
                .post("/innovator_new/consumers/{id}/activate");
    }

    public static Response activateConsumerUser(final String consumerId,
                                                final String consumerUserId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .pathParam("user_id", consumerUserId)
                .when()
                .post("/innovator_new/consumers/{id}/users/{user_id}/activate");
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String corporateUserId,
                                                   final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/innovator_new/corporates/{id}/users/{user_id}/deactivate");
    }

    public static Response getCorporates(final CorporatesFilterModel corporatesFilterModel,
                                         final String token) {
        return getBodyAuthenticatedRequest(corporatesFilterModel, token)
                .when()
                .post("/innovator_new/corporates/find");
    }

    public static Response getCorporate(final String token,
                                        final String corporateId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .when()
                .get("/innovator_new/corporates/{id}");
    }

    public static Response updateCorporate(final UpdateCorporateModel updateCorporateModel,
                                           final String token,
                                           final String corporateId) {
        return getBodyAuthenticatedRequest(updateCorporateModel, token)
                .pathParam("id", corporateId)
                .when()
                .patch("/innovator_new/corporates/{id}");
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                             final String corporateId,
                                             final String token) {
        return getBodyAuthenticatedRequest(activateIdentityModel, token)
                .pathParam("id", corporateId)
                .when()
                .post("/innovator_new/corporates/{id}/activate");
    }

    public static Response updateCorporateUser(
            final UpdateCorporateUserModel updateCorporateUserModel,
            final String token,
            final String corporateId,
            final String userId) {
        return getBodyAuthenticatedRequest(updateCorporateUserModel, token)
                .pathParam("id", corporateId)
                .pathParam("user_id", userId)
                .when()
                .patch("/innovator_new/corporates/{id}/users/{user_id}");
    }

    public static Response activateCorporateUser(final String corporateId,
                                                 final String corporateUserId,
                                                 final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("id", corporateId)
                .pathParam("user_id", corporateUserId)
                .when()
                .post("/innovator_new/corporates/{id}/users/{user_id}/activate");
    }

    public static Response createProgramme(final CreateProgrammeModel createProgrammeModel,
                                           final String token) {
        return getBodyAuthenticatedRequest(createProgrammeModel, token)
                .when()
                .post("/innovator_new/programmes");
    }

    public static Response getProgramme(final String token,
                                        final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .patch("/innovator_new/programmes/{programme_id}");
    }

    public static Response getProgrammeDetails(final String token, final String programmeId) {
        return getBodyAuthenticatedRequest(GetProgrammeKeysModel.getActiveKeysModel(), token)
                .pathParam("programme_id", programmeId)
                .when()
                .get("/innovator_new/programmes/{programme_id}/keys");
    }

    public static Response updateProgramme(final UpdateProgrammeModel updateProgrammeModel,
                                           final String programmeId,
                                           final String token) {
        return getBodyAuthenticatedRequest(updateProgrammeModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .patch("/innovator_new/programmes/{programme_id}");
    }

    public static Response inviteNewUser(final InviteInnovatorUserModel inviteInnovatorUserModel,
                                         final String token) {

        return getAuthenticatedRequest(token)
                .body(inviteInnovatorUserModel)
                .when()
                .post("/innovator_new/gateway/invites");
    }

    public static Response validateInviteNewUser(final ValidateInnovatorInvite validateInnovatorInvite,
                                                 final String inviteId) {
        return getBodyRequest(validateInnovatorInvite)
                .pathParam("invite_id", inviteId)
                .when()
                .post("/innovator_new/gateway/invites/{invite_id}/validate");
    }

    public static Response consumeInviteInnovator(final ConsumeInviteInnovatorModel consumeInviteInnovatorModel,
                                                  final String inviteId) {
        return getBodyRequest(consumeInviteInnovatorModel)
                .pathParam("invite_id", inviteId)
                .post("/innovator_new/gateway/invites/{invite_id}/consume");
    }

    public static Response innovatorUpdateRole(final InnovatorRoleModel innovatorRoleModel,
                                               final String userId,
                                               final String token) {
        return getBodyAuthenticatedRequest(innovatorRoleModel, token)
                .pathParam("user_id", userId)
                .when()
                .put("/innovator_new/gateway/users/{user_id}/role");
    }

    public static Response getInvites(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("/innovator_new/gateway/invites");
    }

    public static Response getCurrentUser(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("/innovator_new/gateway/users/self");
    }

    public static Response getUsers(final String token) {
        return getBodyAuthenticatedRequest("{}", token)
                .when()
                .post("/innovator_new/gateway/users/find");
    }

    public static Response getUser(final String token,
                                   final String userId) {
        return getAuthenticatedRequest(token)
                .when()
                .pathParam("user_id", userId)
                .get("/innovator_new/gateway/users/{user_id}");
    }


    public static Response getPlugins(final String token) {
        return getAuthenticatedRequest(token)
                .when()
                .get("/innovator_new/plugin_registry/plugins");
    }

    public static Response getPluginById(final String id,
                                         final String token) {
        return getAuthenticatedRequest(token)
                .params("id", id)
                .when()
                .get("/innovator_new/plugin_registry/plugins");
    }

    public static Response getPluginByName(final String code,
                                           final String token) {
        return getAuthenticatedRequest(token)
                .queryParam("code", code)
                .when()
                .get("/innovator_new/plugin_registry/plugins");
    }
    public static Response getProfileConstraint(final String programmeId,
                                                final String token) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .when()
                .get("/innovator_new/passwords/programmes/{programme_id}/profiles/constraints");
    }

    public static Response updateProfileConstraint(
            final PasswordConstraintsModel passwordConstraintsModel,
            final String programmeId,
            final String token) {
        return getBodyAuthenticatedRequest(passwordConstraintsModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .patch("/innovator_new/passwords/programmes/{programme_id}/profiles/constraints");
    }
    public static Response startLostPassword(final LostPasswordStartModel lostPasswordStartModel){
        return getBodyRequest(lostPasswordStartModel)
                .when()
                .post("/innovator_new/passwords/lost_password/start");
    }
    public static Response getOWTProfileId(final String token,
                                           final String programmeId,
                                           final String owtProfileId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", owtProfileId)
                .get("/innovator_new/outgoing_wire_transfers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response createOWTProfileId(final String token,
                                              final OwtProgrammes owtProgrammes,
                                              final String programmeId) {
        return getBodyAuthenticatedRequest(owtProgrammes, token)
                .pathParam("programme_id", programmeId)
                .post("/innovator_new/outgoing_wire_transfers/programmes/{programme_id}/profiles");
    }

    public static Response updateOWTProfileId(final String token,
                                              final OwtProgrammes owtProgrammes,
                                              final String programmeId,
                                              final String profileId) {
        return getBodyAuthenticatedRequest(owtProgrammes, token)
                .pathParam("programme_id", programmeId)
                .pathParam("profile_id", profileId)
                .patch("/innovator_new/outgoing_wire_transfers/programmes/{programme_id}/profiles/{profile_id}");
    }

    public static Response getCorporatesDetails(final String token,
                                                final String corporateId) {
        return getBodyAuthenticatedRequest(Optional.empty(), token)
                .pathParam("id", corporateId)
                .when()
                .post("innovator_new/corporates/{id}/users/find");
    }

    public static Response getPluginsOnProgramme (final String token,
                                                  final String programmeId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .get("/innovator_new/programmes/{programme_id}/installed");
    }

    public static Response linkPluginToProgramme (final String token,
                                                  final String programmeId,
                                                  final String pluginId) {
        return getAuthenticatedRequest(token)
                .pathParam("programme_id", programmeId)
                .pathParam("plugin_id", pluginId)
                .post("/innovator_new/programmes/{programme_id}/installed/{plugin_id}");
    }

    public static Response getConsumerKyc(final String token,
                                          final String consumerId) {

        return getAuthenticatedRequest(token)
                .pathParam("id", consumerId)
                .when()
                .get("/innovator_new/consumers/{id}/kyc");
    }

    public static Response createManagedCardsProfileV2(
            final CreateManagedCardsProfileV2Model createManagedCardProfileModel,
            final String token,
            final String programmeId) {
        return getBodyAuthenticatedRequest(createManagedCardProfileModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post("/innovator_new/managed_cards/programmes/{programme_id}/profiles");
    }
}
