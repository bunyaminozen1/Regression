package opc.models.multi.managedcards;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalDataModel {
  private String name;
  private String value;
}
