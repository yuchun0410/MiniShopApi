package com.example.demo.service.impl;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.pdf.JRPdfExporter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JasperReportService {

    public byte[] exportProductSalesReportPdf(List<?> data) throws JRException, java.io.IOException {
        // 1. 從 classpath 讀取 .jrxml 樣板
        InputStream jrxmlStream = new ClassPathResource("reports/product_sales_report.jrxml").getInputStream();

        // 2. 把 .jrxml 編譯成 JasperReport 物件(這一步跟 Java 的「編譯」概念一樣，把原始檔變成可執行的格式)
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

        // 3. 把 Java 物件清單包成 JasperReports 看得懂的資料來源
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

        // 4. 參數（這份報表目前不需要額外參數，先給空的）
        Map<String, Object> parameters = new HashMap<>();

        // 5. 把樣板、參數、資料「填」在一起，產生 JasperPrint（代表「已經算好、準備好要輸出的報表」）
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        // 6. 把 JasperPrint 輸出成 PDF 的 byte[]
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        exporter.exportReport();

        return outputStream.toByteArray();
    }
}