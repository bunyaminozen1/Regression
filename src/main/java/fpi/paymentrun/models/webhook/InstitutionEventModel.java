package fpi.paymentrun.models.webhook;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
@Builder
public class InstitutionEventModel {
    private String id;
    private String displayName;
    private List<String> countries;
    private LinkedHashMap<String, String> images;
    private LinkedHashMap<String, String> info;

}
