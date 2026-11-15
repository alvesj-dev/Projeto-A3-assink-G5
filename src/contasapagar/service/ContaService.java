package contasapagar.service;

import contasapagar.model.Conta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;

/*
 * Finalidade : geração da estrutura inicial desta classe (vetor de Conta,
 *              sub-rotinas de inclusão, alteração, exclusão, consulta, baixa
 *              e validações)

/**
 * Regras de negócio do sistema de Contas a Pagar.
 *
 * As contas ficam armazenadas em um vetor de objetos Conta (classe do pacote
 * model). Erros de validação lançam IllegalArgumentException com mensagem
 * pronta para ser exibida ao usuário (a tela deve pedir a redigitação).
 */
public class ContaService {

    // ---------- constantes ----------

    private static final int CAPACIDADE = 100;

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // ---------- dados ----------

    private final Conta[] contas = new Conta[CAPACIDADE];

    // A classe Conta não permite alterar o saldo devedor depois de criada.
    // Por isso os pagamentos (baixas) ficam neste vetor paralelo: a posição "i"
    // de cada vetor pertence à mesma conta.
    private final BigDecimal[] valoresPagos = new BigDecimal[CAPACIDADE];

    private int quantidade = 0; // quantidade de contas cadastradas

    // =====================================================================
    // CADASTRO
    // =====================================================================

    /** Inclui uma nova conta. O documento não pode estar repetido. */
    public void incluir(String documento, String descricao, String beneficiario,
                        BigDecimal valor, LocalDate vencimento) {
        if (quantidade >= CAPACIDADE) {
            throw new IllegalStateException("Limite de " + CAPACIDADE + " contas atingido.");
        }
        validarNovoDocumento(documento);

        contas[quantidade] = criarConta(documento, descricao, beneficiario, valor, vencimento);
        valoresPagos[quantidade] = BigDecimal.ZERO.setScale(2);
        quantidade++;
    }

    /**
     * Altera os dados de uma conta existente, mantendo o que já foi pago.
     * O número do documento não pode ser alterado (exclua e recadastre).
     */
    public void alterar(String documento, String descricao, String beneficiario,
                        BigDecimal valor, LocalDate vencimento) {
        int indice = localizar(documento);

        Conta nova = criarConta(contas[indice].getDocumento(), descricao, beneficiario, valor, vencimento);

        if (nova.getValorOriginal().compareTo(valoresPagos[indice]) < 0) {
            throw new IllegalArgumentException(
                    "O novo valor não pode ser menor que o já pago (R$ "
                    + formatarValor(valoresPagos[indice]) + ").");
        }
        contas[indice] = nova;
    }

    /** Exclui uma conta, deslocando as seguintes para não deixar "buraco" no vetor. */
    public void excluir(String documento) {
        int indice = localizar(documento);

        for (int i = indice; i < quantidade - 1; i++) {
            contas[i] = contas[i + 1];
            valoresPagos[i] = valoresPagos[i + 1];
        }
        quantidade--;
        contas[quantidade] = null;
        valoresPagos[quantidade] = null;
    }

    // =====================================================================
    // BAIXA
    // =====================================================================

    /** Registra um pagamento (parcial ou total) e atualiza o saldo devedor. */
    public void baixar(String documento, BigDecimal valorPago) {
        int indice = localizar(documento);

        if (valorPago == null || arredondar(valorPago).signum() <= 0) {
            throw new IllegalArgumentException("O valor pago deve ser maior que zero.");
        }
        BigDecimal v = arredondar(valorPago);
        BigDecimal saldo = saldoDaPosicao(indice);

        if (saldo.signum() == 0) {
            throw new IllegalArgumentException("Esta conta já está quitada.");
        }
        if (v.compareTo(saldo) > 0) {
            throw new IllegalArgumentException(
                    "O valor pago não pode ser maior que o saldo devedor (R$ "
                    + formatarValor(saldo) + ").");
        }
        valoresPagos[indice] = valoresPagos[indice].add(v);
    }

    // =====================================================================
    // CONSULTAS (usadas pelas telas e pelos relatórios)
    // =====================================================================

    /** Retorna a conta ou null se o documento não existir. */
    public Conta buscar(String documento) {
        int indice = buscarIndice(documento);
        return indice == -1 ? null : contas[indice];
    }

    /** Retorna a conta ou lança erro se o documento não existir. */
    public Conta consultar(String documento) {
        return contas[localizar(documento)];
    }

    public boolean existe(String documento) {
        return buscarIndice(documento) != -1;
    }

    public int getQuantidade() {
        return quantidade;
    }

    /** Conta da posição indicada (0 até getQuantidade() - 1). Útil para relatórios. */
    public Conta getConta(int indice) {
        if (indice < 0 || indice >= quantidade) {
            throw new IllegalArgumentException("Posição inválida: " + indice);
        }
        return contas[indice];
    }

    /** Cópia do vetor apenas com as contas cadastradas. */
    public Conta[] listar() {
        return Arrays.copyOf(contas, quantidade);
    }

    /** Total já pago da conta. Use este método em vez de conta.getSaldoDevedor(). */
    public BigDecimal getValorPago(Conta conta) {
        return valoresPagos[localizar(conta.getDocumento())];
    }

    /**
     * Saldo devedor atual = valor original - total pago.
     * Atenção: conta.getSaldoDevedor() não reflete as baixas; use este método.
     */
    public BigDecimal getSaldoDevedor(Conta conta) {
        return saldoDaPosicao(localizar(conta.getDocumento()));
    }

    /** Conta vencida = ainda tem saldo devedor e o vencimento já passou. */
    public boolean estaVencida(Conta conta) {
        return getSaldoDevedor(conta).signum() > 0
                && conta.getVencimento().isBefore(LocalDate.now());
    }

    // =====================================================================
    // VALIDAÇÕES (públicas para a tela validar campo a campo)
    // =====================================================================

    /** O documento deve ser informado e ainda não cadastrado. */
    public void validarNovoDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("O número do documento é obrigatório.");
        }
        if (existe(documento)) {
            throw new IllegalArgumentException("Documento " + documento.trim() + " já cadastrado.");
        }
    }

    public void validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " é obrigatório(a).");
        }
    }

    public void validarValor(BigDecimal valor) {
        if (valor == null || arredondar(valor).signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
    }

    // =====================================================================
    // CONVERSÕES (texto digitado -> tipo certo, com erro claro)
    // =====================================================================

    /** Aceita vírgula ou ponto como separador decimal (ex.: 150,75). */
    public static BigDecimal converterValor(String texto) {
        try {
            return new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Valor inválido. Exemplo: 150,75");
        }
    }

    /** Converte "dd/MM/aaaa" em data, recusando datas inexistentes (ex.: 31/02/2026). */
    public static LocalDate converterData(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FORMATO_DATA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Data inválida. Use o formato dd/mm/aaaa.");
        }
    }

    // =====================================================================
    // FORMATAÇÃO
    // =====================================================================

    public static String formatarData(LocalDate data) {
        return data.format(FORMATO_DATA);
    }

    /** Ex.: 1234.5 -> "1.234,50" */
    public static String formatarValor(BigDecimal valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", valor);
    }

    // =====================================================================
    // MÉTODOS INTERNOS
    // =====================================================================

    private int buscarIndice(String documento) {
        if (documento == null) {
            return -1;
        }
        String procurado = documento.trim();
        for (int i = 0; i < quantidade; i++) {
            if (contas[i].getDocumento().equals(procurado)) {
                return i;
            }
        }
        return -1;
    }

    /** Localiza o documento ou lança erro se não existir. */
    private int localizar(String documento) {
        int indice = buscarIndice(documento);
        if (indice == -1) {
            String texto = documento == null ? "" : documento.trim();
            throw new IllegalArgumentException("Documento " + texto + " não encontrado.");
        }
        return indice;
    }

    private BigDecimal saldoDaPosicao(int indice) {
        return contas[indice].getValorOriginal().subtract(valoresPagos[indice]);
    }

    /** Cria a Conta já com texto sem espaços sobrando e valor com 2 casas. */
    private Conta criarConta(String documento, String descricao, String beneficiario,
                             BigDecimal valor, LocalDate vencimento) {
        return new Conta(limpar(documento), limpar(descricao), limpar(beneficiario),
                valor == null ? null : arredondar(valor), vencimento);
    }

    private String limpar(String texto) {
        return texto == null ? null : texto.trim();
    }

    /** Mantém os valores com 2 casas decimais. */
    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
