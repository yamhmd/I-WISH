package iwish.db;

import iwish.model.CatalogItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Read-only queries against the admin-seeded CatalogItems table. */
public class CatalogItemDAO {

    public List<CatalogItem> getAllItems() throws SQLException {
        String sql = "SELECT * FROM CatalogItems ORDER BY name";
        List<CatalogItem> items = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(mapRow(rs));
            }
        }
        return items;
    }

    /** Returns null if no item with that id exists. */
    public CatalogItem getItemById(int itemId) throws SQLException {
        String sql = "SELECT * FROM CatalogItems WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    private CatalogItem mapRow(ResultSet rs) throws SQLException {
        return new CatalogItem(
            rs.getInt("item_id"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getString("image_path")
        );
    }
}
