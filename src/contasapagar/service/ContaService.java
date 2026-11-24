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

/**
 * Classe     : ContaService
 * Finalidade : regras de negócio do sistema de Contas a Pagar (inclusão,
 *              alteração, exclusão, consulta, baixa e validações).
 * 
 *
 * As contas ficam armazenadas em um vetor de objetos Conta (classe do pacote
 * model). Erros de validação lançam IllegalArgumentException com mensagem
 * pronta para ser exibida ao usuário (a tela deve pedir a redigitação).
 *
 * Validações exigidas no enunciado: documento, valor do débito e data de
 * vencimento (além de descrição e beneficiário).
 */
public class ContaService {

    // ---------- constantes ----------

    private static final int CAPACIDADE = 100;

    private static final int TAMANHO_MAXIMO_DOCUMENTO = 9;  // dígitos
    private static final int TAMANHO_MAXIMO_TEXTO = 30;     // descrição e beneficiário
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("999999999.99");
    private static final int ANO_MINIMO = 2000;
    private static final int ANO_MAXIMO = 2100;

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // ---------- dados ----------

    private final Conta[] contas = new Conta[CAPACIDADE];
    private int quantidade = 0; // quantidade de contas cadastradas

    // =====================================================================
    // CADASTRO
    // =====================================================================

    /** Inclui uma nova conta. Valida documento, textos, valor e vencimento. */
    public void incluir(String documento, String descricao, String beneficiario,
                        BigDecimal valor, LocalDate vencimento) {
        if (quantidade >= CAPACIDADE) {
            throw new IllegalStateException("Limite de " + CAPACIDADE + " contas atingido.");
        }
        validarNovoDocumento(documento);
        validarDados(descricao, beneficiario, valor, vencimento);

        contas[quantidade] = criarConta(normalizarDocumento(documento),
                descricao, beneficiario, valor, vencimento);
        quantidade++;
    }

    /**
     * Altera os dados de uma conta existente, mantendo o que já foi pago.
     * O número do documento não pode ser alterado (exclua e recadastre).
     */
    public void alterar(String documento, String descricao, String beneficiario,
                        BigDecimal valor, LocalDate vencimento) {
        int indice = localizar(documento);
        validarDados(descricao, beneficiario, valor, vencimento);

        BigDecimal jaPago = contas[indice].getValorPago();
        Conta nova = criarConta(contas[indice].getDocumento(), descricao, beneficiario, valor, vencimento);

        if (nova.getValorOriginal().compareTo(jaPago) < 0) {
            throw new IllegalArgumentException(
                    "O novo valor não pode ser menor que o já pago (R$ "
                    + formatarValor(jaPago) + ").");
        }
        if (jaPago.signum() > 0) {
            nova.registrarPagamento(jaPago);
        }
        contas[indice] = nova;
    }

    /** Exclui uma conta, deslocando as seguintes para não deixar "buraco" no vetor. */
    public void excluir(String documento) {
        int indice = localizar(documento);

        for (int i = indice; i < quantidade - 1; i++) {
            contas[i] = contas[i + 1];
        }
        quantidade--;
        contas[quantidade] = null;
    }

    // =====================================================================
    // BAIXA
    // =====================================================================

    /** Registra um pagamento (parcial ou total) e atualiza o saldo devedor. */
    public void baixar(String documento, BigDecimal valorPago) {
        Conta conta = contas[localizar(documento)];

        validarValor(valorPago);
        BigDecimal v = arredondar(valorPago);
        BigDecimal saldo = conta.getSaldoDevedor();

        if (saldo.signum() == 0) {
            throw new IllegalArgumentException("Esta conta já está quitada.");
        }
        if (v.compareTo(saldo) > 0) {
            throw new IllegalArgumentException(
                    "O valor pago não pode ser maior que o saldo devedor (R$ "
                    + formatarValor(saldo) + ").");
        }
        conta.registrarPagamento(v);
    }

    // =====================================================================
    // CONSULTAS (usadas pelas telas e pelos relatórios)
    // =====================================================================

    /** Retorna a conta ou null se o documento não existir (ou for inválido). */
    public Conta buscar(String documento) {
        int indice = indiceOuMenosUm(documento);
        return indice == -1 ? null : contas[indice];
    }

    /** Retorna a conta ou lança erro se o documento for inválido ou não existir. */
    public Conta consultar(String documento) {
        return contas[localizar(documento)];
    }

    public boolean existe(String documento) {
        return indiceOuMenosUm(documento) != -1;
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

    /** Total já pago da conta. */
    public BigDecimal getValorPago(Conta conta) {
        return conta.getValorPago();
    }

    /** Saldo devedor atual (valor original - total pago). */
    public BigDecimal getSaldoDevedor(Conta conta) {
        return conta.getSaldoDevedor();
    }

    /** Conta vencida = ainda tem saldo devedor e o vencimento já passou. */
    public boolean estaVencida(Conta conta) {
        return conta.getSaldoDevedor().signum() > 0
                && conta.getVencimento().isBefore(LocalDate.now());
    }

    // =====================================================================
    // VALIDAÇÕES (públicas para a tela validar campo a campo)
    // =====================================================================

    /**
     * Valida o formato do documento: apenas dígitos, maior que zero e com no
     * máximo 9 dígitos. Zeros à esquerda são ignorados ("001" é o documento 1).
     */
    public void validarDocumento(String documento) {
        normalizarDocumento(documento);
    }

    /** Documento com formato válido e ainda não cadastrado. */
    public void validarNovoDocumento(String documento) {
        String doc = normalizarDocumento(documento);
        if (buscarIndice(doc) != -1) {
            throw new IllegalArgumentException("Documento " + doc + " já cadastrado.");
        }
    }

    /** Texto obrigatório, com no máximo 30 caracteres (cabe no relatório). */
    public void validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " é obrigatório(a).");
        }
        if (texto.trim().length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    campo + " deve ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
    }

    /** Valor maior que zero, com no máximo 2 casas decimais e limite máximo. */
    public void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.");
        }
        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "O valor máximo permitido é R$ " + formatarValor(VALOR_MAXIMO) + ".");
        }
    }

    /** Data informada e com ano entre 2000 e 2100. */
    public void validarVencimento(LocalDate vencimento) {
        if (vencimento == null) {
            throw new IllegalArgumentException("Data de vencimento inválida.");
        }
        if (vencimento.getYear() < ANO_MINIMO || vencimento.getYear() > ANO_MAXIMO) {
            throw new IllegalArgumentException(
                    "O ano do vencimento deve estar entre " + ANO_MINIMO + " e " + ANO_MAXIMO + ".");
        }
    }

    // =====================================================================
    // CONVERSÕES (texto digitado -> tipo certo, com erro claro)
    // =====================================================================

    /** Aceita vírgula ou ponto como separador decimal (ex.: 150,75). */
    public static BigDecimal converterValor(String texto) {
        if (texto == null || !texto.trim().matches("\\d+([.,]\\d+)?")) {
            throw new IllegalArgumentException("Valor inválido. Exemplo: 150,75");
        }
        return new BigDecimal(texto.trim().replace(",", "."));
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

    /** Valida o formato e devolve o documento sem espaços e sem zeros à esquerda. */
    private String normalizarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("O número do documento é obrigatório.");
        }
        String doc = documento.trim();
        if (!doc.matches("\\d+")) {
            throw new IllegalArgumentException("Documento inválido. Digite apenas números.");
        }
        doc = doc.replaceFirst("^0+", "");
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("O número do documento deve ser maior que zero.");
        }
        if (doc.length() > TAMANHO_MAXIMO_DOCUMENTO) {
            throw new IllegalArgumentException(
                    "O documento deve ter no máximo " + TAMANHO_MAXIMO_DOCUMENTO + " dígitos.");
        }
        return doc;
    }

    /** Valida descrição, beneficiário, valor e vencimento de uma vez. */
    private void validarDados(String descricao, String beneficiario,
                              BigDecimal valor, LocalDate vencimento) {
        validarTexto(descricao, "Descrição");
        validarTexto(beneficiario, "Beneficiário");
        validarValor(valor);
        validarVencimento(vencimento);
    }

    /** Posição do documento (já normalizado) no vetor, ou -1. */
    private int buscarIndice(String documentoNormalizado) {
        for (int i = 0; i < quantidade; i++) {
            if (contas[i].getDocumento().equals(documentoNormalizado)) {
                return i;
            }
        }
        return -1;
    }

    /** Como buscarIndice, mas aceita qualquer texto: inválido vira -1. */
    private int indiceOuMenosUm(String documento) {
        try {
            return buscarIndice(normalizarDocumento(documento));
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    /** Localiza o documento ou lança erro (formato inválido ou não encontrado). */
    private int localizar(String documento) {
        String doc = normalizarDocumento(documento);
        int indice = buscarIndice(doc);
        if (indice == -1) {
            throw new IllegalArgumentException("Documento " + doc + " não encontrado.");
        }
        return indice;
    }

    /** Cria a Conta já com texto sem espaços sobrando e valor com 2 casas. */
    private Conta criarConta(String documento, String descricao, String beneficiario,
                             BigDecimal valor, LocalDate vencimento) {
        return new Conta(documento, descricao.trim(), beneficiario.trim(),
                arredondar(valor), vencimento);
    }

    /** Mantém os valores com 2 casas decimais. */
    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
