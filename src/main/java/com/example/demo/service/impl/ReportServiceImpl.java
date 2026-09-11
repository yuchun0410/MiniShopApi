package com.example.demo.service.impl;

import com.example.demo.dao.OrderItemDao;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.demo.model.ProductSalesReport;
import com.example.demo.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final OrderItemDao orderItemDao;

    public ReportServiceImpl(@Qualifier("orderItemDaoMyBatis") OrderItemDao orderItemDao) {
        this.orderItemDao = orderItemDao;
    }

    @Override
    public List<ProductSalesReport> getProductSalesReport() {
        return orderItemDao.findProductSalesReport();
    }
}