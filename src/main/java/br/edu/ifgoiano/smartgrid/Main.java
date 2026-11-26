package br.edu.ifgoiano.smartgrid;

import br.edu.ifgoiano.smartgrid.controller.PostoRecargaController;
import br.edu.ifgoiano.smartgrid.dao.InicializadorBanco;
import br.edu.ifgoiano.smartgrid.dao.PostoRecargaDAOJDBC;
import br.edu.ifgoiano.smartgrid.service.MobilidadeService;
import br.edu.ifgoiano.smartgrid.view.MenuConsole;

/** Ponto de entrada: cria o banco, monta as camadas e abre o menu. */
public class Main {

    public static void main(String[] args) {
        InicializadorBanco.criarTabelas();
        MobilidadeService service = new MobilidadeService(new PostoRecargaDAOJDBC());
        PostoRecargaController controller = new PostoRecargaController(service);

        // Dados iniciais para demonstração
        controller.cadastrarEstacao("AA:BB:CC:DD:EE:01", "Av. Central, 100", 220, 44, false);
        controller.cadastrarEstacao("AA:BB:CC:DD:EE:02", "Rod. GO-060, km 5", 380, 120, true);

        new MenuConsole(controller).iniciar();
    }
}