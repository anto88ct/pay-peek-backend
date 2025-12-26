package com.paypeek.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "year_folders")
public class YearFolder extends BaseEntity {

    // BaseEntity provides id, createdDate, lastModifiedDate

    private String userId; // Link to user
    private int year;
    private String color;

    @Builder.Default
    private List<MonthFolder> months = new ArrayList<>();
}
