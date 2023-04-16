package com.kunnchan.batch.config;

import com.kunnchan.batch.entity.Customer;
import com.kunnchan.batch.listener.StepSkipListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;

@Slf4j
@Configuration
@EnableBatchProcessing
public class JobConfiguration {

    @Bean
    public ItemReader<Customer> reader() {
        FlatFileItemReader<Customer> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource("src/main/resources/customers_short.csv"));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    @Bean
    @StepScope
    public ItemReader<Customer> dynamicReader(@Value("#{jobParameters[fullPathFileName]}") String pathToFile) {
        FlatFileItemReader<Customer> itemReader = new FlatFileItemReader<>();
        itemReader.setResource(new FileSystemResource(new File(pathToFile)));
        itemReader.setName("csvReader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());
        return itemReader;
    }

    @Bean
    public ItemProcessor<Customer, Customer> processor() {
        return new CustomerProcessor();
    }

//    @Bean
//    public ItemWriter<Customer> writer() {
//        RepositoryItemWriter<Customer> writer = new RepositoryItemWriter<>();
//        writer.setRepository(customerRepository);
//        writer.setMethodName("save");
//        return writer;
//    }

    private LineMapper<Customer> lineMapper() {
        DefaultLineMapper<Customer> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setStrict(false);
        lineTokenizer.setNames("id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob", "age");

        BeanWrapperFieldSetMapper<Customer> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Customer.class);

        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;

    }

//    @Bean
//    public PartitionHandler partitionHandler(Step slaveStep) {
//        TaskExecutorPartitionHandler taskExecutorPartitionHandler = new TaskExecutorPartitionHandler();
//        taskExecutorPartitionHandler.setGridSize(2);
//        taskExecutorPartitionHandler.setTaskExecutor(taskExecutor());
//        taskExecutorPartitionHandler.setStep(slaveStep);
//        return taskExecutorPartitionHandler;
//    }

    @Bean
    protected Step dynamicFileStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             ItemReader<Customer> dynamicReader, ItemProcessor<Customer, Customer> processor,
                             CustomerWriter customerWriter) {
        return new StepBuilder("dynamicFileStep", jobRepository)
                .<Customer, Customer> chunk(5, transactionManager)
                .reader(dynamicReader)
                .processor(processor)
                .writer(customerWriter)
                .faultTolerant()
                //.skipLimit(100)
                //.skip(NumberFormatException.class)
                //.noSkip(IllegalArgumentException.class)
                .listener(skipListener())
                .skipPolicy(skipPolicy())
                .build();
    }

    @Bean
    protected Step slaveStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             ItemReader<Customer> reader, ItemProcessor<Customer, Customer> processor,
                             CustomerWriter customerWriter) {
        return new StepBuilder("slaveStep", jobRepository)
                .<Customer, Customer> chunk(5, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(customerWriter)
                .faultTolerant()
                //.skipLimit(100)
                //.skip(NumberFormatException.class)
                //.noSkip(IllegalArgumentException.class)
                .listener(skipListener())
                .skipPolicy(skipPolicy())
                .build();
    }

//    @Bean
//    protected Step masterStep(JobRepository jobRepository, Step slaveStep, PartitionHandler partitionHandler, ColumnRangePartitioner partitioner) {
//        return new StepBuilder("masterStep", jobRepository).partitioner(slaveStep.getName(), partitioner)
//                .partitionHandler(partitionHandler).build();
//    }

    @Bean(name = "staticJob")
    @Primary
    public Job staticJob(JobRepository jobRepository, Step slaveStep) {
        return new JobBuilder("staticJob", jobRepository).flow(slaveStep).end().build();
    }

    @Bean(name = "dynamicJob")
    public Job dyanmicJob(JobRepository jobRepository, Step dynamicFileStep) {
        return new JobBuilder("dynamicJob", jobRepository).flow(dynamicFileStep).end().build();
    }


    @Bean
    public SkipPolicy skipPolicy(){
        return new ExceptionSkipPolicy();
    }

    @Bean
    public SkipListener skipListener(){
        return new StepSkipListener();
    }

//    @Bean
//    public TaskExecutor taskExecutor() {
//        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
//        asyncTaskExecutor.setConcurrencyLimit(10);
//        return asyncTaskExecutor;
//    }

}
