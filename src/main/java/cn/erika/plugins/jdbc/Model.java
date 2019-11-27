package cn.erika.plugins.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

public class Model {
    private String tableName;
    private HashMap<String, Object> attr;

    {
        this.attr = new HashMap<>();
    }

    public <T> void set(String k, T v) {
        attr.put(k, v);
    }

    public <T> T get(String k) {
        try {
            return (T) attr.get(k);
        } catch (ClassCastException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public void remove(String k) {
        if (attr.containsKey(k)) {
            attr.remove(k);
        }
    }

    public String insert(String table) {
        StringBuilder buff = new StringBuilder("INSERT INTO " + table + " (");
        for (String column : attr.keySet()) {
            buff.append("`").append(column).append("`,");
        }
        buff.deleteCharAt(buff.length() - 1);
        buff.append(") VALUES (");
        for (String column : attr.keySet()) {
            buff.append("'").append(String.valueOf(get(column))).append("',");
        }
        buff.deleteCharAt(buff.length() - 1);
        buff.append(")");
        return buff.toString();
    }

    public String update(String primaryKey) throws Exception {
        if (!attr.containsKey(primaryKey)) {
            throw new Exception("主键不存在");
        }
        StringBuilder buff = new StringBuilder("UPDATE " + tableName + " SET ");
        for (String column : attr.keySet()) {
            if (column.equals(primaryKey)) {
                continue;
            }
            buff.append("`").append(column).append("`='").append(attr.get(column)).append("',");
        }
        buff.deleteCharAt(buff.length() - 1);
        buff.append(" WHERE `").append(primaryKey).append("`='").append(attr.get(primaryKey)).append("'");
        return buff.toString();
    }

    public String delete(String primaryKey) throws Exception {
        if (!attr.containsKey(primaryKey)) {
            throw new Exception("主键不存在");
        }
        return "DELETE FROM " + tableName + " WHERE `" + primaryKey + "`='" + attr.get(primaryKey) + "'";
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder("Model{");
        for (String key : attr.keySet()) {
            buff.append(key).append("='").append(attr.get(key)).append("',");
        }
        buff.deleteCharAt(buff.length() - 1);
        buff.append("}");
        return buff.toString();
    }

    static LinkedList<Model> convert(String tableName, ResultSet rs) throws SQLException {
        LinkedList<Model> list = null;
        if (rs != null) {
            list = new LinkedList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Model mod = new Model();
                mod.tableName = tableName;
                for (int i = 0; i < columnCount; i++) {
                    mod.set(meta.getColumnName(i + 1), rs.getObject(i + 1));
                }
                list.add(mod);
            }
        }
        return list;
    }
}
