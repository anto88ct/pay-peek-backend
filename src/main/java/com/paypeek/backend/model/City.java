package com.paypeek.backend.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "cities")
public class City extends BaseEntity{

    @Indexed(unique = true)
    @Field("codice")
    private String codice;

    @Field("descrizione")
    private String descrizione;
}
