package contasapagar.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Conta {

    private String documento;
    private String descricao;
    private String beneficiario;

    private BigDecimal valorOriginal;
    private BigDecimal saldoDevedor;

    private LocalDate vencimento;

    public Conta(String documento, String descricao, String beneficiario, BigDecimal valorOriginal, LocalDate vencimento) {
        this.documento = documento;
        this.descricao = descricao;
        this.beneficiario = beneficiario;
        this.valorOriginal = valorOriginal;
        this.vencimento = vencimento;

        this.saldoDevedor = valorOriginal;
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
}
