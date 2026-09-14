package com.example.demo.controller;

import com.example.demo.exception.AccessDeniedException;
import com.example.demo.model.Member;
import com.example.demo.model.ProductSalesReport;
import com.example.demo.model.Role;
import com.example.demo.service.MemberService;
import com.example.demo.service.ReportService;
import com.example.demo.service.impl.JasperReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final MemberService memberService;
    private final JasperReportService jasperReportService;

    public ReportController(ReportService reportService, MemberService memberService, JasperReportService jasperReportService) {
        this.reportService = reportService;
        this.memberService = memberService;
        this.jasperReportService = jasperReportService;
    }

    @GetMapping("/products")
    public List<ProductSalesReport> getProductSalesReport(HttpServletRequest request) {
        requireAdmin(request);
        return reportService.getProductSalesReport();
    }

    @GetMapping("/products/pdf")
    public ResponseEntity<byte[]> getProductSalesReportPdf(HttpServletRequest request) throws Exception {
        requireAdmin(request);
        List<ProductSalesReport> data = reportService.getProductSalesReport();
        byte[] pdf = jasperReportService.exportProductSalesReportPdf(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=product_sales_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void requireAdmin(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        Member current = memberService.findById(memberId);
        if (current.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("需要管理員權限");
        }
    }
}
