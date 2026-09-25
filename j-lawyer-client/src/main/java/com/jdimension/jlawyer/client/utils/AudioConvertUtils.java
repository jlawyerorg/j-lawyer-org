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
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Dekodiert MP3/OGG-Audiodaten in PCM-WAV. Nutzt die auf dem Classpath
 * registrierten SPIs für javax.sound.sampled (mp3spi + jlayer,
 * vorbisspi + jorbis, tritonus-all für Resampling/Downmix).
 */
public class AudioConvertUtils {

    private AudioConvertUtils() {
    }

    /**
     * Dekodiert Audiobytes verlustfrei zu PCM-WAV in der Original-Rate und
     * Original-Kanalanzahl. Für die reine Wiedergabe im
     * ReadOnlySoundplayerPanel.
     */
    public static byte[] decodeForPlayback(byte[] input) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream src = AudioSystem.getAudioInputStream(new ByteArrayInputStream(input))) {
            AudioFormat srcFmt = src.getFormat();
            AudioFormat pcmFmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    srcFmt.getSampleRate(),
                    16,
                    srcFmt.getChannels(),
                    srcFmt.getChannels() * 2,
                    srcFmt.getSampleRate(),
                    false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmFmt, src)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                AudioSystem.write(pcm, AudioFileFormat.Type.WAVE, baos);
                return baos.toByteArray();
            }
        }
    }

    /**
     * Dekodiert Audiobytes in das kanonische "Weiterverarbeitungs"-Format
     * (16 kHz mono, 16 bit PCM signed, little endian) und liefert einen
     * WAV-Container zurück. Format entspricht {@link AudioUtils#getAudioFormat()}.
     * Multi-Channel-Quellen werden downgemischt, Sample-Rates auf 16 kHz konvertiert.
     */
    public static byte[] decodeToStandardWav(byte[] input) throws IOException, UnsupportedAudioFileException {
        AudioFormat targetFmt = AudioUtils.getAudioFormat();
        try (AudioInputStream src = AudioSystem.getAudioInputStream(new ByteArrayInputStream(input))) {
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
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                AudioSystem.write(target, AudioFileFormat.Type.WAVE, baos);
                return baos.toByteArray();
            }
        }
    }

    /**
     * Grobe Größenschätzung für eine als kanonische WAV (16 kHz mono, 16 bit)
     * gespeicherte Version einer Aufnahme gegebener Dauer.
     *
     * @param durationMicros Dauer in Mikrosekunden (aus Clip.getMicrosecondLength())
     * @return geschätzte Größe der Ziel-WAV in Byte, inkl. 44 Byte Header
     */
    public static long estimateStandardWavSize(long durationMicros) {
        AudioFormat fmt = AudioUtils.getAudioFormat();
        double seconds = durationMicros / 1_000_000.0;
        long dataBytes = (long) Math.ceil(seconds * fmt.getSampleRate() * (fmt.getSampleSizeInBits() / 8.0) * fmt.getChannels());
        return dataBytes + 44L;
    }
}
