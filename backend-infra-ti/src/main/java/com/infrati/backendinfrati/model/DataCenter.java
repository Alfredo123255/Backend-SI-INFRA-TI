package com.infrati.backendinfrati.model;

import com.infrati.backendinfrati.model.Enum.AmbienteEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCenter {
    private String nombre;
    private String laitud;
    private String longitud;
}
