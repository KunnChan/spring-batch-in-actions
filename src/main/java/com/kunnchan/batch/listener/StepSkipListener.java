package com.kunnchan.batch.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kunnchan.batch.entity.Customer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StepSkipListener implements SkipListener<Customer, Number> {


    @Override // item reader
    public void onSkipInRead(Throwable throwable) {
        log.info("A failure on read {} ", throwable.getMessage());
    }

    @Override // item writer
    public void onSkipInWrite(Number item, Throwable throwable) {
        log.info("A failure on write {} , {}", throwable.getMessage(), item);
    }

    @SneakyThrows
    @Override // item processor
    public void onSkipInProcess(Customer customer, Throwable throwable) {
        System.out.println("onSkipInProcessonSkipInProcessonSkipInProcessonSkipInProcess");
        log.info("Item {}  was skipped due to the exception  {}", new ObjectMapper().writeValueAsString(customer),
                throwable.getMessage());
    }
}
