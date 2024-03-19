package com.example.batchprocessing.controller;


import lombok.AllArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/jobs")
@AllArgsConstructor
public class JobController {

    private JobLauncher jobLauncher;

    private Job job;

    @PostMapping("/importCustomers")
    public void importDataFromCsvToDBJob(@RequestParam("file")MultipartFile file) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, IOException {

        String fileName = file.getOriginalFilename();
        assert fileName != null;

        String currentDirectory = System.getProperty("user.dir");

        // Save the file to the current working directory
        String filePath = currentDirectory + File.separator + fileName;
        file.transferTo(new File(filePath));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fullPathFileName",filePath)
                .addLong("startAt", System.currentTimeMillis()).toJobParameters();

        jobLauncher.run(job, jobParameters);
    }
}
