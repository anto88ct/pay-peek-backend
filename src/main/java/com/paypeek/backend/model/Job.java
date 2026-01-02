package com.paypeek.backend.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "job")
public class Job extends  BaseEntity {

    @Indexed(unique = true)
    @Field("descrizione")
    private String descrizione;
}
