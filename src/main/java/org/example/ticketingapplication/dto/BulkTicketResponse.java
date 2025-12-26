package org.example.ticketingapplication.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTicketResponse {
    @Builder.Default
    private List<TicketResponse> successfulTickets = new ArrayList<>();


    @Builder.Default
    private List<FailedTicket> failedTickets = new ArrayList<>();

    private int successCount;
    private int failureCount;
    private int totalProcessed;

    /**
     * Represents a failed ticket creation with error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedTicket {
        private String ticketNumber;
        private String errorMessage;
        private String errorCode;
    }

    /**
     * Add a successful ticket to the response.
     */
    public void addSuccess(TicketResponse ticket) {
        if (successfulTickets == null) {
            successfulTickets = new ArrayList<>();
        }
        successfulTickets.add(ticket);
        successCount++;
        totalProcessed++;
    }

    /**
     * Add a failed ticket to the response.
     */
    public void addFailure(String ticketNumber, String errorMessage, String errorCode) {
        if (failedTickets == null) {
            failedTickets = new ArrayList<>();
        }
        failedTickets.add(FailedTicket.builder()
                .ticketNumber(ticketNumber)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build());
        failureCount++;
        totalProcessed++;
    }
}
