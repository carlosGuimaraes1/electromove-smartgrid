package br.edu.ifgoiano.smartgrid.model;

/** Estação de recarga rápida DC, com taxa fixa de conexão e menos tomadas. */
public class MedidorEstacaoRapida extends MedidorEstacaoRecarga {

    private static final long serialVersionUID = 1L;
    private static final double TAXA_CONEXAO = 5.00;
    private static final int TOMADAS_DC = 2;

    public MedidorEstacaoRapida(String macAddress, String endereco, double tensaoAtual, StatusOperacional status, double potenciaMaxEstacaoKw) {
        super(macAddress, endereco, tensaoAtual, status, potenciaMaxEstacaoKw);
    }

    @Override
    public double calcularTarifa(double kwh) {
        return super.calcularTarifa(kwh) + TAXA_CONEXAO;
    }

    @Override
    public TipoCarregamento getTipoCarregamento() {
        return TipoCarregamento.RAPIDO;
    }

    @Override
    public int getQuantidadeTomadas() {
        return TOMADAS_DC;
    }

    @Override
    public double getLimitePorTomadaKw() {
        return getPotenciaMaxEstacaoKw();
    }
}