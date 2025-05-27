package com.coffeeshop.ui;

import com.coffeeshop.core.dao.OrderDAO;
import com.coffeeshop.core.model.dto.DailyRevenueDTO;
import com.coffeeshop.core.model.dto.TopSellingItemDTO;
import com.coffeeshop.core.util.ChartGenerator;
import com.coffeeshop.core.exception.DatabaseOperationException;

import org.jfree.chart.ChartPanel;
import com.toedter.calendar.JDateChooser; // Sử dụng JDateChooser

import javax.swing.*;
import java.awt.*;
import java.sql.Date;
import java.util.Calendar;
import java.util.List;

public class SimpleStatisticsUI extends JPanel {

    private OrderDAO orderDAO;

    private JPanel chartDisplayPanel; // Panel để hiển thị các biểu đồ (có thể dùng CardLayout)
    private JDateChooser startDateChooser;
    private JDateChooser endDateChooser;
    private JButton btnLoadCharts;

    public SimpleStatisticsUI() {
        orderDAO = new OrderDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Thống Kê và Báo Cáo"));

        // --- Panel Điều khiển (chọn ngày, ...) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Từ ngày:"));
        startDateChooser = new JDateChooser();
        startDateChooser.setDateFormatString("yyyy-MM-dd");
        startDateChooser.setPreferredSize(new Dimension(120, startDateChooser.getPreferredSize().height));
        controlPanel.add(startDateChooser);

        controlPanel.add(new JLabel("Đến ngày:"));
        endDateChooser = new JDateChooser();
        endDateChooser.setDateFormatString("yyyy-MM-dd");
        endDateChooser.setPreferredSize(new Dimension(120, endDateChooser.getPreferredSize().height));
        controlPanel.add(endDateChooser);

        // Đặt ngày mặc định (ví dụ: 7 ngày trước đến hôm nay)
        Calendar cal = Calendar.getInstance();
        endDateChooser.setDate(cal.getTime()); // Hôm nay
        cal.add(Calendar.DAY_OF_MONTH, -7); // 7 ngày trước
        startDateChooser.setDate(cal.getTime());

        btnLoadCharts = new JButton("Xem Thống Kê");
        controlPanel.add(btnLoadCharts);
        add(controlPanel, BorderLayout.NORTH);

        // --- Panel Hiển thị Biểu đồ ---
        chartDisplayPanel = new JPanel();
        // Sử dụng BoxLayout để xếp chồng các biểu đồ theo chiều dọc
        chartDisplayPanel.setLayout(new BoxLayout(chartDisplayPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(chartDisplayPanel); // Cho phép cuộn nếu nhiều biểu đồ
        add(scrollPane, BorderLayout.CENTER);

        // --- Listeners ---
        btnLoadCharts.addActionListener(e -> loadAndDisplayCharts());

        // Tải biểu đồ mặc định khi khởi tạo
        loadAndDisplayCharts();
    }

    private void loadAndDisplayCharts() {
        java.util.Date utilStartDate = startDateChooser.getDate();
        java.util.Date utilEndDate = endDateChooser.getDate();

        if (utilStartDate == null || utilEndDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày bắt đầu và kết thúc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (utilStartDate.after(utilEndDate)) {
            JOptionPane.showMessageDialog(this, "Ngày bắt đầu không thể sau ngày kết thúc.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date sqlStartDate = new Date(utilStartDate.getTime());
        Date sqlEndDate = new Date(utilEndDate.getTime());

        chartDisplayPanel.removeAll(); // Xóa các biểu đồ cũ

        try {
            // 1. Biểu đồ Doanh thu theo ngày
            List<DailyRevenueDTO> revenueData = orderDAO.getDailyRevenue(sqlStartDate, sqlEndDate);
            if (!revenueData.isEmpty()) {
                ChartPanel revenueChartPanel = ChartGenerator.createDailyRevenueChartPanel(revenueData, "Doanh Thu Theo Ngày");
                chartDisplayPanel.add(revenueChartPanel);
            } else {
                chartDisplayPanel.add(new JLabel("Không có dữ liệu doanh thu cho khoảng thời gian này."));
            }

            // 2. Biểu đồ Top Sản phẩm bán chạy (Bar chart)
            List<TopSellingItemDTO> topItemsData = orderDAO.getTopSellingItems(5, sqlStartDate, sqlEndDate); // Top 5
            if (!topItemsData.isEmpty()) {
                ChartPanel topItemsBarChartPanel = ChartGenerator.createTopSellingItemsBarChartPanel(topItemsData, "Top 5 Sản Phẩm Bán Chạy (Số Lượng)");
                chartDisplayPanel.add(topItemsBarChartPanel);
            } else {
                chartDisplayPanel.add(new JLabel("Không có dữ liệu sản phẩm bán chạy cho khoảng thời gian này."));
            }

            // 3. Biểu đồ Top Sản phẩm bán chạy (Pie chart)
            // (Bạn có thể dùng lại topItemsData hoặc query lại nếu muốn số lượng khác)
            if (!topItemsData.isEmpty()) {
                ChartPanel topItemsPieChartPanel = ChartGenerator.createTopSellingItemsPieChartPanel(topItemsData, "Tỷ Lệ Top Sản Phẩm Bán Chạy");
                chartDisplayPanel.add(topItemsPieChartPanel);
            }
            // Thêm các biểu đồ khác vào đây

        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu thống kê: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        chartDisplayPanel.revalidate();
        chartDisplayPanel.repaint();
    }
}
