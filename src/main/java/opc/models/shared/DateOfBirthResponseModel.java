package opc.models.shared;

public class DateOfBirthResponseModel {

    private int year;
    private int month;
    private int day;

    public int getYear() {
        return year;
    }

    public DateOfBirthResponseModel setYear(int year) {
        this.year = year;
        return this;
    }

    public int getMonth() {
        return month;
    }

    public DateOfBirthResponseModel setMonth(int month) {
        this.month = month;
        return this;
    }

    public int getDay() {
        return day;
    }

    public DateOfBirthResponseModel setDay(int day) {
        this.day = day;
        return this;
    }
}
