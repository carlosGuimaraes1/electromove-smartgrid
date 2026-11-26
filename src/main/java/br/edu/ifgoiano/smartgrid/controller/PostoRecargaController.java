package br.edu.ifgoiano.smartgrid.controller;

import br.edu.ifgoiano.smartgrid.model.*;
import br.edu.ifgoiano.smartgrid.service.MobilidadeService;

import java.util.List;

/** Intermedia view e service: normaliza entradas, chama as regras e traduz erros em mensagens. */
public class PostoRecargaController {

    /** Ação do controller que pode lançar a exceção de negócio. */
    @FunctionalInterface
    private interface Acao {
        String executar() throws LimitePotenciaExcedidoException;
    }

    private final MobilidadeService service;

    public PostoRecargaController(MobilidadeService service) {
        this.service = service;
    }

    public String cadastrarEstacao(String mac, String endereco, double tensao,
                                   double potenciaMax, boolean rapida) {
        String macNormalizado = mac.trim().toUpperCase();
        return executar(() -> {
            MedidorEstacaoRecarga estacao = rapida
                    ? new MedidorEstacaoRapida(macNormalizado, endereco, tensao, StatusOperacional.ATIVO, potenciaMax)
                    : new MedidorEstacaoRecarga(macNormalizado, endereco, tensao, StatusOperacional.ATIVO, potenciaMax);
            service.cadastrarEstacao(estacao);
            return "Estação cadastrada: " + estacao.getDescricaoExibicao();
        });
    }

    public List<String> listarEstacoes() {
        return service.listarEstacoes().stream()
                .map(e -> String.format("%s | %s | %s | %.0f kW | tomadas %d/%d | fila %d",
                        e.getDescricaoExibicao(), e.getTipoCarregamento(), e.getStatus().getRotulo(),
                        e.getPotenciaMaxEstacaoKw(), e.getVagasOcupadas(), e.getQuantidadeTomadas(),
                        service.tamanhoFila(e.getMacAddress())))
                .toList();
    }

    public String solicitarRecarga(String mac, String placa, double kw) {
        String macNormalizado = mac.trim().toUpperCase();
        String placaNormalizada = placa.trim().toUpperCase();
        return executar(() -> {
            boolean iniciou = service.solicitarRecarga(macNormalizado, placaNormalizada, kw);
            return iniciou
                    ? "Recarga iniciada para " + placaNormalizada
                    : "Sem vaga livre. " + placaNormalizada + " entrou na fila (posição "
                    + service.tamanhoFila(macNormalizado) + ")";
        });
    }

    public String finalizarRecarga(String placa, double kwh) {
        String placaNormalizada = placa.trim().toUpperCase();
        return executar(() -> {
            SessaoRecarga s = service.finalizarRecarga(placaNormalizada, kwh);
            return String.format("Sessão encerrada | %s | %s | %.2f kWh | R$ %.2f",
                    s.placa(), s.tipo(), s.kwhEntregue(), s.valor());
        });
    }

    public String alterarStatus(String mac, String status) {
        String macNormalizado = mac.trim().toUpperCase();
        return executar(() -> {
            StatusOperacional novo = StatusOperacional.valueOf(status.trim().toUpperCase());
            service.alterarStatus(macNormalizado, novo);
            return "Status alterado para " + novo.getRotulo();
        });
    }

    public String removerEstacao(String mac) {
        String macNormalizado = mac.trim().toUpperCase();
        return executar(() -> {
            service.removerEstacao(macNormalizado);
            return "Estação removida: " + macNormalizado;
        });
    }

    public String relatorio() {
        StringBuilder sb = new StringBuilder("=== Relatório ElectroMove ===\n");
        sb.append("Faturamento por estação:\n");
        service.faturamentoPorEstacao()
                .forEach((mac, valor) -> sb.append(String.format("  %s: R$ %.2f%n", mac, valor)));
        sb.append(String.format("Total faturado: R$ %.2f%n", service.faturamentoTotal()));
        sb.append("Energia entregue por tipo:\n");
        service.energiaPorTipo()
                .forEach((tipo, kwh) -> sb.append(String.format("  %s: %.2f kWh%n", tipo, kwh)));
        sb.append("Placas atendidas: ").append(service.placasAtendidas()).append('\n');
        sb.append("Estações com 50%+ de ocupação: ").append(service.estacoesComOcupacaoMinima(0.5));
        return sb.toString();
    }

    /** Executa a ação e converte qualquer erro em mensagem para a view. */
    private String executar(Acao acao) {
        try {
            return acao.executar();
        } catch (LimitePotenciaExcedidoException e) {
            return "Potência insuficiente: " + e.getMessage();
        } catch (RuntimeException e) {
            return "Erro: " + e.getMessage();
        }
    }
}