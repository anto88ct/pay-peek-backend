package com.paypeek.backend.batch;

import com.paypeek.backend.service.BatchNotificationService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class PayslipTasklet implements Tasklet {

    private final BatchNotificationService notificationService;

    public PayslipTasklet(BatchNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        notificationService.processAllUsers();
        return RepeatStatus.FINISHED;
    }
}