package com.trabajaya.jobs;

import com.trabajaya.shared.Db;
import static com.trabajaya.shared.Db.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.*;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.pricing.enabled", havingValue = "true", matchIfMissing = true)
public class PricingScheduler {
    private final Db db;
    private final JobService jobs;
    private static final Logger log = LoggerFactory.getLogger(PricingScheduler.class);

    public PricingScheduler(Db db, JobService jobs) {
        this.db = db;
        this.jobs = jobs;
    }

    @Scheduled(fixedDelayString = "${app.pricing.poll-ms:5000}")
    public void tick() {
        db.list("SELECT id FROM jobs j WHERE status='PUBLICADO' AND hidden=FALSE AND ((scheduled_at<CURRENT_TIMESTAMP AND NOT EXISTS(SELECT 1 FROM assignments a WHERE a.job_id=j.id AND a.status<>'CANCELADO')) OR (scheduled_at>CURRENT_TIMESTAMP AND auto_increase=TRUE AND current_price<max_price AND next_increase_at<=CURRENT_TIMESTAMP)) ORDER BY id LIMIT 100").forEach((j)->{
            try {
                jobs.tick(id(j, "id"));
            } catch (Exception e) {
                log.warn("No se pudo procesar el precio del trabajo {}", j.get("id"), e);
            }
        });
    }

    @Scheduled(fixedDelay = 3600000)
    public void cleanExpiredSessions() {
        db.update("DELETE FROM auth_sessions WHERE expires_at<CURRENT_TIMESTAMP", java.util.Map.of());
        db.update("DELETE FROM password_resets WHERE expires_at<CURRENT_TIMESTAMP", java.util.Map.of());
    }
}
