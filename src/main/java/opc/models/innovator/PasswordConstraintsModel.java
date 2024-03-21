package opc.models.innovator;

import lombok.Getter;
import opc.enums.opc.PasswordConstraint;

@Getter
public class PasswordConstraintsModel {

  private String constraint;

  public PasswordConstraintsModel(final PasswordConstraint constraint) {
    this.constraint = constraint.name();
  }

  public PasswordConstraintsModel setConstraint(String constraint) {
    this.constraint = constraint;
    return this;
  }
}
