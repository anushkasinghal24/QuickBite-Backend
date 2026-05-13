package com.quickbite.order.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderSchemaMigrator {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void normalizeOrderStatusColumn() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String product = metaData.getDatabaseProductName().toLowerCase(Locale.ROOT);

            if (!product.contains("mysql") && !product.contains("mariadb")) {
                log.debug("Skipping order_status schema normalization for database: {}", product);
                return;
            }

            String currentType = null;
            Integer currentSize = null;

            try (ResultSet columns = metaData.getColumns(
                    connection.getCatalog(), null, "orders", "order_status")) {
                if (columns.next()) {
                    currentType = columns.getString("TYPE_NAME");
                    currentSize = columns.getInt("COLUMN_SIZE");
                }
            }

            if ("VARCHAR".equalsIgnoreCase(currentType) && currentSize != null && currentSize >= 30) {
                log.debug("order_status column already normalized: type={}, size={}", currentType, currentSize);
                return;
            }

            log.info("Normalizing orders.order_status column from type={}, size={} to VARCHAR(30)",
                    currentType, currentSize);
            jdbcTemplate.execute("ALTER TABLE orders MODIFY COLUMN order_status VARCHAR(30) NOT NULL");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to normalize orders.order_status column", ex);
        }
    }
}
