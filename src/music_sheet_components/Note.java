/***************************************************************************************************
Class Name: Note
***************************************************************************************************/

package music_sheet_components;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import java.util.Properties;

public class Note {
  public static final int NOTE_ON =  0x90;
  public static final int NOTE_OFF = 0x80;

  public static final String[] NOTE_NAMES =
    {"C", "C sharp ", "D", "D sharp ", "E", "F", "F sharp ", "G", "G sharp ", "A", "A sharp ", "B"};

  public static int review_velocity;
  public static int ppq; // pulses (ticks) per quarter note.

  private ShortMessage start;
  private ShortMessage stop;
  private String name;
  private int index;
  private int velocity;
  private long tick;
  private long length;

  public static void loadConfig(Properties config) {
    review_velocity = Integer.parseInt(config.getProperty("Note.review_velocity", "80"));
  }

  public static void saveConfig(Properties config) {
    config.setProperty("Note.review_velocity", Integer.toString(review_velocity));
  }

  public Note(ShortMessage sm) {
    start = sm;
    name = getMsgNoteName(sm);
    index = sm.getData1();
    velocity = sm.getData2();
    tick = -1;
    length = -1;
  }

  public Note(long t, ShortMessage sm) {
    this(sm);
    tick = t;
  }

  public Note(long t, long l, ShortMessage start, ShortMessage stop) {
    this(t, start);
    this.terminate(t+l, stop);
  }

  public void terminate(long t, ShortMessage sm) {
    stop = sm;
    length = t-tick;
  }

  public boolean isTerminated() {
    return (length != -1);
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return "Note "+name+"; length: "+length+"; velocity: "+velocity;
  }

  public void print() {
    System.out.println(this.toString());
  }

  public int getIndex() {
    return index;
  }

  public long getTick() {
    return tick;
  }

  public long length() {
    return length;
  }

  public ShortMessage start() {
    return start;
  }

  public ShortMessage stop() {
    return stop;
  }

  public static String getMsgNoteName(ShortMessage msg) {
    int key = msg.getData1();
    int oct = (key/12)-1;
    int n = key%12;
    String s = NOTE_NAMES[n];
    return s+oct;
  }

  public static void setPPQ(int p) {
    ppq = p;
  }

  public void play(Sequencer seqr) {
    if (seqr == null) {
      return;
    }

    try {
      Sequence seq = new Sequence(Sequence.PPQ, ppq);
      Track tr = seq.createTrack();
      ShortMessage review_start = new ShortMessage(start.getStatus(), index, review_velocity);
      MidiEvent e_start = new MidiEvent(review_start, 0);
      MidiEvent e_stop = new MidiEvent(stop, length);
      tr.add(e_start);
      tr.add(e_stop);

      if (seqr.isRunning()) {
        seqr.stop();
      }

      seqr.setSequence(seq);
      seqr.setLoopCount(0);
      seqr.start();
    } catch(InvalidMidiDataException e) {
      System.out.println(e.toString());
    }
  }
}
  