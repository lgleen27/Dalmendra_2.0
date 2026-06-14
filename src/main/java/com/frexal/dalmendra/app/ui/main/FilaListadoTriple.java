package com.frexal.dalmendra.app.ui.main;

import com.frexal.dalmendra.app.model.Existencia;

public class FilaListadoTriple {

    private final Existencia item1;
    private final Existencia item2;
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