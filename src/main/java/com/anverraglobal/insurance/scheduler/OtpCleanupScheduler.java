package com.anverraglobal.insurance.scheduler;

import com.anverraglobal.insurance.auth.repository.MobileOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final MobileOtpRepository mobileOtpRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Starting scheduled cleanup of expired OTPs");
        try {
            mobileOtpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            log.info("Finished scheduled cleanup of expired OTPs");
        } catch (Exception e) {
            log.error("Failed to clean up expired OTPs", e);
        }
    }
}
