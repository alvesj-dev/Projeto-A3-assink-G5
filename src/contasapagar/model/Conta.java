
package contasapagar.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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
     * Verifica se o documento foi informado e contém apenas números.
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
    }

    /**
     * Verifica se a data de vencimento foi informada.
     */
    private static LocalDate validarVencimento(LocalDate vencimento) {
        if (vencimento == null) {
            throw new IllegalArgumentException(
                    "A data de vencimento é obrigatória.");
        }

        return vencimento;
    }
}
