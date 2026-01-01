package com.paypeek.backend.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job notifyUsersJob;

    public JobScheduler(JobLauncher jobLauncher, Job notifyUsersJob) {
        this.jobLauncher = jobLauncher;
        this.notifyUsersJob = notifyUsersJob;
    }

    @Scheduled(cron = "0 0 9 1 * *")
    public void runMonthlyJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("timestamp", LocalDateTime.now())
                    .toJobParameters();

            jobLauncher.run(notifyUsersJob, params);
            System.out.println("Batch Job avviato con successo.");
        } catch (Exception e) {
            // Logga l'errore ma non bloccare l'applicazione
            System.err.println("Errore durante l'avvio del Batch Job: " + e.getMessage());
        }
    }
}
