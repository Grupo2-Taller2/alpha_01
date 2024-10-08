package com.ejemplo.carmenuy.tts;

import javax.speech.Central;
import javax.speech.EngineException;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;

/**
 * Clase que gestiona la síntesis de texto a voz (TTS) para el juego Carmen Sandiego Uruguay.
 */
public class TTSManager implements ITTSManager {
    private static final Logger logger = Logger.getLogger(TTSManager.class.getName());
    private Synthesizer synthesizer;
    private final Locale locale;
    private final String voiceName;

    /**
     * Constructor por defecto que inicializa el sintetizador con configuración regional española (Uruguay) y una voz predeterminada.
     */
    public TTSManager() {
        this(new Locale("es", "UY"), "kevin16");
    }

    /**
     * Constructor que permite especificar la configuración regional y la voz del sintetizador.
     *
     * @param locale Configuración regional.
     * @param voiceName Nombre de la voz a utilizar.
     */
    public TTSManager(Locale locale, String voiceName) {
        this.locale = locale;
        this.voiceName = voiceName;
        initializeSynthesizer();
    }

    /**
     * Inicializa el sintetizador de voz.
     */
    private void initializeSynthesizer() {
        try {
            System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
            Central.registerEngineCentral("com.sun.speech.freetts.jsapi.FreeTTSEngineCentral");
            SynthesizerModeDesc desc = new SynthesizerModeDesc(null, "general", locale, null, null);
            synthesizer = Central.createSynthesizer(desc);
            if (synthesizer == null) {
                logger.severe("No se pudo crear el sintetizador.");
                throw new RuntimeException("No se pudo crear el sintetizador");
            }
            synthesizer.allocate();
            synthesizer.resume();
            Voice voice = new Voice(voiceName, Voice.GENDER_NEUTRAL, Voice.AGE_DONT_CARE, null);
            synthesizer.getSynthesizerProperties().setVoice(voice);
        } catch (EngineException e) {
            logger.log(Level.SEVERE, "Error de motor al inicializar el sintetizador", e);
            throw new RuntimeException("Error de motor al inicializar el sintetizador", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error general al inicializar el sintetizador", e);
            throw new RuntimeException("Error general al inicializar el sintetizador", e);
        }
    }

    @Override
    public void speak(String text) {
        try {
            if (synthesizer == null) {
                throw new IllegalStateException("El sintetizador no está inicializado.");
            }
            synthesizer.speakPlainText(text, null);
            synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error al sintetizar el texto", e);
        }
    }

    @Override
    public CompletableFuture<Void> speakAsync(String text) {
        return CompletableFuture.runAsync(() -> speak(text));
    }

    @Override
    public void setVolume(float volume) {
        // La implementación depende de la biblioteca TTS específica
        // Ejemplo: synthesizer.getSynthesizerProperties().setVolume(volume);
    }

    @Override
    public float getVolume() {
        // La implementación depende de la biblioteca TTS específica
        // Ejemplo: return synthesizer.getSynthesizerProperties().getVolume();
        return 1.0f; // Valor predeterminado
    }

    @Override
    public void setSpeed(float speed) {
        // La implementación depende de la biblioteca TTS específica
        // Ejemplo: synthesizer.getSynthesizerProperties().setSpeakingRate(speed);
    }

    @Override
    public void shutdown() {
        try {
            if (synthesizer != null) {
                synthesizer.deallocate();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error al cerrar el sintetizador", e);
        }
    }

    /**
     * Método principal para pruebas y demostración del TTS.
     */
    public static void main(String[] args) {
        TTSManager ttsManager = new TTSManager();
        ttsManager.speak("Bienvenido al juego Carmen Sandiego. Por favor, ingrese su nombre de usuario y contraseña.");
        ttsManager.speak("Elija una pista usando las teclas H, J o K.");

        // Establecer volumen y velocidad para la demostración
        ttsManager.setVolume(0.8f);
        ttsManager.setSpeed(150.0f);

        // Sintetizar texto de forma asíncrona
        ttsManager.speakAsync("Esta es una prueba de síntesis de texto a voz de forma asíncrona.")
                .thenRun(() -> System.out.println("Síntesis de texto a voz completada de forma asíncrona."));

        // Apagar el sintetizador al finalizar
        ttsManager.shutdown();
    }
}
