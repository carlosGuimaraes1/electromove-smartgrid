package br.edu.ifgoiano.smartgrid.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cria as tabelas na inicialização, pois o banco em memória começa vazio a cada execução.
 */
public final class InicializadorBanco {

    private static final String TABELA_MEDIDOR = """
            create table if not exists medidor (
                mac_address varchar(17) primary key,
                endereco varchar(150) not null,
                tensao_atual double not null,
                status_operacional varchar(20) not null
            )""";

    private static final String TABELA_ESTACAO = """
            create table if not exists medidor_estacao_recarga (
                mac_address varchar(17) primary key,
                potencia_max_kw double not null,
                vagas_ocupadas int not null,
                tipo_carregamento varchar(10) not null,
                foreign key (mac_address) references medidor(mac_address)
            )""";

    private InicializadorBanco() {
    }

    public static void criarTabelas() {
        try (Connection c = ConnectionFactory.getConnection();
             Statement st = c.createStatement()) {
            st.execute(TABELA_MEDIDOR);
            st.execute(TABELA_ESTACAO);
        } catch (SQLException e) {
            throw new DAOException("Falha ao criar as tabelas", e);
        }
    }
}
