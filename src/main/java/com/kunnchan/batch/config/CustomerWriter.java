package com.kunnchan.batch.config;

import com.kunnchan.batch.entity.Customer;
import com.kunnchan.batch.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomerWriter implements ItemWriter<Customer> {

    private final CustomerRepository customerRepository;
    @Override
    public void write(Chunk<? extends Customer> chunk) {
        System.out.println("Thread Name : -"+Thread.currentThread().getName());
        customerRepository.saveAll(chunk);
    }
}
