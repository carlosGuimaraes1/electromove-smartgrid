package br.edu.ifgoiano.smartgrid.model;

/**
 * Sessão de recarga finalizada, usada no faturamento e nos relatórios.
 */
public record SessaoRecarga(String macEstacao, int idTomada, String placa, TipoCarregamento tipo, double kwhEntregue, double valor) {

}