package opc.enums.openbanking;

public enum OpenBankingConsentState {
  AWAITING_AUTHORISATION("AWAITING_AUTHORISATION"),
  PENDING_CHALLENGE("PENDING_CHALLENGE"),
  AUTHORISED("AUTHORISED"),
  REJECTED("REJECTED"),
  REVOKED("REVOKED");

  private final String name;

  OpenBankingConsentState(final String name){
    this.name = name;
  }

  public String getName(){
    return name;
  }
}


