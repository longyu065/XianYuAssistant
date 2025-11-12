package com.feijimiao.xianyuassistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库初始化监听器
 */
@Slf4j
@Component
public class DatabaseInitListener implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private DataSource dataSource;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("=".repeat(60));
        log.info("数据库初始化完成，开始验证...");
        log.info("=".repeat(60));
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // 查询表信息
            ResultSet tables = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            );
            
            log.info("📊 数据库表列表:");
            while (tables.next()) {
                String tableName = tables.getString("name");
                
                // 查询表的记录数
                ResultSet count = stmt.executeQuery("SELECT COUNT(*) as cnt FROM " + tableName);
                int recordCount = 0;
                if (count.next()) {
                    recordCount = count.getInt("cnt");
                }
                count.close();
                
                log.info("  ✓ {} (记录数: {})", tableName, recordCount);
            }
            tables.close();
            
            // 查询索引信息
            ResultSet indexes = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            );
            
            log.info("📑 数据库索引列表:");
            while (indexes.next()) {
                String indexName = indexes.getString("name");
                log.info("  ✓ {}", indexName);
            }
            indexes.close();
            
            // 查询触发器信息
            ResultSet triggers = stmt.executeQuery(
                "SELECT name FROM sqlite_master WHERE type='trigger' ORDER BY name"
            );
            
            log.info("⚡ 数据库触发器列表:");
            while (triggers.next()) {
                String triggerName = triggers.getString("name");
                log.info("  ✓ {}", triggerName);
            }
            triggers.close();
            
            log.info("=".repeat(60));
            log.info("✅ 数据库验证完成，系统就绪！");
            log.info("=".repeat(60));
            
        } catch (Exception e) {
            log.error("验证数据库失败", e);
        }
    }
}
