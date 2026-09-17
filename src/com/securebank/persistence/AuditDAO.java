package com.securebank.persistence;
import com.securebank.fraud.FraudAlert;
import com.securebank.model.Transaction;
import com.securebank.util.BankLogger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class AuditDAO {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void saveTransaction(Transaction txn) {
        String sql = "INSERT OR REPLACE INTO transactions " +
                "(transaction_id, source_account, destination_account, amount, status, origin_device, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, txn.getTransactionId());
            ps.setString(2, txn.getSourceAccount());
            ps.setString(3, txn.getDestinationAccount());
            ps.setDouble(4, txn.getAmount());
            ps.setString(5, txn.getStatus().name());
            ps.setString(6, txn.getOriginDevice());
            ps.setString(7, txn.getTimestamp().format(FORMAT));

            ps.executeUpdate();
        } catch (SQLException e) {
            BankLogger.warn("Failed to persist transaction " + txn.getTransactionId() + ": " + e.getMessage());
        }
    }

    public void saveAlert(FraudAlert alert) {
        String sql = "INSERT INTO fraud_alerts (transaction_id, rule_triggered, raised_at) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, alert.getTransactionId());
            ps.setString(2, alert.getRuleTriggered());
            ps.setString(3, alert.getRaisedAt().format(FORMAT));

            ps.executeUpdate();
        } catch (SQLException e) {
            BankLogger.warn("Failed to persist fraud alert for " + alert.getTransactionId() + ": " + e.getMessage());
        }
    }
}
