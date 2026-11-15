package br.com.tapeteskalke.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

// Monta planilhas .xlsx: cabeçalho em negrito, R$ como número com formato de moeda e datas como data.
@Service
public class ExcelService {
    public enum Tipo { TEXTO, NUMERO, MOEDA, DATA, DATAHORA }
    public record Coluna(String titulo, Tipo tipo) {}
    public record Aba(String nome, List<Coluna> colunas, List<List<Object>> linhas) {}

    public static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final Clock clock;

    public ExcelService(Clock clock) {
        this.clock = clock;
    }

    public byte[] planilha(List<Aba> abas) {
        try (var wb = new SXSSFWorkbook(200); var out = new ByteArrayOutputStream()) {
            var fonte = wb.createFont();
            fonte.setBold(true);
            var cabecalho = wb.createCellStyle();
            cabecalho.setFont(fonte);
            var formato = wb.createDataFormat();
            var moeda = wb.createCellStyle();
            moeda.setDataFormat(formato.getFormat("\"R$\" #,##0.00"));
            var numero = wb.createCellStyle();
            numero.setDataFormat(formato.getFormat("#,##0.###"));
            var data = wb.createCellStyle();
            data.setDataFormat(formato.getFormat("dd/mm/yyyy"));
            var dataHora = wb.createCellStyle();
            dataHora.setDataFormat(formato.getFormat("dd/mm/yyyy hh:mm"));
            for (var aba : abas) {
                var sheet = wb.createSheet(aba.nome());
                sheet.trackAllColumnsForAutoSizing();
                var linha0 = sheet.createRow(0);
                for (int c = 0; c < aba.colunas().size(); c++) {
                    var cell = linha0.createCell(c);
                    cell.setCellValue(aba.colunas().get(c).titulo());
                    cell.setCellStyle(cabecalho);
                }
                int r = 1;
                for (var valores : aba.linhas()) {
                    var row = sheet.createRow(r++);
                    for (int c = 0; c < aba.colunas().size() && c < valores.size(); c++) {
                        var v = valores.get(c);
                        if (v == null) continue;
                        var cell = row.createCell(c);
                        switch (aba.colunas().get(c).tipo()) {
                            case MOEDA -> { cell.setCellValue(numero(v)); cell.setCellStyle(moeda); }
                            case NUMERO -> { cell.setCellValue(numero(v)); cell.setCellStyle(numero); }
                            case DATA -> { cell.setCellValue(v instanceof LocalDate d ? d : LocalDate.parse(v.toString())); cell.setCellStyle(data); }
                            case DATAHORA -> { cell.setCellValue(v instanceof OffsetDateTime o ? o.atZoneSameInstant(clock.getZone()).toLocalDateTime() : LocalDateTime.parse(v.toString())); cell.setCellStyle(dataHora); }
                            default -> cell.setCellValue(v.toString());
                        }
                    }
                }
                sheet.createFreezePane(0, 1);
                for (int c = 0; c < aba.colunas().size(); c++) sheet.autoSizeColumn(c);
            }
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static double numero(Object v) {
        return v instanceof Number n ? (v instanceof BigDecimal b ? b.doubleValue() : n.doubleValue()) : Double.parseDouble(v.toString());
    }

    // Resposta de download: nome "<nome>-<aaaa-mm-dd>.xlsx" com a data da loja.
    public ResponseEntity<byte[]> resposta(String nome, byte[] conteudo) {
        var arquivo = nome + "-" + LocalDate.now(clock) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(arquivo).build().toString())
                .body(conteudo);
    }

    public ResponseEntity<byte[]> exportar(String nome, Aba... abas) {
        return resposta(nome, planilha(List.of(abas)));
    }
}
