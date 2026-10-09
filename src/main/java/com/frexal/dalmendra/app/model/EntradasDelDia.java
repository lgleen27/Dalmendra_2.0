package com.frexal.dalmendra.app.model;

/**
 * Modelo para almacenar el desglose de entradas del día.
 */
public class EntradasDelDia {
    private final int compras;
    private final int movimientos;

    public EntradasDelDia(int compras, int movimientos) {
        this.compras = compras;
        this.movimientos = movimientos;
    }

    public int getCompras() {
        return compras;
    }

    public int getMovimientos() {
        return movimientos;
    }

    public int getTotal() {
        return compras + movimientos;
    }
}
