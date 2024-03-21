package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpenBankingInstitutionResponseModel {

    private String id;
    private String displayName;
    private List<String> countries;
    private String environmentType;
    private String releaseStage;
    private InstitutionImagesResponse images;
    private InstitutionInfoResponse info;
    private InstitutionCapabilitiesResponse capabilities;
}
