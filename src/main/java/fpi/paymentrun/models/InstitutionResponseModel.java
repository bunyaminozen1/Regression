package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;
import spi.openbanking.models.InstitutionImagesResponse;
import spi.openbanking.models.InstitutionInfoResponse;

import java.util.List;
@Getter
@Setter
public class InstitutionResponseModel {
    private String id;
    private String displayName;
    private List<String> countries;
    private String environmentType;
    private String releaseStage;
    private InstitutionImagesResponse images;
    private InstitutionInfoResponse info;
    private InstitutionCapabilitiesResponseModel capabilities;
}
