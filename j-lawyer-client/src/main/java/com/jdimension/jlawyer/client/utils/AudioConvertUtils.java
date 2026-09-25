/*
 * Copyright (C) 2026 Jens Kutschke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.jdimension.jlawyer.client.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ServiceLoader;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import org.apache.log4j.Logger;

/**
 * Dekodiert MP3/OGG-Audiodaten in PCM-WAV. Nutzt die auf dem Classpath
 * registrierten SPIs für javax.sound.sampled (mp3spi + jlayer,
 * vorbisspi + jorbis, tritonus-all für Resampling/Downmix).
 */
public class AudioConvertUtils {

    private static final Logger log = Logger.getLogger(AudioConvertUtils.class);

    private AudioConvertUtils() {
    }

    /**
     * Dekodiert Audiobytes in das kanonische "Weiterverarbeitungs"-Format
     * (16 kHz mono, 16 bit PCM signed, little endian) und liefert einen
     * WAV-Container zurück. Format entspricht {@link AudioUtils#getAudioFormat()}.
     * Multi-Channel-Quellen werden downgemischt, Sample-Rates auf 16 kHz
     * konvertiert. Genau das Format nutzen auch AddVoiceMemoDialog, die
     * Assistant-Transkription und alle Aufnahme-Pfade.
     *
     * Dieselbe Ausgabe wird sowohl für die Wiedergabe im
     * ReadOnlySoundplayerPanel als auch für "In WAV konvertieren" verwendet
     * — der Player dekodiert also einmal und teilt sich das Ergebnis mit
     * dem späteren Speichern. Das hält den Speicherverbrauch im Rahmen:
     * eine Stunde Audio erzeugt hier ~115 MB statt (je nach Quelle) über
     * 600 MB, was den Client bei mittelgroßen MP3/OGG-Aufnahmen sonst
     * durchs GC-Thrashing schickte.
     */
    public static byte[] decodeToStandardWav(byte[] input) throws IOException, UnsupportedAudioFileException {
        AudioFormat targetFmt = AudioUtils.getAudioFormat();
        try (AudioInputStream src = openEncodedStream(input)) {
            AudioFormat srcFmt = src.getFormat();
            AudioFormat pcmSrcFmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFmt.getSampleRate(),
                    16,
                    srcFmt.getChannels(),
                    srcFmt.getChannels() * 2,
                    srcFmt.getSampleRate(),
                    false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmSrcFmt, src);
                 AudioInputStream target = AudioSystem.getAudioInputStream(targetFmt, pcm)) {
                return writeToWavWithKnownLength(target, targetFmt);
            }
        }
    }

    /**
     * Öffnet Audiobytes über die auf dem Classpath registrierten
     * {@link AudioFileReader}-SPIs.
     *
     * Wir umgehen bewusst {@link AudioSystem#getAudioInputStream(java.io.InputStream)}:
     * der in tritonus-all mitgelieferte AiffAudioFileReader wirft bei
     * Nicht-AIFF-Streams eine {@link java.io.EOFException} statt einer
     * {@link UnsupportedAudioFileException}. AudioSystem fängt nur letztere
     * ab und probiert dann den nächsten Provider — die IOException
     * propagiert hingegen und bricht die Suche ab, bevor vorbisspi bei
     * einer OGG überhaupt zum Zug kommt. Deshalb iterieren wir hier
     * selbst und behandeln jede Fehlerart als "dieser Reader passt nicht,
     * weiter".
     */
    private static AudioInputStream openEncodedStream(byte[] audioBytes) throws UnsupportedAudioFileException, IOException {
        UnsupportedAudioFileException lastUAFE = null;
        for (AudioFileReader reader : ServiceLoader.load(AudioFileReader.class)) {
            try {
                return reader.getAudioInputStream(new ByteArrayInputStream(audioBytes));
            } catch (UnsupportedAudioFileException uafe) {
                lastUAFE = uafe;
            } catch (IOException | RuntimeException | LinkageError ex) {
                if (log.isDebugEnabled()) {
                    log.debug("AudioFileReader " + reader.getClass().getName() + " rejected stream: " + ex);
                }
            }
        }
        // Fallback für die JDK-eigenen Reader (WAV/AU/AIFF), die nicht per
        // ServiceLoader publiziert sind — falls hier doch mal eine WAV
        // hereingereicht wird.
        try {
            return AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBytes));
        } catch (UnsupportedAudioFileException uafe) {
            throw (lastUAFE != null) ? lastUAFE : uafe;
        }
    }

    /**
     * Puffert einen PCM-Stream vollständig in den Speicher und schreibt das
     * Ergebnis als WAV mit korrekt gesetzten Chunk-Längen zurück. Notwendig,
     * weil AudioSystem.write(stream, WAVE, outputStream) bei Quellen mit
     * getFrameLength() == NOT_SPECIFIED — was mp3spi und vorbisspi
     * durchweg liefern — einen WAV-Header mit falscher Datenlänge (typisch
     * 0 oder -1) schreibt. Der resultierende WAV ist syntaktisch korrekt,
     * meldet aber 0 Samples: Clip.open() liefert Stille, computePeaks()
     * eine leere Waveform und keine der beiden Stellen wirft eine Exception.
     */
    private static byte[] writeToWavWithKnownLength(AudioInputStream pcm, AudioFormat fmt) throws IOException {
        ByteArrayOutputStream pcmData = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = pcm.read(buf)) > 0) {
            pcmData.write(buf, 0, r);
        }
        byte[] pcmBytes = pcmData.toByteArray();
        int frameSize = fmt.getFrameSize();
        long frames = frameSize > 0 ? pcmBytes.length / frameSize : pcmBytes.length;
        try (AudioInputStream withLen = new AudioInputStream(new ByteArrayInputStream(pcmBytes), fmt, frames)) {
            ByteArrayOutputStream wav = new ByteArrayOutputStream();
            AudioSystem.write(withLen, AudioFileFormat.Type.WAVE, wav);
            return wav.toByteArray();
        }
    }

}
