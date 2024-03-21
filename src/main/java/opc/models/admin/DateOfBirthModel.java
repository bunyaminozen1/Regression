package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class DateOfBirthModel {

    private int year;
    private int month;
    private int day;
}
