package com.infrati.backendinfrati.repository;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;

/**
 * Lectura segura de celdas: cada metodo devuelve null si la celda no existe
 * o esta vacia, en vez de lanzar NullPointerException. Los CP negativos del
 * E1 (CP030 a CP036) piden justamente que un campo sin dato no rompa la
 * ficha, sino que se muestre vacio.
 */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    public static String getString(Row fila, int col) {
        Cell celda = fila.getCell(col);
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return null;
        }
        if (celda.getCellType() == CellType.NUMERIC) {
            return String.valueOf(celda.getNumericCellValue());
        }
        String valor = celda.toString().trim();
        return valor.isEmpty() ? null : valor;
    }

    public static Double getDouble(Row fila, int col) {
        Cell celda = fila.getCell(col);
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            return celda.getCellType() == CellType.NUMERIC
                    ? celda.getNumericCellValue()
                    : Double.parseDouble(celda.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer getInteger(Row fila, int col) {
        Double valor = getDouble(fila, col);
        return valor == null ? null : valor.intValue();
    }

    public static LocalDate getLocalDate(Row fila, int col) {
        Cell celda = fila.getCell(col);
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return null;
        }
        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate();
        }
        String texto = getString(fila, col);
        return texto == null ? null : LocalDate.parse(texto);
    }

    public static Boolean getBoolean(Row fila, int col) {
        Cell celda = fila.getCell(col);
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return null;
        }
        if (celda.getCellType() == CellType.BOOLEAN) {
            return celda.getBooleanCellValue();
        }
        String texto = getString(fila, col);
        return texto == null ? null : Boolean.parseBoolean(texto);
    }
}
