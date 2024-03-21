package spi.openbanking.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PayerModel {

    private String accountId;
}
