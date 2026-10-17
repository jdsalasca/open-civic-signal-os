package org.opencivic.signalos.config;

import org.opencivic.signalos.service.DigestSchedulerService;
import org.opencivic.signalos.service.RateLimitService;
import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps the two {@code @Scheduled} beans eager even when
 * {@code spring.main.lazy-initialization=true} is active (see application-prod.yml).
 *
 * <p>{@code ScheduledAnnotationBeanPostProcessor} registers tasks in
 * {@code postProcessAfterInitialization}, so a lazy {@code @Scheduled} bean that nothing
 * else depends on is never instantiated and its tasks would silently never run.
 * Excluding them by type preserves the weekly digest scheduler and the rate-limit
 * cleanup task in production while every other bean stays lazy.
 */
@Configuration
public class SchedulingLazyInitConfig {

	@Bean
	static LazyInitializationExcludeFilter scheduledBeansEagerForLazyInit() {
		return LazyInitializationExcludeFilter.forBeanTypes(
				DigestSchedulerService.class,
				RateLimitService.class
		);
	}
}
