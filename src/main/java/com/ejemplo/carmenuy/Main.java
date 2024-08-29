package com.ejemplo.carmenuy;

import com.ejemplo.carmenuy.ui.VentanaJuego;
import com.ejemplo.carmenuy.service.JuegoService;
import com.ejemplo.carmenuy.dao.PistaDAO;
import com.ejemplo.carmenuy.dao.LocalidadDAO;
import com.ejemplo.carmenuy.dao.SecuazDAO;
import com.ejemplo.carmenuy.model.Usuario;
import com.ejemplo.carmenuy.model.Localidad;
import com.ejemplo.carmenuy.model.Secuaz;
import com.ejemplo.carmenuy.model.Pista;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static SecuazDAO secuazDAO;
    private static PistaDAO pistaDAO;
    private static Secuaz secuazActual;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Connection connection = null;
            try {
                String dbPath = "jdbc:sqlite:src/main/resources/carmen_sandiego.db";
                connection = DriverManager.getConnection(dbPath);
                connection.setAutoCommit(false);  // Desactivar auto-commit para manejar transacciones manualmente

                VentanaJuego ventana = getVentanaJuego(connection);
                ventana.setVisible(true);

                connection.commit();  // Commit de todas las operaciones realizadas
            } catch (SQLException e) {
                try {
                    if (connection != null) {
                        connection.rollback();  // Rollback en caso de error
                    }
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Error al hacer rollback", ex);
                }
                LOGGER.log(Level.SEVERE, "Error al iniciar el juego", e);
                JOptionPane.showMessageDialog(null, "Error al conectar con la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(true);  // Restaurar auto-commit
                        connection.close();
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error al cerrar la conexión a la base de datos", e);
                }
            }
        });
    }

    private static VentanaJuego getVentanaJuego(Connection connection) throws SQLException {
        PistaDAO pistaDAO = new PistaDAO(connection);
        LocalidadDAO localidadDAO = new LocalidadDAO(connection);
        SecuazDAO secuazDAO = new SecuazDAO(connection, localidadDAO);

        pistaDAO.crearTabla();
        localidadDAO.crearTabla();
        secuazDAO.crearTabla();

        pistaDAO.insertarPistasIniciales();
        localidadDAO.insertarLocalidadesIniciales();

        List<Localidad> localidades = localidadDAO.obtenerTodasLasLocalidades();
        List<Secuaz> secuaces = secuazDAO.obtenerTodosLosSecuaces();

        if (secuaces.isEmpty()) {
            Localidad localidadInicial = localidades.stream()
                    .filter(localidad -> "AN".equals(localidad.getNombre()))
                    .findFirst()
                    .orElseThrow(() -> new SQLException("No se encontró la localidad 'AN' para asignar al secuaz."));
            Secuaz secuazInicial = new Secuaz("Secuaz Inicial", "Habilidad Inicial", 5, localidadInicial);
            secuazDAO.insertarSecuaz(secuazInicial);
        } else {
            for (Secuaz secuaz : secuaces) {
                if (secuaz.getLocalidad() == null) {
                    Localidad localidadAN = localidades.stream()
                            .filter(localidad -> "AN".equals(localidad.getNombre()))
                            .findFirst()
                            .orElseThrow(() -> new SQLException("No se encontró la localidad 'AN' para asignar al secuaz."));
                    secuaz.setLocalidad(localidadAN);
                    secuazDAO.actualizarSecuaz(secuaz);
                }
            }
        }

        JuegoService juegoService = new JuegoService(pistaDAO, localidadDAO, secuazDAO);
        Usuario usuario = new Usuario("Detective", "Apellido", "contrasena", "Detective Junior", 0, "");

        return new VentanaJuego(juegoService, usuario);
    }

    private static void seleccionarNuevoSecuaz() {
        try {
            List<Secuaz> secuacesNoCapturados = secuazDAO.obtenerSecuacesNoCapturados();
            if (!secuacesNoCapturados.isEmpty()) {
                secuazActual = secuacesNoCapturados.get(0); // Selecciona el primer secuaz no capturado
                System.out.println("Nuevo secuaz seleccionado: " + secuazActual.getNombre());
                actualizarInterfaz();
            } else {
                System.out.println("No hay más secuaces no capturados.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void capturarSecuazActual() {
        try {
            secuazDAO.capturarSecuaz(secuazActual.getId());
            System.out.println("Secuaz capturado: " + secuazActual.getNombre());
            seleccionarNuevoSecuaz();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para manejar la selección de pistas
    private static void manejarSeleccionDePista(char opcion) {
        try {
            List<Pista> pistas = pistaDAO.obtenerPistasPorLocalidad(secuazActual.getLocalidad().getId());
            Pista pistaSeleccionada = null;
            switch (opcion) {
                case 'H':
                    pistaSeleccionada = pistas.get(0);
                    break;
                case 'J':
                    pistaSeleccionada = pistas.get(1);
                    break;
                case 'K':
                    pistaSeleccionada = pistas.get(2);
                    break;
                default:
                    System.out.println("Opción no válida.");
                    return;
            }
            System.out.println("Pista seleccionada: " + pistaSeleccionada.getDescripcion());
            if (pistaSeleccionada.esCorrecta()) {
                capturarSecuazActual();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método para actualizar la interfaz de usuario
    private static void actualizarInterfaz() {
        System.out.println("Secuaz actual: " + secuazActual.getNombre());
        System.out.println("Localidad: " + secuazActual.getLocalidad().getNombre());
        System.out.println("Elige tu pista con las teclas H, J o K.");
    }
}
