package contasapagar.view;

import javax.swing.JOptionPane;

public class Menu {

    private static final String TITULO = "Sistema de Contas a Pagar";

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

                    0 - Retornar
                    """);
            switch (opcao) {
                case "0" -> { }
                default -> mostrarErro("Opção inválida.");
            }
        } while (!opcao.equals("0"));
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
