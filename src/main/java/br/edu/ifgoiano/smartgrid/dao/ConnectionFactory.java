package br.edu.ifgoiano.smartgrid.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Fornece conexões com o banco H2 em memória. */
public final class ConnectionFactory {

    private static final String URL = "jdbc:h2:mem:smartgrid;DB_CLOSE_DELAY=-1";
    private static final String USUARIO = "sa";
    private static final String SENHA = "";

    private ConnectionFactory() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }
}