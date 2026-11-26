package br.edu.ifgoiano.smartgrid.model;

/** Contrato de gerenciamento de potência das tomadas de uma estação de recarga. */
public interface GerenciadorCarga {

    /** Reserva potência para uma tomada; lança exceção se exceder o limite. */
    void alocarPotenciaTomada(int idTomada, double kwSolicitado) throws LimitePotenciaExcedidoException;

    void liberarTomada(int idTomada);

    double getPotenciaAlocadaKw();
}