package com.expenses.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class Category {
    private Long id;
    private String name;
    private String description;
    private Long userId;
}
