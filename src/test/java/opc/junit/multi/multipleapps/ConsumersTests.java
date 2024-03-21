package opc.junit.multi.multipleapps;

import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class ConsumersTests extends BaseApplicationsSetup {

    private static String consumerProfileId;
    private static String secretKey;

    @BeforeAll
    public static void TestSetup(){
        consumerProfileId = applicationTwo.getConsumersProfileId();
        secretKey = applicationTwo.getSecretKey();
    }

    @Test
    public void CreateConsumer_Success(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id.type", equalTo("CONSUMER"))
                .body("profileId", equalTo(consumerProfileId));
    }

    @Test
    public void CreateConsumer_OtherApplicationKey_Forbidden(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        ConsumersService.createConsumer(createConsumerModel, applicationThree.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateConsumer_OtherApplicationConsumerProfile_Forbidden(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationFour.getConsumersProfileId()).build();

        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateConsumer_SameEmailDifferentApplications_Success(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationFour.getConsumersProfileId()).build();

        ConsumersService.createConsumer(createConsumerModel, applicationFour.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        final CreateConsumerModel otherApplicationConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationTwo.getConsumersProfileId())
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(createConsumerModel.getRootUser().getEmail())
                                .build())
                        .build();

        ConsumersService.createConsumer(otherApplicationConsumerModel, applicationTwo.getSecretKey(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchConsumer_CrossApplicationUpdate_Forbidden(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(applicationFour.getConsumersProfileId()).build();
        final Pair<String, String> businessPayoutsConsumer =
                ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, applicationFour.getSecretKey());

        final PatchConsumerModel patchConsumerModel = PatchConsumerModel.DefaultPatchConsumerModel().build();
        ConsumersService.patchConsumer(patchConsumerModel, secretKey, businessPayoutsConsumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

}
