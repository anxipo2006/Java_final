package com.coffeeshop.core.util;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.coffeeshop.core.model.dto.DailyRevenueDTO;
import com.coffeeshop.core.model.dto.TopSellingItemDTO;

public class ChartGenerator {

    public static ChartPanel createDailyRevenueChartPanel(List<DailyRevenueDTO> data, String title) {
        TimeSeries series = new TimeSeries("Doanh thu hàng ngày");
        for (DailyRevenueDTO dto : data) {
            if (dto.getOrderDate() != null) { // Kiểm tra null cho orderDate
                series.addOrUpdate(new Day(dto.getOrderDate()), dto.getTotalRevenue());
            }
        }
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title, "Ngày", "Doanh Thu (VND)", dataset,
                true, true, false);

        // Tùy chỉnh chart (ví dụ: màu sắc, font) nếu muốn
        // XYPlot plot = (XYPlot) chart.getPlot();
        // XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        // ...
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(600, 400)); // Kích thước gợi ý
        return chartPanel;
    }

    public static ChartPanel createTopSellingItemsBarChartPanel(List<TopSellingItemDTO> data, String title) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (TopSellingItemDTO dto : data) {
            dataset.addValue(dto.getTotalQuantitySold(), "Số lượng bán", dto.getItemName());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                title, "Sản Phẩm", "Số Lượng Bán", dataset,
                PlotOrientation.VERTICAL, true, true, false);

        // Tùy chỉnh
        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(Color.lightGray);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.decode("#007bff")); // Màu xanh dương
        renderer.setDrawBarOutline(false);
        // renderer.setItemMargin(0.01); // Khoảng cách giữa các cột

        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(600, 400));
        return chartPanel;
    }

    public static ChartPanel createTopSellingItemsPieChartPanel(List<TopSellingItemDTO> data, String title) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (TopSellingItemDTO dto : data) {
            dataset.setValue(dto.getItemName() + " (" + dto.getTotalQuantitySold() + ")", dto.getTotalQuantitySold());
        }

        JFreeChart pieChart = ChartFactory.createPieChart(
                title, dataset, true, true, false);

        PiePlot plot = (PiePlot) pieChart.getPlot();
        plot.setSectionPaint("Tên Sản Phẩm A", Color.RED); // Ví dụ đặt màu cho từng phần
        plot.setCircular(true);
        // plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {1} ({2})", NumberFormat.getNumberInstance(), NumberFormat.getPercentInstance()));

        ChartPanel chartPanel = new ChartPanel(pieChart);
        chartPanel.setPreferredSize(new Dimension(500, 400));
        return chartPanel;
    }
}
