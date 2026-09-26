package com.frexal.dalmendra.app.service;

import com.frexal.dalmendra.app.model.Categoria;
import com.frexal.dalmendra.app.repository.CategoriaRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión y validación de categorías.
 * Aplica reglas de negocio antes de persistir o modificar registros en base de datos.
 */
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Retorna todas las categorías existentes.
     *
     * @return Lista de categorías.
     * @throws SQLException Si ocurre un error al consultar.
     */
    public List<Categoria> findAll() throws SQLException {
        return categoriaRepository.findAll();
    }

    public Categoria save(Categoria categoria) throws SQLException {
        validar(categoria);
        return categoriaRepository.save(categoria);
    }

    public void deleteById(Long id) throws SQLException {
        if (id != null) {
            categoriaRepository.deleteById(id);
        }
    }

    public void updateOrden(Long id, Integer orden) throws SQLException {
        if (id != null && orden != null) {
            categoriaRepository.updateOrden(id, orden);
        }
    }

    private void validar(Categoria categoria) {
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
        if (categoria.getDescripcion() == null || categoria.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria.");
        }
        if (categoria.getPalabraClave() == null || categoria.getPalabraClave().trim().isEmpty()) {
            throw new IllegalArgumentException("La palabra clave es obligatoria.");
        }
        if (categoria.getEstado() == null) {
            throw new IllegalArgumentException("El estado es obligatorio.");
        }
        if (categoria.getOrden() == null) {
            categoria.setOrden(0);
        }
    }
}