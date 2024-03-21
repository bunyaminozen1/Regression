package opc.models.simulator;

import opc.models.shared.CurrencyAmount;

public class SimulateDepositByIbanModel {
  private final CurrencyAmount depositAmount;
  private final String iban;

  public SimulateDepositByIbanModel(CurrencyAmount depositAmount, String iban) {
    this.depositAmount = depositAmount;
    this.iban = iban;
  }

  public CurrencyAmount getDepositAmount() {
    return depositAmount;
  }

  public String getIban() {
    return iban;
  }
}
