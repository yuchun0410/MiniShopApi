package com.example.demo.service;

import com.example.demo.model.ProductSalesReport;
import java.util.List;

public interface ReportService {
    List<ProductSalesReport> getProductSalesReport();
}