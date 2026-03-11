package com.aluracursos.literalura.principal;

import com.aluracursos.literalura.model.Autor;
import com.aluracursos.literalura.model.Datos;
import com.aluracursos.literalura.model.DatosLibro;
import com.aluracursos.literalura.model.Libro;
import com.aluracursos.literalura.repository.AutorRepository;
import com.aluracursos.literalura.repository.LibroRepository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Principal {
    private final Scanner lectura = new Scanner(System.in);
    private final ConsumoAPI consumoApi = new ConsumoAPI();
    private final ConvierteDatos conversor = new ConvierteDatos();
    private final LibroRepository repository;
    private final AutorRepository autorRepository;

    public Principal(LibroRepository repository, AutorRepository autorRepository) {
        this.repository = repository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar libro por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    6 - Generar estadísticas de descargas
                    7 - Top 10 libros más descargados
                    8 - Buscar autor por nombre
                    9 - Buscar autores por año de nacimiento
                    
                    0 - Salir
                    """;
            System.out.println(menu);

            try {
                opcion = Integer.parseInt(lectura.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Opción no válida");
                continue;
            }

            switch (opcion) {
                case 1 -> buscarLibroPorTitulo();
                case 2 -> listarLibrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarAutoresVivosPorFecha();
                case 5 -> listarLibrosPorIdioma();
                case 6 -> mostrarEstadisticasDeDescargas();
                case 7 -> listarTop10Libros();
                case 8 -> buscarAutorPorNombre();
                case 9 -> buscarAutoresPorFechaNacimiento();
                case 0 -> System.out.println("Cerrando la aplicación...");
                default -> System.out.println("Opción no válida");
            }
        }
    }

    private Datos getDatosLibro() {
        System.out.println("Ingrese el nombre del libro que desea buscar:");
        var nombreLibro = lectura.nextLine();
        String URL_BASE = "https://gutendex.com/books/";
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        return conversor.obtenerDatos(json, Datos.class);
    }

    private void buscarLibroPorTitulo() {
        Datos datos = getDatosLibro();
        if (!datos.resultados().isEmpty()) {
            DatosLibro primerLibro = datos.resultados().get(0);

            Optional<Libro> libroExistente = repository.findByTituloContainsIgnoreCase(primerLibro.titulo());

            if (libroExistente.isPresent()) {
                System.out.println("\n[INFO] El libro ya forma parte de tu colección local:");
                System.out.println(libroExistente.get());
                System.out.println("--------------------------------------------------\n");
            } else {
                var datosAutor = primerLibro.autor().get(0);
                Autor autor = autorRepository.findByNombreIgnoreCase(datosAutor.nombre())
                        .orElseGet(() -> {
                            Autor nuevoAutor = new Autor(datosAutor);
                            return autorRepository.save(nuevoAutor);
                        });
                Libro libro = new Libro(primerLibro, autor);
                repository.save(libro);
                System.out.println(libro);
            }
        } else {
            System.out.println("Libro no encontrado");
        }
    }

    private void listarLibrosRegistrados() {
        List<Libro> libros = repository.findAll();
        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados.");
        } else {
            libros.forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        List<Autor> autores = autorRepository.findAll();
        autores.forEach(System.out::println);
    }

    private void listarAutoresVivosPorFecha() {
        System.out.println("Ingrese el año que desea consultar:");
        try {
            var fecha = Integer.parseInt(lectura.nextLine());

            if (fecha < 0 || fecha > 2025) {
                System.out.println("Por favor, ingrese un año válido (entre 0 y 2025).");
                return;
            }

            List<Autor> autoresVivos = autorRepository.buscarAutoresVivosEnDeterminadaFecha(fecha);

            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos en ese año.");
            } else {
                System.out.println("---------- AUTORES VIVOS EN " + fecha + " ----------");
                autoresVivos.forEach(System.out::println);
            }
        } catch (NumberFormatException e) {
            System.out.println("Error: Debe ingresar un número entero para el año.");
        }
    }

    private void listarLibrosPorIdioma() {
        System.out.println("""
                Ingrese el idioma para buscar los libros:
                es - español
                en - inglés
                fr - francés
                pt - portugués
                ru - ruso
                """);
        var idioma = lectura.nextLine();

        Long cantidadLibros = repository.countByIdioma(idioma);

        List<Libro> librosPorIdioma = repository.findByIdioma(idioma);

        if (librosPorIdioma.isEmpty()) {
            System.out.println("No se encontraron libros en ese idioma en la base de datos.");
        } else {
            System.out.println("---------- ESTADÍSTICA ----------");
            System.out.println("Cantidad de libros en (" + idioma + "): " + cantidadLibros);
            System.out.println("---------------------------------");

            librosPorIdioma.forEach(System.out::println);
        }
    }

    private void mostrarEstadisticasDeDescargas() {
        List<Libro> todosLosLibros = repository.findAll();
        DoubleSummaryStatistics est = todosLosLibros.stream()
                .filter(l -> l.getNumeroDeDescargas() > 0)
                .mapToDouble(Libro::getNumeroDeDescargas)
                .summaryStatistics();

        System.out.println("---------- ESTADÍSTICAS DE DESCARGAS ----------");
        System.out.println("Media de descargas: " + est.getAverage());
        System.out.println("Máxima cantidad de descargas: " + est.getMax());
        System.out.println("Mínima cantidad de descargas: " + est.getMin());
        System.out.println("Cantidad de libros evaluados: " + est.getCount());
        System.out.println("-----------------------------------------------");
    }

    private void buscarAutorPorNombre() {
        System.out.println("Ingrese el nombre del autor que desea buscar:");
        var nombre = lectura.nextLine();
        Optional<Autor> autor = autorRepository.findByNombreContainsIgnoreCase(nombre);
        if (autor.isPresent()) {
            System.out.println(autor.get());
        } else {
            System.out.println("Autor no encontrado en la base de datos.");
        }
    }

    private void listarTop10Libros() {
        List<Libro> topLibros = repository.findTop10ByOrderByNumeroDeDescargasDesc();
        System.out.println("---------- TOP 10 LIBROS MÁS DESCARGADOS ----------");
        topLibros.forEach(l ->
                System.out.println("Libro: " + l.getTitulo() + " | Descargas: " + l.getNumeroDeDescargas()));
    }

    private void buscarAutoresPorFechaNacimiento() {
        System.out.println("Ingrese el año de nacimiento para buscar autores:");
        try {
            var fecha = Integer.parseInt(lectura.nextLine());
            List<Autor> autores = autorRepository.findByFechaDeNacimiento(fecha);
            if (autores.isEmpty()) {
                System.out.println("No se encontraron autores que nacieran en el año " + fecha);
            } else {
                autores.forEach(System.out::println);
            }
        } catch (NumberFormatException e) {
            System.out.println("Fecha inválida.");
        }
    }
}