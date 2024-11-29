package com.expenses.model;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class Expense {
    private Long id;
    private String description;
    private BigDecimal amount;
    private LocalDate date;
    private Long categoryId;
    private Long userId;
    private LocalDateTime createdAt;
}
