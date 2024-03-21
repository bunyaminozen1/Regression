package opc.models.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import opc.enums.opc.ManagedInstrumentType;

public class ManagedInstrumentTypeId {
    @JsonProperty("id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String type;

    @JsonProperty("beneficiaryId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String beneficiaryId;

    public ManagedInstrumentTypeId(final String id, final ManagedInstrumentType type) {
        this.id = id;
        this.type = type.getValue();
    }

    public ManagedInstrumentTypeId(final String beneficiaryId) {
        this.beneficiaryId = beneficiaryId;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getBeneficiaryId() {
        return beneficiaryId;
    }
}
