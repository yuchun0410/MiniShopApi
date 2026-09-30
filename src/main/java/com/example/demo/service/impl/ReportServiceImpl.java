package com.example.demo.service.impl;

import com.example.demo.dao.OrderItemDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.demo.model.ProductSalesReport;
import com.example.demo.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final OrderItemDao orderItemDao;

    public ReportServiceImpl(@Qualifier("orderItemDaoMyBatis") OrderItemDao orderItemDao) {
        this.orderItemDao = orderItemDao;
    }

    @Override
    public List<ProductSalesReport> getProductSalesReport() {
        List<ProductSalesReport> report = orderItemDao.findProductSalesReport();
        log.info("產生商品銷售報表，品項數量={}", report.size());
        return report;
    }
}
