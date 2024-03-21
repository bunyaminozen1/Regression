package opc.junit.database;

import opc.database.DatabaseHelper;

import java.sql.SQLException;

public class ProgrammeDatabaseHelper {

    private final static String DATABASE_NAME = "programmes";

    public static void updateProgrammeWebhook(final boolean isDisabled,
                                              final String programmeId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.programme SET webhook_disabled = %s where id = %s;", DATABASE_NAME, isDisabled ? 1 : 0, programmeId));
    }

    public static void setDefaultSecurityModel(final String programmeId) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.security_model_configuration SET opt_in = 1 WHERE field_type IN ('CARD_NUMBER', 'CVV', 'PIN') AND programme_id = %s;", DATABASE_NAME, programmeId));

        DatabaseHelper
                .update(String.format("UPDATE %s.security_model_configuration SET opt_in = 0 WHERE field_type = 'PASSWORD' AND programme_id = %s;", DATABASE_NAME, programmeId));
    }

    public static void switchOtpVerifyLimitFunction(final String programmeId, final int isEnabled) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.programme SET otp_limit_verify_enabled = %s WHERE id = %s;", DATABASE_NAME, isEnabled, programmeId));
    }

    public static void updateOtpVerifyLimitNumber(final String programmeId, final Integer limitNumber) throws SQLException {
        DatabaseHelper
                .update(String.format("UPDATE %s.programme SET otp_number_of_verify_attempts = %s WHERE id = %s;", DATABASE_NAME, limitNumber, programmeId));
    }
}
