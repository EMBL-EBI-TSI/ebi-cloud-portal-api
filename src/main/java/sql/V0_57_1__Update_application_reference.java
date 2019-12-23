package sql;

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class V0_57_1__Update_application_reference implements SpringJdbcMigration {

    private static final Logger logger = LoggerFactory.getLogger(V0_57_1__Update_application_reference.class);
    @Override
    public void migrate(JdbcTemplate jdbcTemplate) {
        String select = "select * from application";
        List<Map<String, Object>> applications = jdbcTemplate.queryForList(select);
        logger.debug("Found {} applications", applications.size());
        for(Map<String, Object> application : applications) {
            long applicationId = (Long) application.get("id");
            logger.debug("Update application; id - {}", applicationId);
            // Update application reference with random UUID
            String reference = "app-"+UUID.randomUUID();
            String update = "update application set reference = ? where id = ?";
            jdbcTemplate.update(update, reference, applicationId);
        }
        logger.debug("Done");
    }
}
