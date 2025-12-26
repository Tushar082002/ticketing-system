package org.example.ticketingapplication.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketingapplication.dto.TicketEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkTicketProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.bulk-requests:ticket.bulk.requests}")
    private String bulkRequestsTopic;

    @Value("${spring.kafka.bulk.chunk-size:100}")
    private int chunkSize;

    public String publishBatch(List<TicketEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("Events list cannot be null or empty");
        }

        String batchId = generateBatchId();
        log.info("Batch created: {}, total events: {}", batchId, events.size());

        try {
            List<List<TicketEvent>> chunks = chunkList(events, chunkSize);
            log.info("Batch {} split into {} chunks (chunk size: {})", batchId, chunks.size(), chunkSize);

            for (int i = 0; i < chunks.size(); i++) {
                List<TicketEvent> chunk = chunks.get(i);
                final int chunkNumber = i + 1;
                final int totalChunks = chunks.size();

                try {
                    publishChunk(batchId, chunkNumber, totalChunks, chunk);
                } catch (Exception ex) {
                    log.error("Error publishing chunk {}/{} for batch {}: {}",
                            chunkNumber, totalChunks, batchId, ex.getMessage(), ex);
                }
            }

            log.info("Batch {} submitted to Kafka: {} chunks", batchId, chunks.size());
            return batchId;

        } catch (Exception ex) {
            log.error("Error processing batch {}: {}", batchId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish batch: " + ex.getMessage(), ex);
        }
    }

    private void publishChunk(String batchId, int chunkNumber, int totalChunks, List<TicketEvent> events) {
        log.debug("Publishing chunk {}/{} for batch {}: {} events", chunkNumber, totalChunks, batchId, events.size());

        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(bulkRequestsTopic, batchId, events);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish chunk {}/{} for batch {}: {}",
                            chunkNumber, totalChunks, batchId, ex.getMessage());
                } else {
                    log.info("Chunk {}/{} published for batch {}: partition={}, offset={}",
                            chunkNumber, totalChunks, batchId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

        } catch (Exception ex) {
            log.error("Error sending chunk {}/{} for batch {}: {}",
                    chunkNumber, totalChunks, batchId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to send chunk: " + ex.getMessage(), ex);
        }
    }

    private String generateBatchId() {
        return String.format("BATCH-%d-%s",
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private <T> List<List<T>> chunkList(List<T> list, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, list.size());
            chunks.add(new ArrayList<>(list.subList(i, end)));
        }
        return chunks;
    }
}
