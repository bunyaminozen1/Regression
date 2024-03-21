package opc.models.sumsub.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AddSumSubQuestionnaireResponseModel {
    private String id;
    private String createdAt;
    private String key;
    private String clientId;
    private String inspectionId;
    private String externalUserId;
    private String sourceKey;
    @JsonIgnore
    private HashMap<String, String> info;
    @JsonIgnore
    private HashMap<String, String> idDocs;
    @JsonIgnore
    private HashMap<String, String> fixedInfo;
    private String email;
    private String applicantPlatform;
    private String ipCountry;
    @JsonIgnore
    private HashMap<String, String> requiredIdDocs;
    private String idDocSetType;
    private String questionnaireDefId;
    @JsonIgnore
    private HashMap<String, String> review;
    private String lang;
    private String type;
    private List<SumSubQuestionnairesModel> questionnaires;
    @JsonIgnore
    private HashMap<String, String> memberOf;
}
