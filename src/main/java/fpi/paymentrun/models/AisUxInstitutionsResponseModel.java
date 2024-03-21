package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AisUxInstitutionsResponseModel {
    private String id;
    private String displayName;
    private List<String> countries;
    private String environmentType;
    private String releaseStage;
    private AisUxInstitutionImagesResponseModel images;
    private AisUxInstitutionInfoResponseModel info;

}
