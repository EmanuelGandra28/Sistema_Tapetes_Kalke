package br.com.tapeteskalke.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

// Monta planilhas .xlsx: cabeçalho colorido com a marca, R$ como número com formato de moeda, datas como data,
// filtro e linha congelada no cabeçalho, título/subtítulo opcionais e linha de totais opcional (soma das colunas em R$).
@Service
public class ExcelService {
    public enum Tipo { TEXTO, NUMERO, MOEDA, DATA, DATAHORA }
    public record Coluna(String titulo, Tipo tipo) {}
    public record Aba(String nome, List<Coluna> colunas, List<List<Object>> linhas, String titulo, String subtitulo, boolean totais) {
        public Aba(String nome, List<Coluna> colunas, List<List<Object>> linhas) {
            this(nome, colunas, linhas, null, null, false);
        }
    }

    public static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final byte[] VERDE = {(byte) 0x1C, (byte) 0x5C, (byte) 0x46}; // verde Kalke
    private static final byte[] VERDE_SUAVE = {(byte) 0xE2, (byte) 0xEC, (byte) 0xE7};
    private final Clock clock;

    public ExcelService(Clock clock) {
        this.clock = clock;
    }

    private static XSSFCellStyle estilo(Workbook wb, DataFormat formato, String mascara, boolean negrito, byte[] fundo, byte[] letra, boolean borda) {
        var st = (XSSFCellStyle) wb.createCellStyle();
        if (mascara != null) st.setDataFormat(formato.getFormat(mascara));
        var fonte = (XSSFFont) wb.createFont();
        fonte.setBold(negrito);
        if (letra != null) fonte.setColor(new XSSFColor(letra, null));
        st.setFont(fonte);
        if (fundo != null) {
            st.setFillForegroundColor(new XSSFColor(fundo, null));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (borda) {
            st.setBorderBottom(BorderStyle.THIN);
            st.setBorderTop(BorderStyle.THIN);
            st.setBorderLeft(BorderStyle.THIN);
            st.setBorderRight(BorderStyle.THIN);
            st.setBottomBorderColor(new XSSFColor(new byte[]{(byte) 0xD0, (byte) 0xD8, (byte) 0xD3}, null));
            st.setTopBorderColor(new XSSFColor(new byte[]{(byte) 0xD0, (byte) 0xD8, (byte) 0xD3}, null));
            st.setLeftBorderColor(new XSSFColor(new byte[]{(byte) 0xD0, (byte) 0xD8, (byte) 0xD3}, null));
            st.setRightBorderColor(new XSSFColor(new byte[]{(byte) 0xD0, (byte) 0xD8, (byte) 0xD3}, null));
        }
        return st;
    }

    public byte[] planilha(List<Aba> abas) {
        try (var wb = new SXSSFWorkbook(200); var out = new ByteArrayOutputStream()) {
            var formato = wb.createDataFormat();
            var branco = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
            var cabecalho = estilo(wb, formato, null, true, VERDE, branco, true);
            cabecalho.setAlignment(HorizontalAlignment.CENTER);
            cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);
            var texto = estilo(wb, formato, null, false, null, null, true);
            var moeda = estilo(wb, formato, "\"R$\" #,##0.00", false, null, null, true);
            var numero = estilo(wb, formato, "#,##0.###", false, null, null, true);
            var data = estilo(wb, formato, "dd/mm/yyyy", false, null, null, true);
            var dataHora = estilo(wb, formato, "dd/mm/yyyy hh:mm", false, null, null, true);
            var totalTexto = estilo(wb, formato, null, true, VERDE_SUAVE, null, true);
            var totalMoeda = estilo(wb, formato, "\"R$\" #,##0.00", true, VERDE_SUAVE, null, true);
            var titulo = estilo(wb, formato, null, true, null, VERDE, false);
            ((XSSFFont) wb.getXSSFWorkbook().getFontAt(titulo.getFontIndex())).setFontHeightInPoints((short) 15);
            var subtitulo = estilo(wb, formato, null, false, null, new byte[]{(byte) 0x64, (byte) 0x7B, (byte) 0x72}, false);

            for (var aba : abas) {
                var sheet = wb.createSheet(aba.nome());
                sheet.trackAllColumnsForAutoSizing();
                int colunas = aba.colunas().size();
                int r = 0;
                // Título e subtítulo mesclados na largura da tabela (não influenciam a largura automática das colunas).
                if (aba.titulo() != null) {
                    var t = sheet.createRow(r).createCell(0);
                    t.setCellValue(aba.titulo());
                    t.setCellStyle(titulo);
                    if (colunas > 1) sheet.addMergedRegion(new CellRangeAddress(r, r, 0, colunas - 1));
                    r++;
                    if (aba.subtitulo() != null) {
                        var s = sheet.createRow(r).createCell(0);
                        s.setCellValue(aba.subtitulo());
                        s.setCellStyle(subtitulo);
                        if (colunas > 1) sheet.addMergedRegion(new CellRangeAddress(r, r, 0, colunas - 1));
                        r++;
                    }
                    r++; // linha em branco
                }
                int linhaCabecalho = r;
                var linha0 = sheet.createRow(r++);
                linha0.setHeightInPoints(22);
                for (int c = 0; c < colunas; c++) {
                    var cell = linha0.createCell(c);
                    cell.setCellValue(aba.colunas().get(c).titulo());
                    cell.setCellStyle(cabecalho);
                }
                var somas = new double[colunas];
                for (var valores : aba.linhas()) {
                    var row = sheet.createRow(r++);
                    for (int c = 0; c < colunas && c < valores.size(); c++) {
                        var v = valores.get(c);
                        var cell = row.createCell(c);
                        cell.setCellStyle(texto);
                        if (v == null) continue;
                        var tipo = aba.colunas().get(c).tipo();
                        // Valor que não é número/data na coluna (ex.: uma linha de título) vai como texto, sem derrubar a exportação.
                        if (!(v instanceof Number) && (tipo == Tipo.MOEDA || tipo == Tipo.NUMERO) && !ehNumero(v.toString())) tipo = Tipo.TEXTO;
                        if (!(v instanceof java.time.temporal.Temporal) && (tipo == Tipo.DATA || tipo == Tipo.DATAHORA)) tipo = Tipo.TEXTO;
                        switch (tipo) {
                            case MOEDA -> { var n = numero(v); somas[c] += n; cell.setCellValue(n); cell.setCellStyle(moeda); }
                            case NUMERO -> { cell.setCellValue(numero(v)); cell.setCellStyle(numero); }
                            case DATA -> { cell.setCellValue(v instanceof LocalDate d ? d : LocalDate.parse(v.toString())); cell.setCellStyle(data); }
                            case DATAHORA -> { cell.setCellValue(v instanceof OffsetDateTime o ? o.atZoneSameInstant(clock.getZone()).toLocalDateTime() : LocalDateTime.parse(v.toString())); cell.setCellStyle(dataHora); }
                            default -> cell.setCellValue(v.toString());
                        }
                    }
                }
                if (aba.totais() && !aba.linhas().isEmpty()) {
                    var row = sheet.createRow(r++);
                    for (int c = 0; c < colunas; c++) {
                        var cell = row.createCell(c);
                        if (aba.colunas().get(c).tipo() == Tipo.MOEDA) { cell.setCellValue(somas[c]); cell.setCellStyle(totalMoeda); }
                        else { cell.setCellStyle(totalTexto); if (c == 0) cell.setCellValue("Total"); }
                    }
                }
                sheet.createFreezePane(0, linhaCabecalho + 1);
                if (colunas > 0 && !aba.linhas().isEmpty()) sheet.setAutoFilter(new CellRangeAddress(linhaCabecalho, linhaCabecalho + aba.linhas().size(), 0, colunas - 1));
                for (int c = 0; c < colunas; c++) sheet.autoSizeColumn(c);
                // Impressão: paisagem, uma página de largura.
                sheet.setFitToPage(true);
                sheet.getPrintSetup().setLandscape(true);
                sheet.getPrintSetup().setFitWidth((short) 1);
                sheet.getPrintSetup().setFitHeight((short) 0);
            }
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean ehNumero(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
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
