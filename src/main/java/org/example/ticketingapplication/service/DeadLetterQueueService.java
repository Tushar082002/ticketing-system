//package org.example.ticketingapplication.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * Service for handling Dead Letter Queue (DLQ) messages.
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class DeadLetterQueueService {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private static final String BULK_REQUESTS_TOPIC = "ticket.bulk.requests";
//
//    /**
//     * Manual reprocessing of DLQ messages.
//     *
//     * @param records the list of failed records
//     */
//    @KafkaListener(topics = "ticket.bulk.dlq", groupId = "ticket-dlq-consumers")
//    public void reprocessDeadLetterQueue(List<ConsumerRecord<String, Object>> records) {
//        log.info("Reprocessing {} messages from DLQ.", records.size());
//        records.forEach(record -> {
//            try {
//                kafkaTemplate.send(BULK_REQUESTS_TOPIC, record.value());
//                log.info("Reprocessed message: {}", record.value());
//            } catch (Exception e) {
//                log.error("Failed to reprocess message: {}", record.value(), e);
//            }
//        });
//    }
//}
