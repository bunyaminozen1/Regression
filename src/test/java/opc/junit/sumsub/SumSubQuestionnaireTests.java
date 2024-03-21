package opc.junit.sumsub;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.equalTo;

import opc.junit.helpers.admin.AdminHelper;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@Execution(ExecutionMode.CONCURRENT)
public class SumSubQuestionnaireTests extends BaseSumSubSetup {

  @Test
  public void Consumer_SetSumSubQuestionnaireWhereDimensionIsQuestionnaireID_Success() {

    final SumSubQuestionnaireModel sumSubQuestionnaireModel = SumSubQuestionnaireModel.createSumSubQuestionnaire(
        "QuestionnaireId", "QuestionnaireItemId", "QuestionnaireItemValue");

    AdminService.setSumSubQuestionnaire(sumSubQuestionnaireModel, AdminService.loginAdmin())
        .then()
        .statusCode(SC_NO_CONTENT);
  }

  @ParameterizedTest()
  @CsvSource({
      "wrongQuestionnaireId,due.diligence.id,due.diligence.id"
  })
  public void Consumer_SetSumSubQuestionnaireWithWrongQuestionnaireID_BadRequest(
      final String sumSubQuestionnaireDimensionKey,
      final String sumSubQuestionnaireDimensionValue, final String sumSubValuePart) {
    SumSubQuestionnaireModel sumSubQuestionnaireModel = AdminHelper.setSumSubQuestionnaireContextProperties(
        sumSubQuestionnaireDimensionKey, sumSubQuestionnaireDimensionValue, sumSubValuePart);

    AdminService.setSumSubQuestionnaire(sumSubQuestionnaireModel, AdminService.loginAdmin())
        .then()
        .statusCode(SC_INTERNAL_SERVER_ERROR);
  }

  @ParameterizedTest
  @CsvSource({",,"})
  public void Consumer_SetSumSubQuestionnaireWithNullContextAndPartValues_BadRequest(
      final String sumSubQuestionnaireDimensionKey,
      final String sumSubQuestionnaireDimensionValue, final String sumSubValuePart) {

    SumSubQuestionnaireModel sumSubQuestionnaireModel = AdminHelper.setSumSubQuestionnaireContextProperties(
        sumSubQuestionnaireDimensionKey, sumSubQuestionnaireDimensionValue, sumSubValuePart);

    AdminService.setSumSubQuestionnaire(sumSubQuestionnaireModel, AdminService.loginAdmin())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message",
            equalTo("The request body does not match the schema or is not valid JSON."));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = {"\n", "\t"})
  public void Consumer_SetSumSubQuestionnaireWithValidationRulesForJsonContextAndPartValues_BadRequest(
      final String unacceptedParameterValue) {

    SumSubQuestionnaireModel sumSubQuestionnaireModel = AdminHelper.setSumSubQuestionnaireContextProperties(
        unacceptedParameterValue, unacceptedParameterValue, unacceptedParameterValue);

    AdminService.setSumSubQuestionnaire(sumSubQuestionnaireModel, AdminService.loginAdmin())
        .then()
        .statusCode(SC_BAD_REQUEST)
        .body("message",
            equalTo("JSON request violated validation rules"));
  }

  @ParameterizedTest()
  @CsvSource({
      "QuestionnaireId,due.diligence.id,due.diligence.id"
  })
  public void Consumer_SetSumSubQuestionnaireWithInvalidAccessToken_BadRequest(
      final String sumSubQuestionnaireDimensionKey,
      final String sumSubQuestionnaireDimensionValue, final String sumSubValuePart) {
    SumSubQuestionnaireModel sumSubQuestionnaireModel = AdminHelper.setSumSubQuestionnaireContextProperties(
        sumSubQuestionnaireDimensionKey, sumSubQuestionnaireDimensionValue, sumSubValuePart);
    String wrongToken = RandomStringUtils.randomNumeric(18);

    AdminService.setSumSubQuestionnaire(sumSubQuestionnaireModel, wrongToken)
        .then()
        .statusCode(SC_UNAUTHORIZED);
  }
}
