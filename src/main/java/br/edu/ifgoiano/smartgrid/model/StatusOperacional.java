package br.edu.ifgoiano.smartgrid.model;

/** Estados operacionais de um medidor, com rótulo para exibição. */
public enum StatusOperacional {
    ATIVO("Ativo"),
    MANUTENCAO("Em manutenção"),
    OFFLINE("Offline");

    private final String rotulo;

    StatusOperacional(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return this.rotulo;
    }
}

