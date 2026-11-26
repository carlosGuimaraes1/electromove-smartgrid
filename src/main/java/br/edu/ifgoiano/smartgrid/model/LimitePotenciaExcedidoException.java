package br.edu.ifgoiano.smartgrid.model;

/** Lançada quando a potência solicitada excede o limite da tomada ou da estação. */
public class LimitePotenciaExcedidoException extends Exception{
    private static final long serialVersionUID = 1L;

    public LimitePotenciaExcedidoException(String message) {
        super(message);
    }
}
