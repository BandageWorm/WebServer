package wechat;

import mcs.FilePath;

import java.sql.*;

public class SqLite {

    static SqLite sqLite;
    Connection conn;
    Statement stmt;

    public synchronized static SqLite getInstance() {
        if (sqLite == null) {
            synchronized (SqLite.class) {
                sqLite = new SqLite();
            }
        }
        return sqLite;
    }

    private SqLite() {
        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + FilePath.get("") + "ontology.db");
            this.stmt = conn.createStatement();
            String sql = "create table if not exists ont(id char(30) primary key not null, query text not null)";
            this.stmt.executeUpdate(sql);
        } catch (SQLException e) {
            sqLite = null;
            e.printStackTrace();
        }
    }

    public void insert(String id, String query) {
        String sql = "insert into ont (id, query) values ('" + id + "', '" + query.replaceAll("'", "''") + "')";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(String id) {
        String sql = "delete from ont where id = '" + id + "'";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getQuery(String id) {
        String res = "";
        String sql = "select query from ont where id = '" + id + "'";
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                res = rs.getString("query");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }
}
