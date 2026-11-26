package br.edu.ifgoiano.smartgrid.view;

import br.edu.ifgoiano.smartgrid.controller.PostoRecargaController;

import java.util.List;
import java.util.Scanner;

/** Interface de console do ElectroMove; apenas lê entradas e exibe respostas do controller. */
public class MenuConsole {

    private final PostoRecargaController controller;
    private final Scanner scanner = new Scanner(System.in);

    public MenuConsole(PostoRecargaController controller) {
        this.controller = controller;
    }

    public void iniciar() {
        int opcao;
        do {
            exibirMenu();
            opcao = lerInt("Opção: ");
            switch (opcao) {
                case 1 -> cadastrar();
                case 2 -> listar();
                case 3 -> solicitar();
                case 4 -> finalizar();
                case 5 -> alterarStatus();
                case 6 -> System.out.println(controller.removerEstacao(lerTexto("MAC: ")));
                case 7 -> System.out.println(controller.relatorio());
                case 0 -> System.out.println("Encerrando.");
                default -> System.out.println("Opção inválida.");
            }
        } while (opcao != 0);
    }

    private void exibirMenu() {
        System.out.println("""

                ===== ElectroMove Rota Verde =====
                1 - Cadastrar estação
                2 - Listar estações
                3 - Solicitar recarga
                4 - Finalizar recarga
                5 - Alterar status da estação
                6 - Remover estação
                7 - Relatório
                0 - Sair""");
    }

    private void cadastrar() {
        String mac = lerTexto("MAC (AA:BB:CC:DD:EE:FF): ");
        String endereco = lerTexto("Endereço: ");
        double tensao = lerDouble("Tensão (V): ");
        double potencia = lerDouble("Potência máxima (kW): ");
        boolean rapida = lerInt("Tipo (1 = AC lenta, 2 = DC rápida): ") == 2;
        System.out.println(controller.cadastrarEstacao(mac, endereco, tensao, potencia, rapida));
    }

    private void listar() {
        List<String> estacoes = controller.listarEstacoes();
        if (estacoes.isEmpty()) {
            System.out.println("Nenhuma estação cadastrada.");
        } else {
            estacoes.forEach(System.out::println);
        }
    }

    private void solicitar() {
        String mac = lerTexto("MAC da estação: ");
        String placa = lerTexto("Placa do veículo: ");
        double kw = lerDouble("Potência solicitada (kW): ");
        System.out.println(controller.solicitarRecarga(mac, placa, kw));
    }

    private void finalizar() {
        String placa = lerTexto("Placa do veículo: ");
        double kwh = lerDouble("Energia entregue (kWh): ");
        System.out.println(controller.finalizarRecarga(placa, kwh));
    }

    private void alterarStatus() {
        String mac = lerTexto("MAC da estação: ");
        String status = lerTexto("Novo status (ATIVO, MANUTENCAO, OFFLINE): ");
        System.out.println(controller.alterarStatus(mac, status));
    }

    private String lerTexto(String rotulo) {
        System.out.print(rotulo);
        return scanner.nextLine().trim();
    }

    private double lerDouble(String rotulo) {
        while (true) {
            try {
                return Double.parseDouble(lerTexto(rotulo).replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("Número inválido, tente novamente.");
            }
        }
    }

    private int lerInt(String rotulo) {
        while (true) {
            try {
                return Integer.parseInt(lerTexto(rotulo));
            } catch (NumberFormatException e) {
                System.out.println("Número inválido, tente novamente.");
            }
        }
    }
}