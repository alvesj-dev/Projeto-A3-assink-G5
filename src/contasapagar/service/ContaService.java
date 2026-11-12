package service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/*
 
 * Finalidade : geração da estrutura inicial desta classe (vetores, sub-rotinas
 *              de inclusão, alteração, exclusão, consulta, baixa e validações)
 
 */

/**
 * Regras de negócio do sistema de Contas a Pagar.
 *
 * Os dados ficam em vetores paralelos: a posição "i" de cada vetor
 * pertence à mesma conta.
 *
 * Erros de validação lançam IllegalArgumentException com mensagem pronta
 * para ser exibida ao usuário (a tela deve pedir a redigitação do dado).
 */
public class ContaService {

    // ---------- constantes ----------

    private static final int CAPACIDADE = 100;

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // ---------- vetores de dados ----------

    private final int[] documentos = new int[CAPACIDADE];
    private final String[] descricoes = new String[CAPACIDADE];
    private final String[] beneficiarios = new String[CAPACIDADE];
    private final double[] valores = new double[CAPACIDADE];       // valor do débito
    private final double[] valoresPagos = new double[CAPACIDADE];  // soma das baixas
    private final LocalDate[] vencimentos = new LocalDate[CAPACIDADE];

    private int quantidade = 0; // quantidade de contas cadastradas

    // =====================================================================
    // CADASTRO
    // =====================================================================

    /** Inclui uma nova conta. Valida todos os campos antes de gravar. */
    public void incluir(int documento, String descricao, String beneficiario,
                        double valor, LocalDate vencimento) {
        if (quantidade >= CAPACIDADE) {
            throw new IllegalStateException("Limite de " + CAPACIDADE + " contas atingido.");
        }
        validarNovoDocumento(documento);
        validarTexto(descricao, "Descrição");
        validarTexto(beneficiario, "Beneficiário");
        validarValor(valor);
        validarVencimento(vencimento);

        documentos[quantidade] = documento;
        descricoes[quantidade] = descricao.trim();
        beneficiarios[quantidade] = beneficiario.trim();
        valores[quantidade] = arredondar(valor);
        valoresPagos[quantidade] = 0;
        vencimentos[quantidade] = vencimento;
        quantidade++;
    }

    /**
     * Altera os dados de uma conta existente.
     * O número do documento não pode ser alterado (exclua e recadastre).
     */
    public void alterar(int documento, String descricao, String beneficiario,
                        double valor, LocalDate vencimento) {
        int indice = localizar(documento);
        validarTexto(descricao, "Descrição");
        validarTexto(beneficiario, "Beneficiário");
        validarValor(valor);
        validarVencimento(vencimento);

        if (arredondar(valor) < valoresPagos[indice]) {
            throw new IllegalArgumentException(
                    "O novo valor não pode ser menor que o já pago ("
                    + formatarValor(valoresPagos[indice]) + ").");
        }

        descricoes[indice] = descricao.trim();
        beneficiarios[indice] = beneficiario.trim();
        valores[indice] = arredondar(valor);
        vencimentos[indice] = vencimento;
    }

    /** Exclui uma conta, deslocando as seguintes para não deixar "buraco" nos vetores. */
    public void excluir(int documento) {
        int indice = localizar(documento);

        for (int i = indice; i < quantidade - 1; i++) {
            documentos[i] = documentos[i + 1];
            descricoes[i] = descricoes[i + 1];
            beneficiarios[i] = beneficiarios[i + 1];
            valores[i] = valores[i + 1];
            valoresPagos[i] = valoresPagos[i + 1];
            vencimentos[i] = vencimentos[i + 1];
        }

        quantidade--;
        descricoes[quantidade] = null;
        beneficiarios[quantidade] = null;
        vencimentos[quantidade] = null;
        documentos[quantidade] = 0;
        valores[quantidade] = 0;
        valoresPagos[quantidade] = 0;
    }

    // =====================================================================
    // BAIXA
    // =====================================================================

    /** Registra um pagamento (parcial ou total) e atualiza o saldo devedor. */
    public void baixar(int documento, double valorPago) {
        int indice = localizar(documento);
        validarValor(valorPago);

        double saldo = getSaldoDevedor(indice);
        if (arredondar(valorPago) > saldo) {
            throw new IllegalArgumentException(
                    "O valor pago não pode ser maior que o saldo devedor ("
                    + formatarValor(saldo) + ").");
        }

        valoresPagos[indice] = arredondar(valoresPagos[indice] + valorPago);
    }

    // =====================================================================
    // CONSULTAS (usadas pelas telas e pelos relatórios)
    // =====================================================================

    /** Retorna a posição do documento nos vetores, ou -1 se não existir. */
    public int buscarIndice(int documento) {
        for (int i = 0; i < quantidade; i++) {
            if (documentos[i] == documento) {
                return i;
            }
        }
        return -1;
    }

    public boolean existe(int documento) {
        return buscarIndice(documento) != -1;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getDocumento(int indice) {
        validarIndice(indice);
        return documentos[indice];
    }

    public String getDescricao(int indice) {
        validarIndice(indice);
        return descricoes[indice];
    }

    public String getBeneficiario(int indice) {
        validarIndice(indice);
        return beneficiarios[indice];
    }

    /** Valor original do débito. */
    public double getValor(int indice) {
        validarIndice(indice);
        return valores[indice];
    }

    public double getValorPago(int indice) {
        validarIndice(indice);
        return valoresPagos[indice];
    }

    /** Saldo devedor = valor do débito - total já pago. */
    public double getSaldoDevedor(int indice) {
        validarIndice(indice);
        return arredondar(valores[indice] - valoresPagos[indice]);
    }

    public LocalDate getVencimento(int indice) {
        validarIndice(indice);
        return vencimentos[indice];
    }

    /** Conta vencida = ainda tem saldo devedor e o vencimento já passou. */
    public boolean estaVencida(int indice) {
        validarIndice(indice);
        return getSaldoDevedor(indice) > 0 && vencimentos[indice].isBefore(LocalDate.now());
    }

    // =====================================================================
    // VALIDAÇÕES (públicas para a tela validar campo a campo)
    // =====================================================================

    /** O documento deve ser positivo e ainda não cadastrado. */
    public void validarNovoDocumento(int documento) {
        if (documento <= 0) {
            throw new IllegalArgumentException("O número do documento deve ser maior que zero.");
        }
        if (existe(documento)) {
            throw new IllegalArgumentException("Documento " + documento + " já cadastrado.");
        }
    }

    public void validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " é obrigatório(a).");
        }
    }

    public void validarValor(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || arredondar(valor) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
    }

    public void validarVencimento(LocalDate vencimento) {
        if (vencimento == null) {
            throw new IllegalArgumentException("Data de vencimento inválida.");
        }
    }

    // =====================================================================
    // CONVERSÕES (texto digitado -> tipo certo, com erro claro)
    // =====================================================================

    public static int converterDocumento(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException("Documento inválido. Digite apenas números.");
        }
    }

    /** Aceita vírgula ou ponto como separador decimal (ex.: 150,75). */
    public static double converterValor(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
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
    public static String formatarValor(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", valor);
    }

    // =====================================================================
    // MÉTODOS INTERNOS
    // =====================================================================

    /** Localiza o documento ou lança erro se não existir. */
    private int localizar(int documento) {
        int indice = buscarIndice(documento);
        if (indice == -1) {
            throw new IllegalArgumentException("Documento " + documento + " não encontrado.");
        }
        return indice;
    }

    private void validarIndice(int indice) {
        if (indice < 0 || indice >= quantidade) {
            throw new IllegalArgumentException("Posição inválida: " + indice);
        }
    }

    /** Mantém os valores com 2 casas decimais. */
    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
