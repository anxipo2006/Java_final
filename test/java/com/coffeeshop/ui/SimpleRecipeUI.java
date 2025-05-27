package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.IngredientDAO;
import com.coffeeshop.core.dao.ItemDAO;
import com.coffeeshop.core.dao.RecipeDAO;
import com.coffeeshop.core.dao.RecipeDAO.RecipeEntryView;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Ingredient;
import com.coffeeshop.core.model.Item;
import com.coffeeshop.core.model.RecipeEntry;

public class SimpleRecipeUI extends JPanel {

    private final RecipeDAO recipeDAO = new RecipeDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final IngredientDAO ingredientDAO = new IngredientDAO();

    private JComboBox<ItemWrapper> cbItems;
    private JTable recipeDetailTable;
    private DefaultTableModel recipeTableModel;

    private JComboBox<IngredientWrapper> cbIngredientsForRecipe;
    private JTextField txtIngredientQuantity;
    private JButton btnAddIngredientToRecipe;
    private JButton btnUpdateIngredientInRecipe;
    private JButton btnRemoveIngredientFromRecipe;

    private JLabel lblSelectedRecipeItemName;
    private int selectedRecipeEntryRowId = -1;

    public SimpleRecipeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel itemSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        itemSelectionPanel.setBorder(BorderFactory.createTitledBorder("Chọn Sản Phẩm (Công Thức)"));
        itemSelectionPanel.add(new JLabel("Sản phẩm:"));
        cbItems = new JComboBox<>();
        loadItemsIntoComboBox();
        itemSelectionPanel.add(cbItems);
        lblSelectedRecipeItemName = new JLabel("Tên sản phẩm: ");
        itemSelectionPanel.add(lblSelectedRecipeItemName);

        JPanel recipeDetailPanel = new JPanel(new BorderLayout());
        recipeDetailPanel.setBorder(BorderFactory.createTitledBorder("Chi Tiết Công Thức"));
        recipeTableModel = new DefaultTableModel(new Object[]{"Row ID", "Ing. ID", "Tên Nguyên Liệu", "Số Lượng", "Đơn Vị"}, 0);
        recipeDetailTable = new JTable(recipeTableModel);
        recipeDetailPanel.add(new JScrollPane(recipeDetailTable), BorderLayout.CENTER);

        JPanel manageIngredientsPanel = new JPanel(new GridBagLayout());
        manageIngredientsPanel.setBorder(BorderFactory.createTitledBorder("Quản Lý Nguyên Liệu Trong Công Thức"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        manageIngredientsPanel.add(new JLabel("Nguyên liệu:"), gbc);
        gbc.gridx = 1;
        cbIngredientsForRecipe = new JComboBox<>();
        loadIngredientsForRecipeComboBox();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        manageIngredientsPanel.add(cbIngredientsForRecipe, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        manageIngredientsPanel.add(new JLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        txtIngredientQuantity = new JTextField(10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        manageIngredientsPanel.add(txtIngredientQuantity, gbc);

        JPanel recipeActionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnAddIngredientToRecipe = new JButton("Thêm vào CT");
        btnUpdateIngredientInRecipe = new JButton("Cập nhật trong CT");
        btnRemoveIngredientFromRecipe = new JButton("Xóa khỏi CT");
        recipeActionButtons.add(btnAddIngredientToRecipe);
        recipeActionButtons.add(btnUpdateIngredientInRecipe);
        recipeActionButtons.add(btnRemoveIngredientFromRecipe);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        manageIngredientsPanel.add(recipeActionButtons, gbc);

        add(itemSelectionPanel, BorderLayout.NORTH);
        add(recipeDetailPanel, BorderLayout.CENTER);
        add(manageIngredientsPanel, BorderLayout.SOUTH);

        cbItems.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ItemWrapper selectedItemWrapper = (ItemWrapper) cbItems.getSelectedItem();
                if (selectedItemWrapper != null) {
                    lblSelectedRecipeItemName.setText("Tên sản phẩm: " + selectedItemWrapper.getItem().getItemName());
                    loadRecipeDetailsForSelectedItem(selectedItemWrapper.getItem().getSku());
                    clearIngredientSelectionForm();
                } else {
                    lblSelectedRecipeItemName.setText("Tên sản phẩm: ");
                    recipeTableModel.setRowCount(0);
                }
            }
        });

        recipeDetailTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && recipeDetailTable.getSelectedRow() != -1) {
                int selectedRow = recipeDetailTable.getSelectedRow();
                selectedRecipeEntryRowId = (int) recipeTableModel.getValueAt(selectedRow, 0);
                String ingId = recipeTableModel.getValueAt(selectedRow, 1).toString();
                String quantity = recipeTableModel.getValueAt(selectedRow, 3).toString();

                for (int i = 0; i < cbIngredientsForRecipe.getItemCount(); i++) {
                    if (cbIngredientsForRecipe.getItemAt(i).getIngredient().getIngId().equals(ingId)) {
                        cbIngredientsForRecipe.setSelectedIndex(i);
                        break;
                    }
                }
                txtIngredientQuantity.setText(quantity);
                cbIngredientsForRecipe.setEnabled(false);
            }
        });

        btnAddIngredientToRecipe.addActionListener(e -> addIngredientToSelectedRecipe());
        btnUpdateIngredientInRecipe.addActionListener(e -> updateIngredientInSelectedRecipe());
        btnRemoveIngredientFromRecipe.addActionListener(e -> removeIngredientFromSelectedRecipe());

        if (cbItems.getItemCount() > 0) {
            cbItems.setSelectedIndex(0);
        } else {
            recipeTableModel.setRowCount(0);
        }
    }

    private void loadItemsIntoComboBox() {
        try {
            List<Item> items = itemDAO.getAllItems();
            items.sort((i1, i2) -> i1.getItemName().compareToIgnoreCase(i2.getItemName()));
            cbItems.removeAllItems();
            cbItems.addItem(null);
            for (Item item : items) {
                cbItems.addItem(new ItemWrapper(item));
            }
        } catch (DatabaseOperationException e) {
            showError("Lỗi tải danh sách sản phẩm", e);
        }
    }

    private void loadIngredientsForRecipeComboBox() {
        try {
            List<Ingredient> ingredients = ingredientDAO.getAllIngredients();
            ingredients.sort((i1, i2) -> i1.getIngName().compareToIgnoreCase(i2.getIngName()));
            cbIngredientsForRecipe.removeAllItems();
            cbIngredientsForRecipe.addItem(null);
            for (Ingredient ing : ingredients) {
                cbIngredientsForRecipe.addItem(new IngredientWrapper(ing));
            }
        } catch (DatabaseOperationException e) {
            showError("Lỗi tải danh sách nguyên liệu", e);
        }
    }

    private void clearIngredientSelectionForm() {
        cbIngredientsForRecipe.setSelectedIndex(0);
        txtIngredientQuantity.setText("");
        selectedRecipeEntryRowId = -1;
        cbIngredientsForRecipe.setEnabled(true);
        recipeDetailTable.clearSelection();
    }

    private void loadRecipeDetailsForSelectedItem(String itemSku) {
        recipeTableModel.setRowCount(0);
        if (itemSku == null || itemSku.isEmpty()) {
            return;
        }

        try {
            List<RecipeEntryView> details = recipeDAO.getRecipeDetails(itemSku);
            for (RecipeEntryView rev : details) {
                recipeTableModel.addRow(new Object[]{
                    rev.rowId,
                    rev.ingId,
                    rev.ingredientName,
                    rev.quantity,
                    rev.ingMeas
                });
            }
        } catch (DatabaseOperationException e) {
            showError("Lỗi tải chi tiết công thức", e);
        }
    }

    private void addIngredientToSelectedRecipe() {
        ItemWrapper selectedItemWrapper = (ItemWrapper) cbItems.getSelectedItem();
        IngredientWrapper selectedIngredientWrapper = (IngredientWrapper) cbIngredientsForRecipe.getSelectedItem();

        if (selectedItemWrapper == null || selectedIngredientWrapper == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm và nguyên liệu.");
            return;
        }

        try {
            int quantity = Integer.parseInt(txtIngredientQuantity.getText().trim());
            if (quantity <= 0) {
                throw new NumberFormatException();
            }

            RecipeEntry newEntry = new RecipeEntry(
                    selectedItemWrapper.getItem().getSku(),
                    selectedIngredientWrapper.getIngredient().getIngId(),
                    quantity
            );
            recipeDAO.addIngredientToRecipe(newEntry);

            JOptionPane.showMessageDialog(this, "Thêm nguyên liệu thành công!");
            loadRecipeDetailsForSelectedItem(newEntry.getRecipeId());
            clearIngredientSelectionForm();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương.");
        } catch (DatabaseOperationException e) {
            showError("Lỗi khi thêm nguyên liệu", e);
        }
    }

    private void updateIngredientInSelectedRecipe() {
        if (selectedRecipeEntryRowId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nguyên liệu từ bảng để cập nhật.");
            return;
        }

        try {
            int quantity = Integer.parseInt(txtIngredientQuantity.getText().trim());
            if (quantity <= 0) {
                throw new NumberFormatException();
            }

            ItemWrapper selectedItemWrapper = (ItemWrapper) cbItems.getSelectedItem();
            if (selectedItemWrapper == null) {
                return;
            }

            if (recipeDAO.updateRecipeEntryQuantity(selectedRecipeEntryRowId, quantity)) {
                JOptionPane.showMessageDialog(this, "Cập nhật thành công.");
                loadRecipeDetailsForSelectedItem(selectedItemWrapper.getItem().getSku());
                clearIngredientSelectionForm();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy entry cần cập nhật.");
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng phải là số nguyên dương.");
        } catch (DatabaseOperationException e) {
            showError("Lỗi khi cập nhật", e);
        }
    }

    private void removeIngredientFromSelectedRecipe() {
        if (selectedRecipeEntryRowId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nguyên liệu để xóa.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Xóa nguyên liệu khỏi công thức?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            ItemWrapper selectedItemWrapper = (ItemWrapper) cbItems.getSelectedItem();
            if (selectedItemWrapper == null) {
                return;
            }

            if (recipeDAO.removeIngredientFromRecipe(selectedRecipeEntryRowId)) {
                JOptionPane.showMessageDialog(this, "Xóa thành công!");
                loadRecipeDetailsForSelectedItem(selectedItemWrapper.getItem().getSku());
                clearIngredientSelectionForm();
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy entry cần xóa.");
            }

        } catch (DatabaseOperationException e) {
            showError("Lỗi khi xóa", e);
        }
    }

    private void showError(String title, Exception e) {
        JOptionPane.showMessageDialog(this, title + ": " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private static class ItemWrapper {

        private final Item item;

        public ItemWrapper(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }

        public String toString() {
            return item != null ? (item.getItemName() + " (SKU: " + item.getSku() + ")") : "Chọn sản phẩm";
        }
    }

    private static class IngredientWrapper {

        private final Ingredient ingredient;

        public IngredientWrapper(Ingredient ingredient) {
            this.ingredient = ingredient;
        }

        public Ingredient getIngredient() {
            return ingredient;
        }

        public String toString() {
            return ingredient != null ? (ingredient.getIngName() + " (ID: " + ingredient.getIngId() + ")") : "Chọn nguyên liệu";
        }
    }

    public static void main(String[] args) {
        // Test the SimpleRecipeUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Quản Lý Công Thức");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleRecipeUI());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
