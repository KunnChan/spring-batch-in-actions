package com.kunnchan.batch.config;

import com.kunnchan.batch.entity.Customer;
import com.kunnchan.batch.listener.StepSkipListener;
import com.kunnchan.batch.partition.ColumnRangePartitioner;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class JobConfiguration {


    private final StepSkipListener stepSkipListener;

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

    @Bean
    public PartitionHandler partitionHandler(Step slaveStep) {
        TaskExecutorPartitionHandler taskExecutorPartitionHandler = new TaskExecutorPartitionHandler();
        taskExecutorPartitionHandler.setGridSize(2);
        taskExecutorPartitionHandler.setTaskExecutor(taskExecutor());
        taskExecutorPartitionHandler.setStep(slaveStep);
        return taskExecutorPartitionHandler;
    }

    @Bean
    protected Step slaveStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                             ItemReader<Customer> reader, ItemProcessor<Customer, Customer> processor,
                             CustomerWriter customerWriter,
                             ExceptionSkipPolicy exceptionSkipPolicy) {
        return new StepBuilder("slaveStep", jobRepository)
                .<Customer, Customer> chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(customerWriter)
                .faultTolerant()
                //.skipLimit(100)
                //.skip(NumberFormatException.class)
                //.noSkip(IllegalArgumentException.class)
                .listener(stepSkipListener)
                .skipPolicy(exceptionSkipPolicy)
                .build();
    }

//    @Bean
//    protected Step masterStep(JobRepository jobRepository, Step slaveStep, PartitionHandler partitionHandler, ColumnRangePartitioner partitioner) {
//        return new StepBuilder("masterStep", jobRepository).partitioner(slaveStep.getName(), partitioner)
//                .partitionHandler(partitionHandler).build();
//    }

    @Bean(name = "firstBatchJob")
    public Job job(JobRepository jobRepository, Step slaveStep) {
        return new JobBuilder("firstBatchJob", jobRepository).flow(slaveStep).end().build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor asyncTaskExecutor = new SimpleAsyncTaskExecutor();
        asyncTaskExecutor.setConcurrencyLimit(10);
        return asyncTaskExecutor;
    }

}
