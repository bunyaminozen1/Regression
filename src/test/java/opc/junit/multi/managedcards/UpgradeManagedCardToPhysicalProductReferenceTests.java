package opc.junit.multi.managedcards;

import java.util.Arrays;
import java.util.UUID;
import java.util.Collections;
import java.util.List;
import opc.enums.opc.CardBureau;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.innovator.ContextDimensionKeyValueModel;
import opc.models.innovator.ContextDimensionPartModel;
import opc.models.innovator.ContextDimensionValueModel;
import opc.models.innovator.ContextDimensionsModel;
import opc.models.innovator.ContextModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.ActivatePhysicalCardModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.managedcards.PatchManagedCardModel;
import opc.models.multi.managedcards.PhysicalCardAddressModel;
import opc.models.multi.managedcards.ReplacePhysicalCardModel;
import opc.models.multi.managedcards.UpgradeToPhysicalCardModel;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
// Temporary Execution Mode, Remove after DEV-4892
@Execution(ExecutionMode.SAME_THREAD)
public class UpgradeManagedCardToPhysicalProductReferenceTests extends BaseManagedCardsSetup {

  /**
   * Upgrade Managed Card to Physical - Maximum Allowed Characters tests
   * Covering nameOnCard, nameOnCardLine2 and nameOnCardLine2 when provided in upgrade to physical payload
   * The Maximum allowed characters for mentioned fields are specified as product reference by innovator as part of context property
   * If maximum allowed character length is not specified as context property, the default value is 20
   */

  private static String corporateAuthenticationToken;
  private static String corporateCurrency;
  private static CreateCorporateModel corporateDetails;
  final static Integer defaultMaxCharLength = 20;
  final static String defaultPhysicalProduct = String.format("NTCRST_PR_%s", UUID.randomUUID());
  final static String physicalProductLowMaxChars = "NTCRST_PR_LOW_MAX_CHARS";
  final static String physicalProductHighMaxChars = "NTCRST_PR_HIGH_MAX_CHARS";
  final static String physicalProductVariableMaxChars = "NTCRST_PR_VARIABLE_MAX_CHARS";

  @BeforeAll
  public static void Setup(){
    corporateSetup();
    productReferenceSetup();
  }

  @AfterAll
  public static void TearDown(){
    defaultProductReferenceSetup();
  }

  // Move to BeforeAll and AfterAll after DEV-
  @BeforeEach
  public  void DefaultSetup(){
    defaultMaxCharLengthSetup(defaultMaxCharLength);
  }

  @AfterEach
  public  void DefaultTearDown(){
    defaultMaxCharLengthSetup(defaultMaxCharLength);
  }


  @Nested
  @DisplayName("Main Tests")
  class MainTests {
    @Test
    public void UpgradeManagedCard_ProductReferenceNotSpecified_Conflict() {

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(null)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("PRODUCT_REFERENCE_NOT_SPECIFIED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharBelowAllowedDefault_Success() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(19));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2MaxCharBelowAllowedDefault_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(19));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharAboveAllowedDefault_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(21));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnLine2CardMaxCharAboveAllowedDefault_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(21));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharAboveSetValue_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2MaxCharAboveSetValue_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharBelowSetValue_Success() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(24));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(physicalProductHighMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2MaxCharBelowSetValue_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(24));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductHighMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2OnUpgradeMaxCharBelowDefaultValue_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(19));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(null)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2OnUpgradeMaxCharAboveDefaultValue_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(21));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(null)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2OnUpgradeMaxCharBelowSetValue_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(24));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final String nameOnCardLine2OnUpgrade = RandomStringUtils.randomAlphabetic(9);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2OnUpgrade)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2OnUpgrade))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2OnUpgrade));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2OnUpgradeMaxCharAboveAllowedDefault_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final String nameOnCardLine2OnUpgrade = RandomStringUtils.randomAlphabetic(16);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2OnUpgrade)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardSwitchProductReferenceToAllowCharacters_Success() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);


      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModelHighMaxChars =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(physicalProductHighMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModelHighMaxChars, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));
    }

    @Test
    public void UpgradeManagedCard_PatchNameOnCardToAllowCharacters_Success() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);


      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      final String patchedNameOnCard = String.format(RandomStringUtils.randomAlphabetic(14));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCard(patchedNameOnCard)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));
    }

    @Test
    public void UpgradeManagedCard_ReplaceDamagedCardNameOnCardMaxCharactersExceeded_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      final String patchedNameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCard(patchedNameOnCard)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_ReplaceLostCardNameOnCardMaxCharactersExceeded_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      final String patchedNameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCard(patchedNameOnCard)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_ReplaceLostCardNameOnCardLine2MaxCharactersExceeded_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2));

      final String patchedNameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCardLine2(patchedNameOnCardLine2)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_ReplaceStolenCardNameOnCardMaxCharactersExceeded_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      final String patchedNameOnCard = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCard(patchedNameOnCard)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(patchedNameOnCard));

      ManagedCardsService.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_ReplaceStolenCardNameOnCardLine2MaxCharactersExceeded_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2));

      final String patchedNameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCardLine2(patchedNameOnCardLine2)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_ReplaceDamagedCardNameOnCardLine2MaxCharactersExceeded_Conflict() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(14));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));

      final String patchedNameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCardLine2(patchedNameOnCardLine2)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_PatchNameOnCardLine2ToAllowCharacters_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);


      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      final String patchedNameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(14));
      final PatchManagedCardModel patchManagedCardModel =
          PatchManagedCardModel.DefaultPatchManagedCardModel()
              .setNameOnCardLine2(patchedNameOnCardLine2)
              .build();

      ManagedCardsService.patchManagedCard(patchManagedCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2));

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(patchedNameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(patchedNameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2SwitchProductReferenceToAllowCharacters_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);


      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModelHighMaxChars =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductHighMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModelHighMaxChars, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2OnUpgradeSwitchProductReferenceToAllowCharacters_Success() {

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(16));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .build(),
          secretKey, corporateAuthenticationToken);


      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2)
              .setProductReference(physicalProductLowMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModelHighMaxChars =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(nameOnCardLine2)
              .setProductReference(physicalProductHighMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModelHighMaxChars, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeDebitManagedCard_NameOnCardMaxCharBelowAllowedDefault_Success() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(19));

      final CreateManagedAccountModel createManagedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build();

      final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken );

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getDebitManagedCardModel(managedAccountId)
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));
    }

    @Test
    public void UpgradeDebitManagedCard_NameOnCardMaxCharAboveAllowedDefault_Conflict() {

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(21));

      final CreateManagedAccountModel createManagedAccountModel = CreateManagedAccountModel.DefaultCreateManagedAccountModel(corporateManagedAccountsProfileId, corporateCurrency).build();

      final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken );

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getDebitManagedCardModel(managedAccountId)
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));
    }

    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharBelowAllowedSetAfterProductReferenceUpdate_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars(innovatorId, physicalProductVariableMaxChars, 10, adminToken);

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(11));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(physicalProductVariableMaxChars)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_CONFLICT)
          .body("errorCode", equalTo("MAX_PRINT_CHARACTERS_EXCEEDED"));

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars(innovatorId, physicalProductVariableMaxChars, 12, adminToken);

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));
    }
  }

  // Remove after DEV-4892
  @Nested
  @DisplayName("Temporary Tests to ensure production compatibility")
  class TemporaryTests {
    @Test
    public void UpgradeManagedCard_NameOnCardMaxCharSameAsDefault_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( 27, adminToken);

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(27));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

    }

    @Test
    public void UpgradeManagedCard_NameOnCardLine2MaxCharSameAsDefault_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( 27, adminToken);

      final String nameOnCardLine2 = String.format(RandomStringUtils.randomAlphabetic(27));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCardLine2(nameOnCardLine2)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCardLine2", equalTo(nameOnCardLine2))
          .body("physicalCardDetails.nameOnCardLine2", equalTo(nameOnCardLine2));
    }

    @Test
    public void UpgradeManagedCard_ReplaceDamagedCardNameOnCardMaxCharactersNotExceeded_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( 27, adminToken);

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(25));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      ManagedCardsService.replaceDamagedCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void UpgradeManagedCard_ReplaceLostCardNameOnCardMaxCharactersNotExceeded_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( 27, adminToken);

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(25));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      ManagedCardsService.reportLostCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK);
    }

    @Test
    public void UpgradeManagedCard_ReplaceStolenCardNameOnCardMaxCharactersNotExceeded_Success() {

      AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( 27, adminToken);

      final String nameOnCard = String.format(RandomStringUtils.randomAlphabetic(25));

      final String managedCardId = ManagedCardsHelper.createManagedCard(
          getPrepaidManagedCardModel()
              .setNameOnCard(nameOnCard)
              .build(),
          secretKey, corporateAuthenticationToken);

      final UpgradeToPhysicalCardModel upgradeToPhysicalCardModel =
          UpgradeToPhysicalCardModel.DefaultUpgradeToPhysicalCardModel(PhysicalCardAddressModel.DefaultPhysicalCardAddressModel().build(), TestHelper.VERIFICATION_CODE)
              .setNameOnCardLine2(null)
              .setProductReference(defaultPhysicalProduct)
              .build();

      ManagedCardsService.upgradeManagedCardToPhysical(upgradeToPhysicalCardModel, secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      ManagedCardsService.activatePhysicalCard(new ActivatePhysicalCardModel(TestHelper.VERIFICATION_CODE), secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK)
          .body("nameOnCard", equalTo(nameOnCard));

      ManagedCardsService.reportStolenCard(secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_NO_CONTENT);

      ManagedCardsService.replaceLostOrStolenCard(new ReplacePhysicalCardModel(TestHelper.VERIFICATION_CODE),secretKey, managedCardId, corporateAuthenticationToken)
          .then()
          .statusCode(SC_OK);
    }
  }

  private static void corporateSetup() {
    corporateDetails =
        CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

    final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
    corporateAuthenticationToken = authenticatedCorporate.getRight();
    corporateCurrency = corporateDetails.getBaseCurrency();

    CorporatesHelper.verifyKyb(secretKey, authenticatedCorporate.getLeft());
  }
  private static void defaultMaxCharLengthSetup(final Integer maxCharLength) {
    AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars( maxCharLength, adminToken);
  }
  private static void productReferenceSetup() {
    List<ContextDimensionPartModel> physicalProductReferences = Arrays.asList(
        new ContextDimensionPartModel(Collections.singletonList(defaultPhysicalProduct)),
        new ContextDimensionPartModel(Collections.singletonList(physicalProductLowMaxChars)),
        new ContextDimensionPartModel(Collections.singletonList(physicalProductHighMaxChars)),
        new ContextDimensionPartModel(Collections.singletonList(physicalProductVariableMaxChars))
    );

    final ContextModel contextModel =
        ContextModel.builder()
            .setContext(new ContextDimensionsModel(Arrays.asList(
                new ContextDimensionKeyValueModel("TenantIdDimension", innovatorId, false),
                new ContextDimensionKeyValueModel("ManagedCardPhysicalBureau", CardBureau.NITECREST.name(), false))))
            .setSet(new ContextDimensionValueModel(physicalProductReferences))
            .build();

    // Set 3 product references for an innovator
    AdminHelper.setManagedCardsProductReference(contextModel, adminToken);
    // Set max chars allowed when upgrading to physical to second and third product reference
    AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars(innovatorId, physicalProductLowMaxChars, 15, adminToken);
    AdminHelper.setManagedCardsProductReferenceNameOnCardMaxChars(innovatorId, physicalProductHighMaxChars, 25, adminToken);
  }

  private static void defaultProductReferenceSetup() {
    final ContextModel contextModel =
        ContextModel.builder()
            .setContext(new ContextDimensionsModel(Arrays.asList(
                new ContextDimensionKeyValueModel("TenantIdDimension", innovatorId, false),
                new ContextDimensionKeyValueModel("ManagedCardPhysicalBureau", CardBureau.NITECREST.name(), false))))
            .setSet(new ContextDimensionValueModel(Collections.singletonList(
                new ContextDimensionPartModel(Collections.singletonList(CardBureau.NITECREST.getProductReference())))))
            .build();

    AdminHelper.setManagedCardsProductReference(contextModel, adminToken);
  }

  private CreateManagedCardModel.Builder getPrepaidManagedCardModel(){
    return CreateManagedCardModel
        .DefaultCreatePrepaidManagedCardModel(corporatePrepaidManagedCardsProfileId,
            corporateCurrency)
        .setCardholderMobileNumber(String.format("%s%s",
            corporateDetails.getRootUser().getMobile().getCountryCode(),
            corporateDetails.getRootUser().getMobile().getNumber()));
  }

  private CreateManagedCardModel.Builder getDebitManagedCardModel(final String managedAccountId){
    return CreateManagedCardModel
        .DefaultCreateDebitManagedCardModel(corporateDebitManagedCardsProfileId,
            managedAccountId)
        .setCardholderMobileNumber(String.format("%s%s",
            corporateDetails.getRootUser().getMobile().getCountryCode(),
            corporateDetails.getRootUser().getMobile().getNumber()));
  }
}