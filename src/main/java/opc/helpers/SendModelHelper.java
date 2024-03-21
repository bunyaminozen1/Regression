package opc.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.sends.BulkSendFundsModel;
import opc.models.multi.sends.SendFundsModel;

public class SendModelHelper {
  public static BulkSendFundsModel createSendBulkPayments(final int numberOfSends,
                                                          final String profileId,
                                                          final String sourceManagedAccountId,
                                                          final String destinationManagedAccountId,
                                                          final String currency,
                                                          final Long amount) {

    List<SendFundsModel> sendFundsModels = new ArrayList<>();
    IntStream.range(0, numberOfSends).forEach(owt ->
        sendFundsModels.add(SendFundsModel
            .DefaultSendsModel(profileId, ManagedInstrumentType.MANAGED_ACCOUNTS, sourceManagedAccountId,
                ManagedInstrumentType.MANAGED_ACCOUNTS, destinationManagedAccountId, currency, amount).build()));

    return BulkSendFundsModel.builder().sends(sendFundsModels).build();
  }

  public static BulkSendFundsModel createSendBulkPayments(final int numberOfSends,
                                                          final String profileId,
                                                          final ManagedInstrumentType sourceInstrumentType,
                                                          final String sourceInstrumentId,
                                                          final ManagedInstrumentType destinationInstrumentType,
                                                          final String destinationInstrumentId,
                                                          final String currency,
                                                          final Long amount) {

    List<SendFundsModel> sendFundsModels = new ArrayList<>();
    IntStream.range(0, numberOfSends).forEach(owt ->
        sendFundsModels.add(SendFundsModel
            .DefaultSendsModel(profileId, sourceInstrumentType, sourceInstrumentId,
                destinationInstrumentType, destinationInstrumentId, currency, amount).build()));

    return BulkSendFundsModel.builder().sends(sendFundsModels).build();
  }

  public static BulkSendFundsModel createSendBulkPayments(final int numberOfSends,
                                                          final SendFundsModel sendFundsModel) {

    List<SendFundsModel> bulkSendFundsModels = new ArrayList<>();
    IntStream.range(0, numberOfSends).forEach(owt -> bulkSendFundsModels.add(sendFundsModel));

    return BulkSendFundsModel.builder().sends(bulkSendFundsModels).build();
  }

  public static SendFundsModel createSendScheduledPayment(final String scheduledTimestamp,
                                                          final String profileId,
                                                          final ManagedInstrumentType sourceInstrumentType,
                                                          final String sourceInstrumentId,
                                                          final ManagedInstrumentType destinationInstrumentType,
                                                          final String destinationInstrumentId,
                                                          final String currency,
                                                          final Long amount) {
    return SendFundsModel
        .DefaultSendsModel(profileId, sourceInstrumentType, sourceInstrumentId,
            destinationInstrumentType, destinationInstrumentId, currency, amount)
        .setScheduledTimestamp(scheduledTimestamp).build();
  }

  public static BulkSendFundsModel createSendBulkScheduledPaymentsPayments(final int numberOfSends,
                                                                           final String scheduledTimestamp,
                                                                           final String profileId,
                                                                           final ManagedInstrumentType sourceInstrumentType,
                                                                           final String sourceInstrumentId,
                                                                           final ManagedInstrumentType destinationInstrumentType,
                                                                           final String destinationInstrumentId,
                                                                           final String currency,
                                                                           final Long amount) {

    List<SendFundsModel> sendFundsModels = new ArrayList<>();
    IntStream.range(0, numberOfSends).forEach(owt ->
        sendFundsModels.add(SendFundsModel
            .DefaultSendsModel(profileId, sourceInstrumentType, sourceInstrumentId,
                destinationInstrumentType, destinationInstrumentId, currency, amount)
            .setScheduledTimestamp(scheduledTimestamp).build()));

    return BulkSendFundsModel.builder().sends(sendFundsModels).build();
  }
}
