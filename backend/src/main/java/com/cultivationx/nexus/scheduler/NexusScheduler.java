package com.cultivationx.nexus.scheduler;

import com.cultivationx.nexus.service.GitHubService;
import com.cultivationx.nexus.service.LeetCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NexusScheduler {

    private final GitHubService gitHubService;
    private final LeetCodeService leetCodeService;

    @Value("${app.scheduling.sync-cron:0 0 */6 * * *}")
    private String syncCron;

    // Every 6 hours
    @Scheduled(cron = "${app.scheduling.sync-cron:0 0 */6 * * *}")
    public void syncAllPlatforms() {
        log.info("Starting scheduled platform sync...");

        try {
            gitHubService.syncAllConnectedUsers();
            log.info("GitHub sync complete");
        } catch (Exception e) {
            log.error("GitHub bulk sync failed: {}", e.getMessage());
        }

        try {
            leetCodeService.syncAllConnectedUsers();
            log.info("LeetCode sync complete");
        } catch (Exception e) {
            log.error("LeetCode bulk sync failed: {}", e.getMessage());
        }

        log.info("Scheduled sync complete.");
    }
}