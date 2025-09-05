package com.jdimension.jlawyer.client.editors.documents.viewer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Lightweight waveform view with seek support.
 * - Call setWaveform(float[]) with normalized peak values in [0,1].
 * - Call setPlayheadFraction(double) to update the cursor.
 * - Register a seek listener to receive click/drag seek fractions.
 */
public class WaveformPanel extends JPanel {

    public interface SeekListener {
        void onSeek(double fraction);
    }

    private float[] peaks = new float[0];
    private double playhead = 0.0; // 0..1
    private SeekListener seekListener;

    public WaveformPanel() {
        setOpaque(true);
        setBackground(Color.white);
        setPreferredSize(new Dimension(400, 80));

        MouseAdapter mouse = new MouseAdapter() {
            private void seekFrom(MouseEvent e) {
                if (peaks == null || peaks.length == 0) return;
                int w = Math.max(1, getWidth());
                double fraction = Math.min(1.0, Math.max(0.0, e.getX() / (double) w));
                if (seekListener != null) seekListener.onSeek(fraction);
                setPlayheadFraction(fraction);
            }
            @Override public void mousePressed(MouseEvent e) { seekFrom(e); }
            @Override public void mouseDragged(MouseEvent e) { seekFrom(e); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void setSeekListener(SeekListener listener) {
        this.seekListener = listener;
    }

    public void setWaveform(float[] peaks) {
        if (peaks == null) peaks = new float[0];
        this.peaks = Arrays.copyOf(peaks, peaks.length);
        repaint();
    }

    public void setPlayheadFraction(double fraction) {
        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        if (Math.abs(clamped - this.playhead) > 1e-6) {
            this.playhead = clamped;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Baseline
            int mid = h / 2;
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(0, mid, w, mid);

            if (peaks != null && peaks.length > 0) {
                // Downsample/upsample peaks to width
                g2.setColor(new Color(14, 114, 181));
                for (int x = 0; x < w; x++) {
                    double idx = x * (peaks.length - 1) / Math.max(1.0, (w - 1));
                    int i0 = (int) Math.floor(idx);
                    int i1 = Math.min(peaks.length - 1, i0 + 1);
                    double t = idx - i0;
                    double val = (1 - t) * peaks[i0] + t * peaks[i1];
                    val = Math.min(1.0, Math.max(0.0, val));
                    int amp = (int) Math.round(val * (h * 0.45));
                    g2.drawLine(x, mid - amp, x, mid + amp);
                }
            }

            // Playhead
            g2.setColor(new Color(200, 30, 30));
            int x = (int) Math.round(playhead * w);
            g2.drawLine(x, 0, x, h);
        } finally {
            g2.dispose();
        }
    }

    // Convenience helper to compute peaks from raw WAV/PCM bytes
    public static float[] computePeaks(byte[] audioBytes, int targetPeaks) throws Exception {
        if (audioBytes == null || audioBytes.length == 0) return new float[0];

        try (AudioInputStream in = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBytes))) {
            AudioFormat base = in.getFormat();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 16000f,
                    16, 1, 2,
                    base.getSampleRate() > 0 ? base.getSampleRate() : 16000f, false);

            AudioInputStream pcm = in;
            if (!isPcm16MonoLE(base)) {
                pcm = AudioSystem.getAudioInputStream(target, in);
            }

            int frameSize = pcm.getFormat().getFrameSize();
            long frames = pcm.getFrameLength();
            long totalBytes = frames <= 0 ? audioBytes.length : frames * frameSize;
            int bins = Math.max(64, Math.min(4096, targetPeaks));

            float[] peaks = new float[bins];
            byte[] buf = new byte[4096];
            long bytesReadTotal = 0;
            while (true) {
                int read = pcm.read(buf);
                if (read <= 0) break;
                int samples = read / 2; // 16-bit mono
                for (int i = 0; i < samples; i++) {
                    int lo = buf[2 * i] & 0xFF;
                    int hi = buf[2 * i + 1];
                    short s = (short) ((hi << 8) | lo);
                    double norm = Math.abs(s) / 32768.0;
                    // position within full stream [0,1)
                    double pos = (bytesReadTotal / (double) Math.max(1, totalBytes));
                    int bin = (int) Math.min(bins - 1, Math.floor(pos * bins));
                    if (norm > peaks[bin]) peaks[bin] = (float) norm;
                    bytesReadTotal += 2;
                }
            }

            // Normalize in case of zeros
            float max = 0f;
            for (float v : peaks) max = Math.max(max, v);
            if (max > 0f) {
                for (int i = 0; i < peaks.length; i++) peaks[i] = Math.min(1f, peaks[i] / max);
            }
            return peaks;
        }
    }

    private static boolean isPcm16MonoLE(AudioFormat f) {
        return f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                && f.getSampleSizeInBits() == 16
                && f.getChannels() == 1
                && f.isBigEndian() == false;
    }
}

