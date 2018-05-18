package utils;

import javax.sound.sampled.*;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Mixer.Info;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class VolumeControl {

    private VolumeControl() {
    }

//    public static void main(String[] args) throws Exception {
//        System.out.println(getHierarchyInfo());
//        System.out.println(getMasterOutputVolume());
//        System.out.println("Change volume to 0.5");
//        setMasterOutputVolume(0.5f);
//        System.out.println(getMasterOutputVolume());
//        Thread.sleep(5000);
//        setMasterOutputVolume(1.0f);
//        Thread.sleep(1000);
//        setMasterOutputVolume(0.5f);
//    }

    private static LinkedList<Line> speakers = new LinkedList<>();

    private static final void findSpeakers() {
        Info[] mixers = AudioSystem.getMixerInfo();
        for (Info mixerInfo : mixers) {
            if (!mixerInfo.getName().equals("Java Sound Audio Engine")) continue;

            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lines = mixer.getSourceLineInfo();

            for (Line.Info info : lines) {

                try {
                    Line line = mixer.getLine(info);
                    speakers.add(line);

                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException iaEx) {
                    System.out.println("Illegal arguments!");
                }
            }
        }
    }

    static {
        findSpeakers();
    }

    static void setMasterOutputVolume(float value) {
        if (value < 0 || value > 1)
            throw new IllegalArgumentException(
                    "Volume can only be set to a value from 0 to 1. Given value is illegal: " + value);
        Line line = getMasterOutputLine();
        if (line == null) throw new RuntimeException("Master output port not found");
        boolean opened = open(line);
        try {
            FloatControl control = getVolumeControl(line);
            if (control == null)
                throw new RuntimeException("Volume control not found in master port: " + toString(line));
            control.setValue(value);
        } finally {
            if (opened) line.close();
        }
    }

    static Float getMasterOutputVolume() {
        Line line = getMasterOutputLine();
        if (line == null) return 0.0f;
        boolean opened = open(line);
        try {
            FloatControl control = getVolumeControl(line);
            if (control == null) return 0.0f;
            return control.getValue();
        } finally {
            if (opened) line.close();
        }
    }

    private static Line getMasterOutputLine() {
        for (Mixer mixer : getMixers()) {
            for (Line line : getAvailableOutputLines(mixer)) {
                if (line.getLineInfo().toString().contains("Master")) return line;
            }
        }
        return null;
    }

    private static FloatControl getVolumeControl(Line line) {
        if (!line.isOpen()) throw new RuntimeException("Line is closed: " + toString(line));
        return (FloatControl) findControl(FloatControl.Type.VOLUME, line.getControls());
    }

    private static Control findControl(Type type, Control... controls) {
        if (controls == null || controls.length == 0) return null;
        for (Control control : controls) {
            if (control.getType().equals(type)) return control;
            if (control instanceof CompoundControl) {
                CompoundControl compoundControl = (CompoundControl) control;
                Control member = findControl(type, compoundControl.getMemberControls());
                if (member != null) return member;
            }
        }
        return null;
    }

    private static List<Mixer> getMixers() {
        Info[] infos = AudioSystem.getMixerInfo();
        List<Mixer> mixers = new ArrayList<>(infos.length);
        for (Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            mixers.add(mixer);
        }
        return mixers;
    }

    private static List<Line> getAvailableOutputLines(Mixer mixer) {
        return getAvailableLines(mixer, mixer.getTargetLineInfo());
    }

    private static List<Line> getAvailableLines(Mixer mixer, Line.Info[] lineInfos) {
        List<Line> lines = new ArrayList<>(lineInfos.length);
        for (Line.Info lineInfo : lineInfos) {
            Line line;
            line = getLineIfAvailable(mixer, lineInfo);
            if (line != null) lines.add(line);
        }
        return lines;
    }

    private static Line getLineIfAvailable(Mixer mixer, Line.Info lineInfo) {
        try {
            return mixer.getLine(lineInfo);
        } catch (LineUnavailableException ex) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static String getHierarchyInfo() {
        StringBuilder sb = new StringBuilder();
        for (Mixer mixer : getMixers()) {
            sb.append("Mixer: ").append(toString(mixer)).append("\n");

            for (Line line : getAvailableOutputLines(mixer)) {
                sb.append("  OUT: ").append(toString(line)).append("\n");
                boolean opened = open(line);
                for (Control control : line.getControls()) {
                    sb.append("    Control: ").append(toString(control)).append("\n");
                    if (control instanceof CompoundControl) {
                        CompoundControl compoundControl = (CompoundControl) control;
                        for (Control subControl : compoundControl.getMemberControls()) {
                            sb.append("      Sub-Control: ").append(toString(subControl)).append("\n");
                        }
                    }
                }
                if (opened) line.close();
            }

            for (Line line : getAvailableOutputLines(mixer)) {
                sb.append("  IN: ").append(toString(line)).append("\n");
                boolean opened = open(line);
                for (Control control : line.getControls()) {
                    sb.append("    Control: ").append(toString(control)).append("\n");
                    if (control instanceof CompoundControl) {
                        CompoundControl compoundControl = (CompoundControl) control;
                        for (Control subControl : compoundControl.getMemberControls()) {
                            sb.append("      Sub-Control: ").append(toString(subControl)).append("\n");
                        }
                    }
                }
                if (opened) line.close();
            }

            sb.append("\n");
        }
        return sb.toString();
    }

    private static boolean open(Line line) {
        if (line.isOpen()) return false;
        try {
            line.open();
        } catch (LineUnavailableException ex) {
            return false;
        }
        return true;
    }

    private static String toString(Control control) {
        if (control == null) return null;
        return control.toString() + " (" + control.getType().toString() + ")";
    }

    private static String toString(Line line) {
        if (line == null) return null;
        Line.Info info = line.getLineInfo();
        return info.toString();// + " (" + line.getClass().getSimpleName() + ")";
    }

    private static String toString(Mixer mixer) {
        if (mixer == null) return null;
        StringBuilder sb = new StringBuilder();
        Info info = mixer.getMixerInfo();
        sb.append(info.getName());
        sb.append(" (").append(info.getDescription()).append(")");
        sb.append(mixer.isOpen() ? " [open]" : " [closed]");
        return sb.toString();
    }

}
