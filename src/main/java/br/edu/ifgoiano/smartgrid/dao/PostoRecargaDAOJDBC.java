package br.edu.ifgoiano.smartgrid.dao;

import br.edu.ifgoiano.smartgrid.model.MedidorEstacaoRapida;
import br.edu.ifgoiano.smartgrid.model.MedidorEstacaoRecarga;
import br.edu.ifgoiano.smartgrid.model.StatusOperacional;
import br.edu.ifgoiano.smartgrid.model.TipoCarregamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação JDBC do PostoRecargaDAO; grava nas tabelas medidor e medidor_estacao_recarga.
 */
public class PostoRecargaDAOJDBC implements PostoRecargaDAO {

    private static final String INSERT_MEDIDOR =
            "insert into medidor (mac_address, endereco, tensao_atual, status_operacional) values (?, ?, ?, ?)";
    private static final String INSERT_ESTACAO =
            "insert into medidor_estacao_recarga (mac_address, potencia_max_kw, vagas_ocupadas, tipo_carregamento) values (?, ?, ?, ?)";
    private static final String SELECT_BASE =
            "select m.mac_address, m.endereco, m.tensao_atual, m.status_operacional, "
                    + "e.potencia_max_kw, e.tipo_carregamento "
                    + "from medidor m join medidor_estacao_recarga e on e.mac_address = m.mac_address";
    private static final String SELECT_POR_MAC = SELECT_BASE + " where m.mac_address = ?";
    private static final String UPDATE_MEDIDOR =
            "update medidor set tensao_atual = ?, status_operacional = ? where mac_address = ?";
    private static final String UPDATE_ESTACAO =
            "update medidor_estacao_recarga set vagas_ocupadas = ? where mac_address = ?";
    private static final String DELETE_ESTACAO = "delete from medidor_estacao_recarga where mac_address = ?";
    private static final String DELETE_MEDIDOR = "delete from medidor where mac_address = ?";

    @Override
    public void inserir(MedidorEstacaoRecarga estacao) {
        emTransacao("Erro ao inserir estação " + estacao.getMacAddress(), c -> {
            try (PreparedStatement ps = c.prepareStatement(INSERT_MEDIDOR)) {
                ps.setString(1, estacao.getMacAddress());
                ps.setString(2, estacao.getEndereco());
                ps.setDouble(3, estacao.getTensaoAtual());
                ps.setString(4, estacao.getStatus().name());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(INSERT_ESTACAO)) {
                ps.setString(1, estacao.getMacAddress());
                ps.setDouble(2, estacao.getPotenciaMaxEstacaoKw());
                ps.setInt(3, estacao.getVagasOcupadas());
                ps.setString(4, estacao.getTipoCarregamento().name());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public Optional<MedidorEstacaoRecarga> buscarPorMac(String mac) {
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_POR_MAC)) {
            ps.setString(1, mac);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Erro ao buscar estação " + mac, e);
        }
    }

    @Override
    public List<MedidorEstacaoRecarga> listarTodas() {
        List<MedidorEstacaoRecarga> estacoes = new ArrayList<>();
        try (Connection c = ConnectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_BASE);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                estacoes.add(mapear(rs));
            }
            return estacoes;
        } catch (SQLException e) {
            throw new DAOException("Erro ao listar estações", e);
        }
    }

    @Override
    public void atualizar(MedidorEstacaoRecarga estacao) {
        emTransacao("Erro ao atualizar estação " + estacao.getMacAddress(), c -> {
            try (PreparedStatement ps = c.prepareStatement(UPDATE_MEDIDOR)) {
                ps.setDouble(1, estacao.getTensaoAtual());
                ps.setString(2, estacao.getStatus().name());
                ps.setString(3, estacao.getMacAddress());
                if (ps.executeUpdate() == 0) {
                    throw new DAOException("Estação não encontrada: " + estacao.getMacAddress());
                }
            }
            try (PreparedStatement ps = c.prepareStatement(UPDATE_ESTACAO)) {
                ps.setInt(1, estacao.getVagasOcupadas());
                ps.setString(2, estacao.getMacAddress());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void remover(String mac) {
        emTransacao("Erro ao remover estação " + mac, c -> {
            try (PreparedStatement ps = c.prepareStatement(DELETE_ESTACAO)) {
                ps.setString(1, mac);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(DELETE_MEDIDOR)) {
                ps.setString(1, mac);
                if (ps.executeUpdate() == 0) {
                    throw new DAOException("Estação não encontrada: " + mac);
                }
            }
        });
    }

    /**
     * Converte uma linha do ResultSet na subclasse correta, conforme o tipo gravado.
     */
    private MedidorEstacaoRecarga mapear(ResultSet rs) throws SQLException {
        String mac = rs.getString("mac_address");
        String endereco = rs.getString("endereco");
        double tensao = rs.getDouble("tensao_atual");
        StatusOperacional status = StatusOperacional.valueOf(rs.getString("status_operacional"));
        double potencia = rs.getDouble("potencia_max_kw");
        TipoCarregamento tipo = TipoCarregamento.valueOf(rs.getString("tipo_carregamento"));

        return switch (tipo) {
            case LENTO -> new MedidorEstacaoRecarga(mac, endereco, tensao, status, potencia);
            case RAPIDO -> new MedidorEstacaoRapida(mac, endereco, tensao, status, potencia);
        };
    }

    /**
     * Bloco de operações SQL que roda dentro de uma transação.
     */
    @FunctionalInterface
    private interface OperacaoSql {
        void executar(Connection c) throws SQLException;
    }

    /**
     * Executa a operação com commit no sucesso e rollback em qualquer falha.
     */
    private void emTransacao(String mensagemErro, OperacaoSql operacao) {
        try (Connection c = ConnectionFactory.getConnection()) {
            c.setAutoCommit(false);
            try {
                operacao.executar(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException(mensagemErro, e);
        }
    }
}