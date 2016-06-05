package db;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;




/**
 * Created by edgar on 29/08/2015.
 * Clase que maneja la conexion con la base de datos SQL Server y tiene
 * todos los metodos de base de datos. La conexion se mantiene siepre abierta
 */
public class SQLServerConnection {

    private Connection _conn;

    @SuppressLint("NewApi")
    public SQLServerConnection() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection connection = null;
        String ConnectionURL;

        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

            ConnectionURL = "jdbc:jtds:sqlserver://" + "SQL5020.Smarterasp.net:1433/DB_9EBA44_geopublish;";
            connection = DriverManager.getConnection(ConnectionURL, "DB_9EBA44_geopublish_admin", "geopublish");

            /*
            ConnectionURL = "jdbc:jtds:sqlserver://" + "DESKTOP-T95V4U4:1433\\SQLEXPRESS;";
            connection = DriverManager.getConnection(ConnectionURL, "sa", "system");
            */
        } catch (SQLException se) {
            Log.e("ERROR", se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("ERROR", e.getMessage());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }

        _conn = connection;
    }

    public Connection getConnection() {
        return _conn;
    }

    /**
     * Created by edgar Molibna on 09/05/2016.
     * Devuelve un balor booleano indicando  si se encuentran los datos de cliente y su promocion
     */
    public boolean ExistsClientPromo(String clientCode, String promoCode) {
        CallableStatement cs;
        ResultSet rs;

        try {
            cs = _conn.prepareCall("call ExistsClientPromo(?,?)");

            cs.setString("clientCode", clientCode);
            cs.setString("promoClientCode", promoCode);

            cs.execute();

            rs = cs.getResultSet();

            if  (rs.next()) return true;

            return false;

        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }

        return false;
    }

    /**
     * Created by edgar Molibna on 09/05/2016.
     * Obtiene las estadisticas generales del cliente y de un codigo de promocion
     *
     * @param date fecha actual del dispositivo
     * @param clientCode código indentificador de cliente
     * @param promoCode  código identificador de promoción
     */
    public ResultSet GetGeneralStats( java.sql.Timestamp date,String clientCode, String promoCode) {
        CallableStatement cs = null;
        ResultSet rs = null;

        try {
            if (_conn == null) return null;

            cs = _conn.prepareCall("{call GetGeneralStats(? ,?, ?)}");
            cs.setTimestamp("date", date);
            cs.setString("clientCode", clientCode);
            cs.setString("promoCode", promoCode);
            cs.execute();

            return cs.getResultSet();

        } catch (SQLException e) {

            System.err.println("SQLException: " + e.getMessage());
        } finally {
            if (cs != null) {

            }
        }

        return rs;
    }


    /**
     * Obtiene la ultima posicion conocida de un bus
     * @param tabletCode Codigo de la tablet o dispoistivo servidor presente en el bus
     * @return Latitude y longitud
     */
    public ResultSet GetLastBusRoutePosition (String tabletCode ) {
        CallableStatement cs = null;
        ResultSet rs = null;

        try {
            if (_conn == null) return null;

            cs = _conn.prepareCall("{call GetLastBusPosition(?)}");

            cs.setString("@id", tabletCode);
            cs.execute();



            return cs.getResultSet();

        } catch (SQLException e) {

            System.err.println("SQLException: " + e.getMessage());
        } finally {
            if (cs != null) {

            }
        }

        return rs;
    }

}
