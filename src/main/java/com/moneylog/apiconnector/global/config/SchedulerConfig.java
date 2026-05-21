package com.moneylog.apiconnector.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
SchedulerConfig
외부 API 시간 Cron에 맞게 Get Request하기 위해
@EnableScheduling 사용 Class

 */

@Configuration
@EnableScheduling
public class SchedulerConfig {
}
