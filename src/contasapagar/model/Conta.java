
package contasapagar.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Representa uma conta a pagar.
 * Informações da conta: número do documento, descrição, beneficiário, valor do
 * débito e data de vencimento. O saldo devedor começa igual ao valor original e
 * diminui a cada baixa (pagamento parcial ou total).
 */
public class Conta {

    // Limites do sistema (o ContaService usa as mesmas constantes)
    public static final int TAMANHO_MAXIMO_DOCUMENTO = 9;  // dígitos
    public static final int TAMANHO_MAXIMO_TEXTO = 30;     // descrição e beneficiário
    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("999999999.99");
    public static final int ANO_MINIMO = 2000;             // ano do vencimento
    public static final int ANO_MAXIMO = 2100;

    private String documento;
    private String descricao;
    private String beneficiario;

    private BigDecimal valorOriginal;
    private BigDecimal saldoDevedor;

    private LocalDate vencimento;

    public Conta(String documento, String descricao, String beneficiario,
            BigDecimal valorOriginal, LocalDate vencimento) {

        validarValor(valorOriginal);
        this.documento = validarDocumento(documento);
        this.descricao = validarTexto(descricao, "Descrição");
        this.beneficiario = validarTexto(beneficiario, "Beneficiário");
        this.vencimento = validarVencimento(vencimento);

        this.valorOriginal = valorOriginal.setScale(2, RoundingMode.HALF_UP);
        this.saldoDevedor = this.valorOriginal;
    }

    // ---------- getters ----------

    public String getDocumento() {
        return documento;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getBeneficiario() {
        return beneficiario;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public BigDecimal getSaldoDevedor() {
        return saldoDevedor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    // ---------- alterações de dados ----------

    public void alterarDescricao(String novaDescricao) {
        this.descricao = validarTexto(novaDescricao, "Descrição");
    }

    public void alterarBeneficiario(String novoBeneficiario) {
        this.beneficiario = validarTexto(novoBeneficiario, "Beneficiário");
    }

    // ---------- controle de pagamentos (baixas) ----------

    /**
     * Total já pago da conta (valor original - saldo devedor).
     */
    public BigDecimal getValorPago() {
        return valorOriginal.subtract(saldoDevedor);
    }

    /**
     * Registra um pagamento parcial ou total e atualiza o saldo devedor.
     */
    public void registrarPagamento(BigDecimal valor) {
        validarValor(valor);

        BigDecimal pago = valor.setScale(2, RoundingMode.HALF_UP);

        if (pago.compareTo(saldoDevedor) > 0) {
            throw new IllegalArgumentException(
                    "O valor pago não pode ser maior que o saldo devedor.");
        }

        saldoDevedor = saldoDevedor.subtract(pago);
    }

    // ---------- validações ----------

    /**
     * Verifica o documento (só números, maior que zero, até 9 dígitos) e
     * devolve sem espaços e sem zeros à esquerda ("001" vira "1").
     */
    private static String validarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O número do documento é obrigatório.");
        }

        String doc = documento.trim();

        if (!doc.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Documento inválido. Digite apenas números.");
        }

        doc = doc.replaceFirst("^0+", "");

        if (doc.isEmpty()) {
            throw new IllegalArgumentException(
                    "O número do documento deve ser maior que zero.");
        }

        if (doc.length() > TAMANHO_MAXIMO_DOCUMENTO) {
            throw new IllegalArgumentException(
                    "O documento deve ter no máximo " + TAMANHO_MAXIMO_DOCUMENTO + " dígitos.");
        }

        return doc;
    }

    /**
     * Verifica se o texto foi informado e remove espaços nas extremidades.
     */
    private static String validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    campo + " é obrigatório(a).");
        }

        if (texto.trim().length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    campo + " deve ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }

        return texto.trim();
    }

    /**
     * Verifica se o valor é positivo e possui no máximo duas casas decimais.
     */
    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException(
                    "O valor deve ser maior que zero.");
        }

        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "O valor deve ter no máximo 2 casas decimais.");
        }

        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "O valor máximo permitido é R$ 999.999.999,99.");
        }
    }

    /**
     * Verifica se a data de vencimento foi informada e se o ano é válido.
     */
    private static LocalDate validarVencimento(LocalDate vencimento) {
        if (vencimento == null) {
            throw new IllegalArgumentException(
                    "A data de vencimento é obrigatória.");
        }

        if (vencimento.getYear() < ANO_MINIMO || vencimento.getYear() > ANO_MAXIMO) {
            throw new IllegalArgumentException(
                    "O ano do vencimento deve estar entre "
                    + ANO_MINIMO + " e " + ANO_MAXIMO + ".");
        }

        return vencimento;
    }
}
