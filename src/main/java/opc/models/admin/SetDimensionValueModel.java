package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetDimensionValueModel {
  private String key;
  private String value;

  public SetDimensionValueModel (final String key, final String value) {
    this.key = key;
    this.value = value;
  }
}
