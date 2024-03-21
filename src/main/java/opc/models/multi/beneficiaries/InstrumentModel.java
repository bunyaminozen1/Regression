package opc.models.multi.beneficiaries;

import com.fasterxml.jackson.annotation.JsonProperty;
import opc.enums.opc.ManagedInstrumentType;

public class InstrumentModel {
  @JsonProperty("id")
  private final String id;
  @JsonProperty("type")
  private final String type;


  public InstrumentModel(final InstrumentModel.Builder builder) {
    this.id = builder.id;
    this.type = builder.type;
  }

  private String getId()  { return id; }
  private String getType()  { return type; }


  public static class Builder {
    private String id;
    private String type;

    public InstrumentModel.Builder setId(String id) {
      this.id = id;
      return this;
    }

    public InstrumentModel.Builder setType(String type) {
      this.type = type;
      return this;
    }

    public InstrumentModel build() { return new InstrumentModel(this); }
  }

  public static InstrumentModel.Builder builder() {
    return new InstrumentModel.Builder();
  }

  public static InstrumentModel.Builder Instrument(final ManagedInstrumentType instrumentType,
                                                   final String instrumentId) {
    return instrumentType == ManagedInstrumentType.MANAGED_ACCOUNTS ? new Builder()
        .setId(instrumentId)
        .setType("managed_accounts") : new Builder()
        .setId(instrumentId)
        .setType("managed_cards");
  }
}