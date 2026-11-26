package br.edu.ifgoiano.smartgrid.service;

import br.edu.ifgoiano.smartgrid.dao.PostoRecargaDAO;
import br.edu.ifgoiano.smartgrid.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Regras de negócio da mobilidade elétrica: potência, fila, faturamento e relatórios. */
public class MobilidadeService {

    /** Pedido de recarga aguardando vaga. */
    private record SolicitacaoRecarga(String placa, double kwSolicitado) {
    }

    /** Recarga em andamento numa tomada. */
    private record SessaoAtiva(String macEstacao, int idTomada) {
    }

    private final PostoRecargaDAO dao;
    private final Map<String, MedidorEstacaoRecarga> estacoes = new LinkedHashMap<>();
    private final Map<String, Queue<SolicitacaoRecarga>> filas = new HashMap<>();
    private final Map<String, SessaoAtiva> sessoesAtivas = new HashMap<>(); // placa -> sessão
    private final List<SessaoRecarga> historico = new ArrayList<>();

    public MobilidadeService(PostoRecargaDAO dao) {
        this.dao = dao;
        dao.listarTodas().forEach(e -> estacoes.put(e.getMacAddress(), e));
    }

    // ---------- Cadastro ----------

    public void cadastrarEstacao(MedidorEstacaoRecarga estacao) {
        if (estacoes.containsKey(estacao.getMacAddress())) {
            throw new IllegalArgumentException("Já existe estação com o MAC " + estacao.getMacAddress());
        }
        dao.inserir(estacao);
        estacoes.put(estacao.getMacAddress(), estacao);
    }

    public List<MedidorEstacaoRecarga> listarEstacoes() {
        return List.copyOf(estacoes.values());
    }

    public void alterarStatus(String mac, StatusOperacional status) {
        MedidorEstacaoRecarga estacao = buscar(mac);
        estacao.setStatus(status);
        dao.atualizar(estacao);
    }

    public void removerEstacao(String mac) {
        buscar(mac);
        boolean emUso = sessoesAtivas.values().stream().anyMatch(s -> s.macEstacao().equals(mac));
        if (emUso || !fila(mac).isEmpty()) {
            throw new IllegalStateException("Estação com recargas em andamento ou fila não pode ser removida");
        }
        dao.remover(mac);
        estacoes.remove(mac);
        filas.remove(mac);
    }

    // ---------- Recarga ----------

    /**
     * Inicia a recarga se houver tomada livre; senão, coloca o veículo na fila.
     *
     * @return true se iniciou, false se entrou na fila
     * @throws LimitePotenciaExcedidoException se não houver potência suficiente
     */
    public boolean solicitarRecarga(String mac, String placa, double kwSolicitado)
            throws LimitePotenciaExcedidoException {
        MedidorEstacaoRecarga estacao = buscar(mac);
        validarPlacaLivre(placa);
        if (estacao.getStatus() != StatusOperacional.ATIVO) {
            throw new IllegalStateException("Estação " + mac + " não está ativa");
        }
        if (kwSolicitado <= 0) {
            throw new IllegalArgumentException("Potência solicitada deve ser positiva");
        }
        if (kwSolicitado > estacao.getLimitePorTomadaKw()) {
            throw new LimitePotenciaExcedidoException(String.format(
                    "%.1f kW excede o limite de %.1f kW por tomada desta estação",
                    kwSolicitado, estacao.getLimitePorTomadaKw()));
        }
        if (!estacao.possuiVagaLivre()) {
            fila(mac).add(new SolicitacaoRecarga(placa, kwSolicitado));
            return false;
        }
        iniciar(estacao, placa, kwSolicitado);
        return true;
    }

    /** Encerra a recarga, fatura a sessão e chama o próximo da fila. */
    public SessaoRecarga finalizarRecarga(String placa, double kwhEntregue) {
        SessaoAtiva ativa = sessoesAtivas.get(placa);
        if (ativa == null) {
            throw new IllegalArgumentException("Nenhuma recarga ativa para a placa " + placa);
        }
        MedidorEstacaoRecarga estacao = buscar(ativa.macEstacao());

        MedidorInteligente medidor = estacao;            // polimorfismo: a regra é a da subclasse real
        double valor = medidor.calcularTarifa(kwhEntregue);

        GerenciadorCarga gerenciador = estacao;          // polimorfismo via interface
        gerenciador.liberarTomada(ativa.idTomada());
        sessoesAtivas.remove(placa);

        SessaoRecarga sessao = new SessaoRecarga(estacao.getMacAddress(), ativa.idTomada(), placa,
                estacao.getTipoCarregamento(), kwhEntregue, valor);
        historico.add(sessao);

        processarFila(estacao);
        dao.atualizar(estacao);
        return sessao;
    }

    public int tamanhoFila(String mac) {
        buscar(mac);
        return fila(mac).size();
    }

    // ---------- Relatórios (Streams) ----------

    public Map<String, Double> faturamentoPorEstacao() {
        return historico.stream()
                .collect(Collectors.groupingBy(SessaoRecarga::macEstacao,
                        Collectors.summingDouble(SessaoRecarga::valor)));
    }

    public double faturamentoTotal() {
        return historico.stream().mapToDouble(SessaoRecarga::valor).sum();
    }

    public Map<TipoCarregamento, Double> energiaPorTipo() {
        return historico.stream()
                .collect(Collectors.groupingBy(SessaoRecarga::tipo,
                        Collectors.summingDouble(SessaoRecarga::kwhEntregue)));
    }

    /** @param percentual valor entre 0 e 1 (ex.: 0.5 = metade das tomadas ocupadas) */
    public List<String> estacoesComOcupacaoMinima(double percentual) {
        return estacoes.values().stream()
                .filter(e -> (double) e.getVagasOcupadas() / e.getQuantidadeTomadas() >= percentual)
                .map(MedidorInteligente::getDescricaoExibicao)
                .collect(Collectors.toList());
    }

    public Set<String> placasAtendidas() {
        return historico.stream()
                .map(SessaoRecarga::placa)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public List<SessaoRecarga> getHistorico() {
        return List.copyOf(historico);
    }

    // ---------- Internos ----------

    private void iniciar(MedidorEstacaoRecarga estacao, String placa, double kw)
            throws LimitePotenciaExcedidoException {
        int idTomada = tomadaLivre(estacao);
        GerenciadorCarga gerenciador = estacao;
        gerenciador.alocarPotenciaTomada(idTomada, kw);
        sessoesAtivas.put(placa, new SessaoAtiva(estacao.getMacAddress(), idTomada));
        dao.atualizar(estacao);
    }

    /** Atende a fila em ordem; se o primeiro não couber na potência livre, ele continua esperando. */
    private void processarFila(MedidorEstacaoRecarga estacao) {
        if (estacao.getStatus() != StatusOperacional.ATIVO) {
            return;
        }
        Queue<SolicitacaoRecarga> fila = fila(estacao.getMacAddress());
        while (estacao.possuiVagaLivre() && !fila.isEmpty()) {
            SolicitacaoRecarga proxima = fila.peek();
            try {
                iniciar(estacao, proxima.placa(), proxima.kwSolicitado());
                fila.poll();
            } catch (LimitePotenciaExcedidoException e) {
                break;
            }
        }
    }

    private int tomadaLivre(MedidorEstacaoRecarga estacao) {
        Set<Integer> ocupadas = sessoesAtivas.values().stream()
                .filter(s -> s.macEstacao().equals(estacao.getMacAddress()))
                .map(SessaoAtiva::idTomada)
                .collect(Collectors.toSet());
        return IntStream.rangeClosed(1, estacao.getQuantidadeTomadas())
                .filter(id -> !ocupadas.contains(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sem tomada livre"));
    }

    private void validarPlacaLivre(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("Placa não pode ser vazia");
        }
        boolean naFila = filas.values().stream()
                .flatMap(Queue::stream)
                .anyMatch(s -> s.placa().equals(placa));
        if (sessoesAtivas.containsKey(placa) || naFila) {
            throw new IllegalArgumentException("Veículo " + placa + " já está em recarga ou na fila");
        }
    }

    private Queue<SolicitacaoRecarga> fila(String mac) {
        return filas.computeIfAbsent(mac, k -> new ArrayDeque<>());
    }

    private MedidorEstacaoRecarga buscar(String mac) {
        return Optional.ofNullable(estacoes.get(mac))
                .orElseThrow(() -> new IllegalArgumentException("Estação não encontrada: " + mac));
    }
}
