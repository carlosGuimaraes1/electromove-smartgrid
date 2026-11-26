package br.edu.ifgoiano.smartgrid.dao;

/** Erro de acesso a dados; encapsula SQLException para que o JDBC não vaze para as outras camadas. */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DAOException(String mensagem) {
        super(mensagem);
    }

    public DAOException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
