package br.edu.ifgoiano.smartgrid.dao;

import br.edu.ifgoiano.smartgrid.model.MedidorEstacaoRecarga;

import java.util.List;
import java.util.Optional;

/** Operações de persistência das estações de recarga. */
public interface PostoRecargaDAO {

    void inserir(MedidorEstacaoRecarga estacao);

    Optional<MedidorEstacaoRecarga> buscarPorMac(String mac);

    List<MedidorEstacaoRecarga> listarTodas();

    void atualizar(MedidorEstacaoRecarga estacao);

    void remover(String mac);
}