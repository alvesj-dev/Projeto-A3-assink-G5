package contasapagar.service;

import contasapagar.model.Conta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Geração dos relatórios do sistema de Contas a Pagar.
 *
 * Relatórios:
 *  1) Contas a pagar por período (com saldo devedor, vencimento no período)
 *  2) Contas vencidas por período (com saldo devedor e vencimento já passado)
 *  3) Balanço de despesas por período (totais de valor, pago e em aberto)
 *
 * Todos usam vetores (Conta[]) e são ordenados por data de vencimento.
 * Os saldos são sempre lidos do ContaService, pois a Conta não guarda as baixas.
 */
public class RelatorioService {

    // ---------- filtros internos ----------

    private static final int TODAS = 0;
    private static final int EM_ABERTO = 1;
    private static final int VENCIDAS = 2;

    private final ContaService contaService;

    public RelatorioService(ContaService contaService) {
        this.contaService = contaService;
    }

    // =====================================================================
    // BALANÇO (resultado do relatório 3)
    // =====================================================================

    /** Totais do balanço de despesas de um período. */
    public static class Balanco {
        private final int quantidade;
        private final BigDecimal totalOriginal;
        private final BigDecimal totalPago;
        private final BigDecimal saldoEmAberto;

        public Balanco(int quantidade, BigDecimal totalOriginal,
                       BigDecimal totalPago, BigDecimal saldoEmAberto) {
            this.quantidade = quantidade;
            this.totalOriginal = totalOriginal;
            this.totalPago = totalPago;
            this.saldoEmAberto = saldoEmAberto;
        }

        public int getQuantidade() { return quantidade; }
        public BigDecimal getTotalOriginal() { return totalOriginal; }
        public BigDecimal getTotalPago() { return totalPago; }
        public BigDecimal getSaldoEmAberto() { return saldoEmAberto; }
    }

    // =====================================================================
    // DADOS DOS RELATÓRIOS (vetores ordenados por vencimento)
    // =====================================================================

    /** Contas com saldo devedor e vencimento dentro do período. */
    public Conta[] contasAPagar(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return filtrar(inicio, fim, EM_ABERTO);
    }

    /** Contas com saldo devedor, vencimento no período e já vencidas. */
    public Conta[] contasVencidas(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        return filtrar(inicio, fim, VENCIDAS);
    }

    /** Totais de todas as contas com vencimento no período (pagas ou não). */
    public Balanco balanco(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        Conta[] doPeriodo = filtrar(inicio, fim, TODAS);

        BigDecimal original = BigDecimal.ZERO;
        BigDecimal pago = BigDecimal.ZERO;
        BigDecimal aberto = BigDecimal.ZERO;

        for (int i = 0; i < doPeriodo.length; i++) {
            original = original.add(doPeriodo[i].getValorOriginal());
            pago = pago.add(contaService.getValorPago(doPeriodo[i]));
            aberto = aberto.add(contaService.getSaldoDevedor(doPeriodo[i]));
        }
        return new Balanco(doPeriodo.length, original, pago, aberto);
    }

    // =====================================================================
    // TEXTO PRONTO PARA EXIBIR NA TELA
    // =====================================================================

    public String relatorioContasAPagar(LocalDate inicio, LocalDate fim) {
        Conta[] lista = contasAPagar(inicio, fim);

        StringBuilder sb = new StringBuilder();
        sb.append(titulo("CONTAS A PAGAR", inicio, fim));
        sb.append(String.format("%-12s %-22s %-18s %-10s %13s %13s%n",
                "DOCUMENTO", "DESCRICAO", "BENEFICIARIO", "VENCIMENTO", "VALOR (R$)", "SALDO (R$)"));
        sb.append(linha());

        BigDecimal totalSaldo = BigDecimal.ZERO;
        for (int i = 0; i < lista.length; i++) {
            Conta c = lista[i];
            BigDecimal saldo = contaService.getSaldoDevedor(c);
            totalSaldo = totalSaldo.add(saldo);

            sb.append(String.format("%-12s %-22s %-18s %-10s %13s %13s%n",
                    truncar(c.getDocumento(), 12),
                    truncar(c.getDescricao(), 22),
                    truncar(c.getBeneficiario(), 18),
                    ContaService.formatarData(c.getVencimento()),
                    ContaService.formatarValor(c.getValorOriginal()),
                    ContaService.formatarValor(saldo)));
        }
        if (lista.length == 0) {
            sb.append("Nenhuma conta a pagar no periodo informado.\n");
        }
        sb.append(linha());
        sb.append("Quantidade de contas: ").append(lista.length).append("\n");
        sb.append("Total a pagar: R$ ").append(ContaService.formatarValor(totalSaldo)).append("\n");
        return sb.toString();
    }

    public String relatorioContasVencidas(LocalDate inicio, LocalDate fim) {
        Conta[] lista = contasVencidas(inicio, fim);
        LocalDate hoje = LocalDate.now();

        StringBuilder sb = new StringBuilder();
        sb.append(titulo("CONTAS VENCIDAS", inicio, fim));
        sb.append(String.format("%-12s %-22s %-18s %-10s %6s %13s%n",
                "DOCUMENTO", "DESCRICAO", "BENEFICIARIO", "VENCIMENTO", "ATRASO", "SALDO (R$)"));
        sb.append(linha());

        BigDecimal totalSaldo = BigDecimal.ZERO;
        for (int i = 0; i < lista.length; i++) {
            Conta c = lista[i];
            BigDecimal saldo = contaService.getSaldoDevedor(c);
            totalSaldo = totalSaldo.add(saldo);
            long dias = ChronoUnit.DAYS.between(c.getVencimento(), hoje);

            sb.append(String.format("%-12s %-22s %-18s %-10s %4dd  %13s%n",
                    truncar(c.getDocumento(), 12),
                    truncar(c.getDescricao(), 22),
                    truncar(c.getBeneficiario(), 18),
                    ContaService.formatarData(c.getVencimento()),
                    dias,
                    ContaService.formatarValor(saldo)));
        }
        if (lista.length == 0) {
            sb.append("Nenhuma conta vencida no periodo informado.\n");
        }
        sb.append(linha());
        sb.append("Quantidade de contas: ").append(lista.length).append("\n");
        sb.append("Total vencido: R$ ").append(ContaService.formatarValor(totalSaldo)).append("\n");
        return sb.toString();
    }

    public String relatorioBalancoDespesas(LocalDate inicio, LocalDate fim) {
        Balanco b = balanco(inicio, fim);

        StringBuilder sb = new StringBuilder();
        sb.append(titulo("BALANCO DE DESPESAS", inicio, fim));
        sb.append("Contas no periodo ....: ").append(b.getQuantidade()).append("\n");
        sb.append("Total das despesas ...: R$ ")
                .append(ContaService.formatarValor(b.getTotalOriginal())).append("\n");
        sb.append("Total ja pago ........: R$ ")
                .append(ContaService.formatarValor(b.getTotalPago())).append("\n");
        sb.append("Saldo em aberto ......: R$ ")
                .append(ContaService.formatarValor(b.getSaldoEmAberto())).append("\n");
        return sb.toString();
    }

    // =====================================================================
    // MÉTODOS INTERNOS
    // =====================================================================

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Informe a data inicial e a data final.");
        }
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser maior que a data final.");
        }
    }

    /** Conta pertence ao período e atende ao filtro escolhido? */
    private boolean atende(Conta c, LocalDate inicio, LocalDate fim, int filtro) {
        LocalDate venc = c.getVencimento();
        if (venc.isBefore(inicio) || venc.isAfter(fim)) {
            return false;
        }
        if (filtro == EM_ABERTO) {
            return contaService.getSaldoDevedor(c).signum() > 0;
        }
        if (filtro == VENCIDAS) {
            return contaService.estaVencida(c);
        }
        return true;
    }

    /** Monta um vetor só com as contas que atendem ao filtro, já ordenado. */
    private Conta[] filtrar(LocalDate inicio, LocalDate fim, int filtro) {
        Conta[] todas = contaService.listar();

        int total = 0;
        for (int i = 0; i < todas.length; i++) {
            if (atende(todas[i], inicio, fim, filtro)) {
                total++;
            }
        }

        Conta[] resultado = new Conta[total];
        int pos = 0;
        for (int i = 0; i < todas.length; i++) {
            if (atende(todas[i], inicio, fim, filtro)) {
                resultado[pos] = todas[i];
                pos++;
            }
        }
        ordenarPorVencimento(resultado);
        return resultado;
    }

    /** Ordenação por inserção: vencimento crescente e, em empate, pelo documento. */
    private void ordenarPorVencimento(Conta[] vetor) {
        for (int i = 1; i < vetor.length; i++) {
            Conta atual = vetor[i];
            int j = i - 1;
            while (j >= 0 && vemDepois(vetor[j], atual)) {
                vetor[j + 1] = vetor[j];
                j--;
            }
            vetor[j + 1] = atual;
        }
    }

    private boolean vemDepois(Conta a, Conta b) {
        int cmp = a.getVencimento().compareTo(b.getVencimento());
        if (cmp == 0) {
            cmp = a.getDocumento().compareTo(b.getDocumento());
        }
        return cmp > 0;
    }

    private String titulo(String nome, LocalDate inicio, LocalDate fim) {
        return "\n" + linha()
                + nome + " - periodo de " + ContaService.formatarData(inicio)
                + " a " + ContaService.formatarData(fim) + "\n"
                + linha();
    }

    private String linha() {
        return "-".repeat(95) + "\n";
    }

    private String truncar(String texto, int tamanho) {
        if (texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho - 1) + ".";
    }
}
