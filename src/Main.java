import contasapagar.view.Menu;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Menu().executar());
    }
}
