package br.edu.ifgoiano.smartgrid.model;

import java.io.Serializable;
import java.util.Objects;

/** Superclasse abstrata dos medidores do SmartGrid. Corresponde à tabela medidor. */
public abstract class MedidorInteligente implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int TAMANHO_MAC = 17;

    private final String macAddress;
    private final String endereco;
    private double tensaoAtual;
    private StatusOperacional status;


    private transient String descricaoExibicao;

    public MedidorInteligente(String macAddress, String endereco, double tensaoAtual, StatusOperacional status) {
        if (macAddress == null || macAddress.isBlank() || macAddress.length() != TAMANHO_MAC) {
            throw new IllegalArgumentException("MAC inválido: deve ter exatamente 17 caracteres");
        }
        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException("Endereço não pode ser vazio");
        }
        validarTensao(tensaoAtual);
        this.macAddress = macAddress;
        this.endereco = endereco;
        this.tensaoAtual = tensaoAtual;
        this.status = Objects.requireNonNull(status, "Status não pode ser nulo");
    }

    /** Calcula o valor em reais para a energia consumida; cada subclasse define a regra. */
    public abstract double calcularTarifa(double kwh);

    private static void validarTensao(double tensao) {
        if (tensao < 0) {
            throw new IllegalArgumentException("Tensão não pode ser negativa");
        }
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getEndereco() {
        return endereco;
    }

    public double getTensaoAtual() {
        return tensaoAtual;
    }

    public void setTensaoAtual(double tensaoAtual) {
        validarTensao(tensaoAtual);
        this.tensaoAtual = tensaoAtual;
    }

    public StatusOperacional getStatus() {
        return status;
    }

    public void setStatus(StatusOperacional status) {
        this.status = Objects.requireNonNull(status, "Status não pode ser nulo");
    }

    public String getDescricaoExibicao() {
        if (descricaoExibicao == null) {
            descricaoExibicao = macAddress + " - " + endereco;
        }
        return descricaoExibicao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedidorInteligente outro)) return false;
        return macAddress.equals(outro.macAddress);
    }

    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{mac=" + macAddress
                + ", endereco=" + endereco
                + ", tensao=" + tensaoAtual
                + ", status=" + status.getRotulo() + "}";
    }
}