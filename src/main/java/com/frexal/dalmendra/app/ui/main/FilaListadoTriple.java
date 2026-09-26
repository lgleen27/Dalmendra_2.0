package com.frexal.dalmendra.app.ui.main;

import com.frexal.dalmendra.app.model.Existencia;

/**
 * Modelo contenedor para presentar el listado general en formato de 3 columnas paralelas.
 * Cada instancia de esta clase agrupa hasta tres objetos {@link Existencia} en una misma fila visual.
 */
public class FilaListadoTriple {

    /** Primer artículo de la fila (columna izquierda). */
    private final Existencia item1;

    /** Segundo artículo de la fila (columna central), puede ser {@code null}. */
    private final Existencia item2;

    /** Tercer artículo de la fila (columna derecha), puede ser {@code null}. */
    private final Existencia item3;

    public FilaListadoTriple(Existencia item1, Existencia item2, Existencia item3) {
        this.item1 = item1;
        this.item2 = item2;
        this.item3 = item3;
    }

    public Existencia getItem1() {
        return item1;
    }

    public Existencia getItem2() {
        return item2;
    }

    public Existencia getItem3() {
        return item3;
    }
}