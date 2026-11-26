package br.edu.ifgoiano.smartgrid.model;

/** Tipos de recarga com tarifa por kWh (valores ilustrativos). */
public enum TipoCarregamento {
    LENTO(1.50),
    RAPIDO(2.50);

    private final double valorPorKwh;

    TipoCarregamento(double valorPorKwh) {
        this.valorPorKwh = valorPorKwh;
    }

    public double getValorPorKwh() {
        return this.valorPorKwh;
    }
}
