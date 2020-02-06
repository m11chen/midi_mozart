/***************************************************************************************************
Class Name: Chord
***************************************************************************************************/

package music_sheet_components;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.InvalidMidiDataException;

import javax.swing.DefaultListModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Properties;

public class Chord {
  public static final int SYNCHRONIZED = 0;
  public static final int OFFSET = 1;

  public static int chord_offset;
  public static int offset_buffer;

  private static float qpb; // quarter notes per beat
  private static int bpm; // beats per measure
  private static int ppq; // pulses/ticks per quarter note
  private static float mpq; // microseconds per quarter note

  private DefaultListModel<Note> chord_model = new DefaultListModel<>();
  private long tick;
  private int index;

  public static void loadConfig(Properties config) {
    chord_offset = Integer.parseInt(config.getProperty("Chord.chord_offset", "1000"));
    offset_buffer = Integer.parseInt(config.getProperty("Chord.offset_buffer", "1000"));
  }

  public static void saveConfig(Properties config) {
    config.setProperty("Chord.chord_offset", Integer.toString(chord_offset));
    config.setProperty("Chord.offset_buffer", Integer.toString(offset_buffer));
  }

  public Chord(Note n) {
    chord_model.addElement(n);
    tick = n.getTick();
    index = 0;
  }

  public DefaultListModel<Note> getModel() {
    return chord_model;
  }

  public static void setTimeSignature(int numerator, int denominator, int ppq, float mpq) {
    Chord.bpm = numerator;
    Chord.qpb = 4/denominator;
    Chord.ppq = ppq;
    Chord.mpq = mpq;
    Note.setPPQ(ppq);
  }

  public void addNote(Note n) {
    int i;

    for (i = 0; i < this.size(); i++) {
      Note m = chord_model.get(i);

      if (n.getIndex() > m.getIndex()) {
        chord_model.add(i, n);
        break;
      }
    }

    if (i == this.size()) {
      chord_model.addElement(n);
    }
  }

  public Note moveUp() {
    index--;

    if (index < 0) {
      index = 0;
    }

    return chord_model.get(index);
  }

  public Note moveDown() {
    index++;

    if (index >= this.size()-1) {
      index = this.size()-1;
    }

    return chord_model.get(index);
  }

  public void resetIndex() {
    index = this.size()-1;
  }

  public int getIndex() {
    return index;
  }

  public String[] getNoteNames() {
    ArrayList<String> note_names = new ArrayList<String>();
    
    for (Object o : chord_model.toArray()) {
      Note n = (Note) o;
      note_names.add(n.getName());
    }

    String[] ret_note_names = new String[this.size()];
    ret_note_names = note_names.toArray(ret_note_names);
    return ret_note_names;
  }

  public Note getNote(int i) {
    index = i;
    return chord_model.get(i);
  }

  public long getTick() {
    return tick;
  }

  public int size() {
    return chord_model.size();
  }

  @Override
  public String toString() {
    long t = tick;
    int tpb = ppq;
    int tick_resolution = Integer.toString(tpb).length();
    String measure = Long.toString((t/tpb)/bpm);
    String beat = Long.toString((t/ppq)%bpm);
    long tick = t%tpb;
    String s = String.format("%-"+tick_resolution+"s", tick).replace(" ", "0");
    return measure+"M "+beat+"."+s;
  }

  public static Chord join(Chord c1, Chord c2) {
    Chord c = c1;

    for (Object o : c2.chord_model.toArray()) {
      Note n = (Note) o;
      c.addNote(n);
    }

    return c;
  }

  public void print() {
    if (chord_model.size() > 1) {
      System.out.println("chord at tick "+tick+", ("+chord_model.size()+" note(s))");
    }

    for (Object o : chord_model.toArray()) {
      Note i_note = (Note) o;
      i_note.print();
    }
  }

  public void play(Sequencer seqr, int mode) {
    if (seqr == null) {
      return;
    }

    try {
      Sequence seq = new Sequence(Sequence.PPQ, ppq);
      Track tr = seq.createTrack();
      Note n;
      long start = 0;
      long stop = 0;
      long offset_in_ticks = 0;
      ShortMessage start_msg;

      if (mode == OFFSET) {
        offset_in_ticks = (long)(ppq*chord_offset*1000/mpq);
      }

      int i = 0;

      for (int j = this.size()-1; j >= 0; j--) {
        n = chord_model.get(j);
        start = i*offset_in_ticks;

        if (mode == SYNCHRONIZED) {
        stop = start+n.length();
      } else if (mode == OFFSET) {
        stop = start+(j+1)*offset_in_ticks;
      }

        start_msg = new ShortMessage(n.start().getStatus(),
            n.getIndex(), Note.review_velocity);

        MidiEvent start_e = new MidiEvent(start_msg, start);
        MidiEvent stop_e = new MidiEvent(n.stop(), stop);
        tr.add(start_e);
        tr.add(stop_e);
        i++;
      }

      if (seqr.isRunning()) {
        seqr.stop();
      }

      seqr.setSequence(seq);

      if (mpq != 0) {
        seqr.setTempoInMPQ(mpq);
      }

      seqr.setLoopCount(0);
      seqr.start();
    } catch(InvalidMidiDataException e) {
      System.out.println(e.toString());
    }
  }
}
