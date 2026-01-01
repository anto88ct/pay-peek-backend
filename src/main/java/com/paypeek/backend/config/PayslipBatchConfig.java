package com.paypeek.backend.config;

import com.paypeek.backend.batch.PayslipTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PayslipBatchConfig {

    @Bean
    public Job notifyUsersJob(JobRepository jobRepository, Step notificationStep) {
        return new JobBuilder("notifyUsersJob", jobRepository)
                .start(notificationStep)
                .build();
    }

    @Bean
    public Step notificationStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 PayslipTasklet payslipTasklet) {
        return new StepBuilder("notificationStep", jobRepository)
                .tasklet(payslipTasklet, transactionManager)
                .build();
    }
}