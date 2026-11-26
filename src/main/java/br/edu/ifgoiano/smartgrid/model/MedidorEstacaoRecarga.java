package br.edu.ifgoiano.smartgrid.model;

import java.util.HashMap;
import java.util.Map;

/** Estação de recarga AC (lenta). Corresponde à tabela medidor_estacao_recarga. */
public class MedidorEstacaoRecarga extends MedidorInteligente implements GerenciadorCarga {

    private static final long serialVersionUID = 1L;
    private static final double LIMITE_TOMADA_AC_KW = 22.0;
    private static final int TOMADAS_AC = 4;

    private final double potenciaMaxEstacaoKw;

    private final Map<Integer, Double> potenciaPorTomada = new HashMap<>();

    public MedidorEstacaoRecarga(String macAddress, String endereco, double tensaoAtual, StatusOperacional status, double potenciaMaxEstacaoKw) {
        super(macAddress, endereco, tensaoAtual, status);
        if (potenciaMaxEstacaoKw <= 0) {
            throw new IllegalArgumentException("Potência máxima deve ser positiva");
        }
        this.potenciaMaxEstacaoKw = potenciaMaxEstacaoKw;
    }

    @Override
    public void alocarPotenciaTomada(int idTomada, double kwSolicitado) throws LimitePotenciaExcedidoException {
        if (getStatus() != StatusOperacional.ATIVO) {
            throw new IllegalStateException("Estação " + getMacAddress() + " não está ativa");
        }
        if (idTomada < 1 || idTomada > getQuantidadeTomadas()) {
            throw new IllegalArgumentException("Tomada inexistente: " + idTomada);
        }
        if (kwSolicitado <= 0) {
            throw new IllegalArgumentException("Potência solicitada deve ser positiva");
        }
        if (kwSolicitado > getLimitePorTomadaKw()) {
            throw new LimitePotenciaExcedidoException(String.format("Tomada %d: %.1f kW excede o limite de %.1f kW por tomada",
                    idTomada, kwSolicitado, getLimitePorTomadaKw()));
        }
        double alocadoNasOutras = getPotenciaAlocadaKw() - potenciaPorTomada.getOrDefault(idTomada, 0.0);
        if (alocadoNasOutras + kwSolicitado > potenciaMaxEstacaoKw) {
            throw new LimitePotenciaExcedidoException(String.format("Estação %s: livre %.1f kW, solicitado %.1f kW",
                    getMacAddress(), potenciaMaxEstacaoKw - alocadoNasOutras, kwSolicitado));
        }
        potenciaPorTomada.put(idTomada, kwSolicitado);
    }

    @Override
    public void liberarTomada(int idTomada) {
        potenciaPorTomada.remove(idTomada);
    }

    @Override
    public double getPotenciaAlocadaKw() {
        return potenciaPorTomada.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Override
    public double calcularTarifa(double kwh) {
        if (kwh < 0) {
            throw new IllegalArgumentException("Energia não pode ser negativa");
        }
        return kwh * getTipoCarregamento().getValorPorKwh();
    }

    public int getVagasOcupadas() {
        return potenciaPorTomada.size();
    }

    public boolean possuiVagaLivre() {
        return getVagasOcupadas() < getQuantidadeTomadas();
    }

    public double getPotenciaMaxEstacaoKw() {
        return potenciaMaxEstacaoKw;
    }

    public TipoCarregamento getTipoCarregamento() {
        return TipoCarregamento.LENTO;
    }

    public int getQuantidadeTomadas() {
        return TOMADAS_AC;
    }

    public double getLimitePorTomadaKw() {
        return LIMITE_TOMADA_AC_KW;
    }
}