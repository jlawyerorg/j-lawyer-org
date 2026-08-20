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
    // Selection support (fractions 0..1, NaN if none)
    private double selStart = Double.NaN;
    private double selEnd = Double.NaN;
    private boolean selectionEnabled = false;

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
            private double anchor = Double.NaN;
            @Override public void mousePressed(MouseEvent e) {
                if (peaks == null || peaks.length == 0) return;
                if (!selectionEnabled) { seekFrom(e); return; }
                int w = Math.max(1, getWidth());
                anchor = Math.min(1.0, Math.max(0.0, e.getX() / (double) w));
                // start selection
                selStart = anchor;
                selEnd = anchor;
                // also move playhead to anchor
                if (seekListener != null) seekListener.onSeek(anchor);
                setPlayheadFraction(anchor);
                repaint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (peaks == null || peaks.length == 0) return;
                if (!selectionEnabled) { seekFrom(e); return; }
                if (Double.isNaN(anchor)) return;
                int w = Math.max(1, getWidth());
                double f = Math.min(1.0, Math.max(0.0, e.getX() / (double) w));
                selStart = Math.min(anchor, f);
                selEnd = Math.max(anchor, f);
                // Do not force playhead during drag beyond initial anchor
                if (seekListener != null) seekListener.onSeek(playhead);
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!selectionEnabled) return;
                // finalize selection; if tiny range, treat as a click (clear selection)
                if (!Double.isNaN(selStart) && !Double.isNaN(selEnd)) {
                    if (Math.abs(selEnd - selStart) < 0.002) { // <0.2% width => click
                        clearSelection();
                    }
                }
                anchor = Double.NaN;
                if (seekListener != null) seekListener.onSeek(playhead);
                repaint();
            }
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
        // keep any existing selection but clamp to [0,1]
        if (selectionEnabled) {
            if (!Double.isNaN(selStart)) selStart = Math.min(1.0, Math.max(0.0, selStart));
            if (!Double.isNaN(selEnd)) selEnd = Math.min(1.0, Math.max(0.0, selEnd));
        } else {
            clearSelection();
        }
        repaint();
    }

    public void setPlayheadFraction(double fraction) {
        double clamped = Math.min(1.0, Math.max(0.0, fraction));
        if (Math.abs(clamped - this.playhead) > 1e-6) {
            this.playhead = clamped;
            repaint();
        }
    }

    public boolean hasSelection() {
        return selectionEnabled && !Double.isNaN(selStart) && !Double.isNaN(selEnd) && selEnd > selStart;
    }

    public double getSelectionStartFraction() { return hasSelection() ? selStart : Double.NaN; }
    public double getSelectionEndFraction() { return hasSelection() ? selEnd : Double.NaN; }
    public void clearSelection() { selStart = Double.NaN; selEnd = Double.NaN; repaint(); }

    public void setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
        if (!enabled) {
            clearSelection();
        }
        repaint();
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

            // Selection region (drawn over waveform, under playhead)
            if (selectionEnabled && hasSelection()) {
                int x1 = (int) Math.round(selStart * w);
                int x2 = (int) Math.round(selEnd * w);
                int x = Math.max(0, Math.min(w, Math.min(x1, x2)));
                int width = Math.max(1, Math.min(w, Math.abs(x2 - x1)));
                g2.setColor(new Color(14, 114, 181, 60));
                g2.fillRect(x, 0, width, h);
                g2.setColor(new Color(14, 114, 181, 120));
                g2.drawRect(x, 0, width, h-1);
            }

            // Playhead (ensure visible even at fraction 0)
            g2.setColor(new Color(200, 30, 30));
            int x = (int) Math.round(playhead * w);
            x = Math.max(1, Math.min(w - 1, x));
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
