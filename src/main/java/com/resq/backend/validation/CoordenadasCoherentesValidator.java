package com.resq.backend.validation;

import com.resq.backend.entity.Reporte;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CoordenadasCoherentesValidator implements ConstraintValidator<CoordenadasCoherentes, Reporte> {

    @Override
    public boolean isValid(Reporte reporte, ConstraintValidatorContext contexto) {
        return reporte == null || sonCoherentes(reporte.getLatitud(), reporte.getLongitud());
    }

    static boolean sonCoherentes(Double latitud, Double longitud) {
        if (latitud == null && longitud == null) {
            return true;
        }

        if (latitud == null || longitud == null) {
            return false;
        }

        // NaN e infinito se escapan de @DecimalMin/@DecimalMax porque toda
        // comparacion contra ellos da false, asi que se rechazan aqui.
        return Double.isFinite(latitud) && Double.isFinite(longitud);
    }
}
