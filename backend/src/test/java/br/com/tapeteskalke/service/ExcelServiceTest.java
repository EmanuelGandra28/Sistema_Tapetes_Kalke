package br.com.tapeteskalke.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.tapeteskalke.service.ExcelService.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelServiceTest {
    private final ExcelService service = new ExcelService(Clock.fixed(Instant.parse("2026-11-14T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    @Test
    void geraCabecalhoEmNegritoMoedaComoNumeroEData() throws Exception {
        var aba = new Aba("Vendas", List.of(new Coluna("Cliente", Tipo.TEXTO), new Coluna("Total", Tipo.MOEDA), new Coluna("Data", Tipo.DATA)),
                List.of(List.of("Ana", new BigDecimal("199.90"), LocalDate.of(2026, 11, 14)), java.util.Arrays.asList("Bia", null, null)));

        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(service.planilha(List.of(aba))))) {
            var sheet = wb.getSheet("Vendas");
            var cabecalho = sheet.getRow(0).getCell(0);
            assertEquals("Cliente", cabecalho.getStringCellValue());
            assertTrue(wb.getFontAt(cabecalho.getCellStyle().getFontIndex()).getBold());
            var total = sheet.getRow(1).getCell(1);
            assertEquals(CellType.NUMERIC, total.getCellType());
            assertEquals(199.90, total.getNumericCellValue(), 0.001);
            assertTrue(total.getCellStyle().getDataFormatString().contains("R$"));
            assertEquals(LocalDate.of(2026, 11, 14), sheet.getRow(1).getCell(2).getLocalDateTimeCellValue().toLocalDate());
            var vazia = sheet.getRow(2).getCell(1);
            assertTrue(vazia == null || vazia.getCellType() == CellType.BLANK);
        }
    }

    @Test
    void respostaTemTipoENomeComData() {
        var r = service.resposta("vendas", new byte[]{1});
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", r.getHeaders().getContentType().toString());
        assertTrue(r.getHeaders().getContentDisposition().toString().contains("vendas-2026-11-14.xlsx"));
    }
}
