package io.cklau1001.workflow1.dto;

import java.time.LocalDateTime;

public record BookTicketRequest(LocalDateTime filmtime,
                                String cinema,
                                LocalDateTime mealtime,
                                String restaurant) {}
