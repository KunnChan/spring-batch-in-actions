package com.kunnchan.batch.config;

import com.kunnchan.batch.entity.Customer;
import org.springframework.batch.item.ItemProcessor;

public class CustomerProcessor implements ItemProcessor<Customer, Customer> {
    @Override
    public Customer process(Customer customer) throws Exception {
        int age = Integer.parseInt(customer.getAge());//vhjkdfh38497infdhg
        if (age >= 18) {
            return customer;
        }
        return null;
    }
}
