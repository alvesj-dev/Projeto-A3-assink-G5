package contasapagar.view;

import contasapagar.model.Conta;
import contasapagar.service.ContaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.swing.JOptionPane;

public class Menu {

    private static final String TITULO = "Sistema de Contas a Pagar";
    private final ContaService contas = new ContaService();

    public void executar() {
        String opcao;
        do {
            opcao = lerOpcao("""
                    XYZ COMÉRCIO DE PRODUTOS LTDA.
                    MENU PRINCIPAL

                    1 - Cadastro de contas
                    0 - Finalizar
                    """);
            switch (opcao) {
                case "1" -> menuCadastro();
                case "0" -> { }
                default -> mostrarErro("Opção inválida.");
            }
        } while (!opcao.equals("0"));
    }

    private void menuCadastro() {
        String opcao;
        do {
            opcao = lerOpcao("""
                    CADASTRO DE CONTAS

                    1 - Inclusão
                    2 - Alteração
                    3 - Consulta
                    4 - Exclusão
                    0 - Retornar
                    """);
            switch (opcao) {
                case "1" -> repetir("Nova inclusão?", this::incluirConta);
                case "2" -> repetir("Nova alteração?", this::alterarConta);
                case "3" -> repetir("Nova consulta?", this::consultarConta);
                case "4" -> repetir("Nova exclusão?", this::excluirConta);
                case "0" -> { }
                default -> mostrarErro("Opção inválida.");
            }
        } while (!opcao.equals("0"));
    }

    private boolean incluirConta() {
        String titulo = "Inclusão de conta";
        String documento = lerCampo(titulo, "Número do documento:", texto -> {
            contas.validarNovoDocumento(texto);
            return texto;
        });
        if (documento == null) {
            return false;
        }

        DadosConta dados = lerDadosConta(titulo, null);
        if (dados == null) {
            return false;
        }

        Conta candidata = new Conta(documento, dados.descricao(), dados.beneficiario(),
                dados.valor(), dados.vencimento());
        if (confirmar(titulo, exibirDados(candidata) + "\n\nConfirma inclusão?")) {
            contas.incluir(documento, dados.descricao(), dados.beneficiario(),
                    dados.valor(), dados.vencimento());
            mostrarMensagem(titulo, "Conta incluída.");
        }
        return true;
    }

    private DadosConta lerDadosConta(String titulo, Conta atual) {
        String descricao = lerTexto(titulo, "Descrição", atual == null ? "" : atual.getDescricao());
        if (descricao == null) {
            return null;
        }
        String beneficiario = lerTexto(titulo, "Beneficiário", atual == null ? "" : atual.getBeneficiario());
        if (beneficiario == null) {
            return null;
        }
        String valorAtual = atual == null ? "" : ContaService.formatarValor(atual.getValorOriginal());
        BigDecimal valor = lerCampo(titulo, "Valor do débito:", valorAtual, texto -> {
            BigDecimal valorDigitado = converterValorValidado(texto);
            if (atual != null && valorDigitado.compareTo(atual.getValorPago()) < 0) {
                throw new IllegalArgumentException("O novo valor não pode ser menor que o valor já pago ("
                        + moeda(atual.getValorPago()) + ").");
            }
            return valorDigitado;
        });
        if (valor == null) {
            return null;
        }
        String vencimentoAtual = atual == null ? "" : ContaService.formatarData(atual.getVencimento());
        LocalDate vencimento = lerData(titulo, "Vencimento (dd/mm/aaaa):", vencimentoAtual);
        return vencimento == null ? null : new DadosConta(descricao, beneficiario, valor, vencimento);
    }

    private record DadosConta(String descricao, String beneficiario, BigDecimal valor, LocalDate vencimento) {
    }

    private String lerTexto(String titulo, String campo, String inicial) {
        return lerCampo(titulo, campo + ":", inicial, texto -> {
            contas.validarTexto(texto, campo);
            return texto;
        });
    }

    private BigDecimal converterValorValidado(String texto) {
        BigDecimal valor = ContaService.converterValor(texto);
        contas.validarValor(valor);
        return valor;
    }

    private LocalDate lerData(String titulo, String pergunta, String inicial) {
        return lerCampo(titulo, pergunta, inicial, texto -> {
            LocalDate data = ContaService.converterData(texto);
            contas.validarVencimento(data);
            return data;
        });
    }

    private String exibirDados(Conta conta) {
        return "Documento: " + conta.getDocumento()
                + "\nDescrição: " + conta.getDescricao()
                + "\nBeneficiário: " + conta.getBeneficiario()
                + "\nValor do débito: " + moeda(conta.getValorOriginal())
                + "\nVencimento: " + ContaService.formatarData(conta.getVencimento())
                + "\nSaldo devedor: " + moeda(conta.getSaldoDevedor());
    }

    private String moeda(BigDecimal valor) {
        return "R$ " + ContaService.formatarValor(valor);
    }

    private void repetir(String pergunta, BooleanSupplier operacao) {
        do {
            try {
                if (!operacao.getAsBoolean()) {
                    return;
                }
            } catch (IllegalArgumentException | IllegalStateException erro) {
                mostrarErro(erro.getMessage());
            }
        } while (confirmar(TITULO, pergunta));
    }

    private <T> T lerCampo(String titulo, String pergunta, Function<String, T> validar) {
        return lerCampo(titulo, pergunta, "", validar);
    }

    private <T> T lerCampo(String titulo, String pergunta, String inicial, Function<String, T> validar) {
        String texto = inicial;
        while (true) {
            texto = (String) JOptionPane.showInputDialog(null, pergunta, titulo,
                    JOptionPane.QUESTION_MESSAGE, null, null, texto);
            if (texto == null) {
                return null;
            }
            try {
                return validar.apply(texto);
            } catch (IllegalArgumentException erro) {
                mostrarErro(erro.getMessage());
            }
        }
    }

    private boolean confirmar(String titulo, String mensagem) {
        String[] opcoes = {"Sim", "Não"};
        return JOptionPane.showOptionDialog(null, mensagem, titulo, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[1]) == JOptionPane.YES_OPTION;
    }

    private void mostrarMensagem(String titulo, String mensagem) {
        JOptionPane.showMessageDialog(null, mensagem, titulo, JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean consultarConta() {
        String titulo = "Consulta de conta";
        Conta conta = lerConta(titulo);
        if (conta == null) {
            return false;
        }
        mostrarMensagem(titulo, exibirDados(conta));
        return true;
    }

    private Conta lerConta(String titulo) {
        return lerCampo(titulo, "Número do documento:", contas::consultar);
    }

    private boolean alterarConta() {
        String titulo = "Alteração de conta";
        Conta conta = lerConta(titulo);
        if (conta == null) {
            return false;
        }
        mostrarMensagem(titulo, exibirDados(conta));
        DadosConta dados = lerDadosConta(titulo, conta);
        if (dados == null) {
            return false;
        }

        Conta previa = new Conta(conta.getDocumento(), dados.descricao(), dados.beneficiario(),
                dados.valor(), dados.vencimento());
        if (conta.getValorPago().signum() > 0) {
            previa.registrarPagamento(conta.getValorPago());
        }
        if (confirmar(titulo, exibirDados(previa) + "\n\nConfirma alteração?")) {
            contas.alterar(conta.getDocumento(), dados.descricao(), dados.beneficiario(),
                    dados.valor(), dados.vencimento());
            mostrarMensagem(titulo, "Conta alterada.");
        }
        return true;
    }

    private boolean excluirConta() {
        String titulo = "Exclusão de conta";
        Conta conta = lerConta(titulo);
        if (conta == null) {
            return false;
        }
        if (confirmar(titulo, exibirDados(conta) + "\n\nConfirma exclusão?")) {
            contas.excluir(conta.getDocumento());
            mostrarMensagem(titulo, "Conta excluída.");
        }
        return true;
    }

    private String lerOpcao(String menu) {
        String entrada = JOptionPane.showInputDialog(null, menu + "\nEscolha uma opção:",
                TITULO, JOptionPane.QUESTION_MESSAGE);
        return entrada == null ? "0" : entrada.trim();
    }

    private void mostrarErro(String mensagem) {
        JOptionPane.showMessageDialog(null, mensagem, TITULO, JOptionPane.ERROR_MESSAGE);
    }
}
