package com.example.batchprocessing.config;

import com.example.batchprocessing.model.Customer;
import com.example.batchprocessing.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

@Configuration
@AllArgsConstructor
public class BatchConfiguration {

    private JobRepository jobRepository;

    private PlatformTransactionManager transactionManager;

    private CustomerRepository customerRepository;


    // Step
    @Bean
    public Step step1(FlatFileItemReader<Customer> reader) {
        return new StepBuilder("csv-step", jobRepository)
                .<Customer, Customer>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor())
                .writer(writer())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Job runJob(FlatFileItemReader<Customer> reader) {
        return new JobBuilder("importCustomers", jobRepository)
                .start(step1(reader))
                .build();
    }


    // Item Reader
    @Bean
    @StepScope // Instantiates a new Instance of this component (useful during late param binding)
    public FlatFileItemReader<Customer> reader(@Value("#{jobParameters[fullPathFileName]}") String pathOfFile) {
        FlatFileItemReader<Customer> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource(new File(pathOfFile)));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    private LineMapper<Customer> lineMapper() {
        DefaultLineMapper<Customer> mapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob", "age");

        BeanWrapperFieldSetMapper<Customer> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Customer.class);


        mapper.setLineTokenizer(lineTokenizer);
        mapper.setFieldSetMapper(fieldSetMapper);

        return mapper;
    }


    //Item Processor
    @Bean
    public CustomerProcessor processor(){
        return new CustomerProcessor();
    }


    //Item Writer
    @Bean
    public RepositoryItemWriter<Customer> writer(){
        RepositoryItemWriter<Customer> itemWriter = new RepositoryItemWriter<>();
        itemWriter.setRepository(customerRepository);
        itemWriter.setMethodName("save");
        return itemWriter;
    }


    //Task to insert Asynchronously (Main Concept is to increase insertion speed to DB)
    @Bean
    public TaskExecutor taskExecutor(){
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(10);
        return asyncTaskExecutor;
    }



}




