package contasapagar.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

/*
 * Representa uma conta a pagar.
 * Informações da conta: número do documento, descrição, beneficiário, valor do
 * débito e data de vencimento. O saldo devedor começa igual ao valor original e
 * diminui a cada baixa (pagamento parcial ou total).
 */
public class Conta {

    private String documento;
    private String descricao;
    private String beneficiario;

    private BigDecimal valorOriginal;
    private BigDecimal saldoDevedor;

    private LocalDate vencimento;

    public Conta(String documento, String descricao, String beneficiario, BigDecimal valorOriginal, LocalDate vencimento) {

        validarValor(valorOriginal);
        this.documento = validarDocumento(documento);
        this.descricao = validarTexto(descricao, "Descrição");
        this.beneficiario = validarTexto(beneficiario, "Beneficiário");
        this.vencimento = validarVencimento(vencimento);

        this.valorOriginal = valorOriginal.setScale(2, RoundingMode.HALF_UP);
        this.saldoDevedor = this.valorOriginal;
    }

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

    // ---------- controle de pagamentos (baixas) ----------

    /** Total já pago da conta (valor original - saldo devedor). */
    public BigDecimal getValorPago() {
        return valorOriginal.subtract(saldoDevedor);
    }

    /** Registra um pagamento (parcial ou total) e atualiza o saldo devedor. */
    public void registrarPagamento(BigDecimal valor) {
        validarValor(valor);
        BigDecimal pago = valor.setScale(2, RoundingMode.HALF_UP);

        if (pago.compareTo(saldoDevedor) > 0) {
            throw new IllegalArgumentException("O valor pago não pode ser maior que o saldo devedor.");
        }
        saldoDevedor = saldoDevedor.subtract(pago);
    }

    // ---------- validações ----------

    private static String validarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            throw new IllegalArgumentException("O número do documento é obrigatório.");
        }
        String doc = documento.trim();
        if (!doc.matches("\\d+")) {
            throw new IllegalArgumentException("Documento inválido. Digite apenas números.");
        }
        if (doc.isEmpty()) {
            throw new IllegalArgumentException("O número do documento deve ser maior que zero.");
        }
        if (doc.length() > TAMANHO_MAXIMO_DOCUMENTO) {
            throw new IllegalArgumentException(
                    "O documento deve ter no máximo " + TAMANHO_MAXIMO_DOCUMENTO + " dígitos.");
        }
        return doc;
    }

    /** Texto obrigatório, com até 30 caracteres. Devolve o texto sem espaços sobrando. */
    private static String validarTexto(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " é obrigatório(a).");
        }
        String limpo = texto.trim();
        if (limpo.length() > TAMANHO_MAXIMO_TEXTO) {
            throw new IllegalArgumentException(
                    campo + " deve ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
        return limpo;
    }

    /** Maior que zero, no máximo 2 casas decimais e dentro do limite máximo. */
    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor deve ter no máximo 2 casas decimais.");
        }
        if (valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor máximo permitido é R$ "
                    + String.format(Locale.forLanguageTag("pt-BR"), "%,.2f", VALOR_MAXIMO) + ".");
        }
    }

    /** Data informada e com ano entre 2000 e 2100. */
    private static LocalDate validarVencimento(LocalDate vencimento) {
        if (vencimento == null) {
            throw new IllegalArgumentException("A data de vencimento é obrigatória.");
        }
        if (vencimento.getYear() < ANO_MINIMO || vencimento.getYear() > ANO_MAXIMO) {
            throw new IllegalArgumentException(
                    "O ano do vencimento deve estar entre " + ANO_MINIMO + " e " + ANO_MAXIMO + ".");
        }
        return vencimento;
    }
}
