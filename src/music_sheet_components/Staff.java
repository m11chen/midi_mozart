/***************************************************************************************************
Class Name: Staff
***************************************************************************************************/

package music_sheet_components;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.sound.midi.InvalidMidiDataException;

import javax.swing.DefaultListModel;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Properties;

public class Staff {
  public static final int MIDI_SET_TEMPO = 0x51;
  public static final int MIDI_TIME_SIGNATURE = 0x58;
  public static final int MAX_DENOMINATOR = 255;

  public static int default_numerator; // beats per measure
  public static int default_denominator; // 4/n quarter notes per beet
  public static int default_ppq; // pulses (ticks) per quarter note
  public static float default_mpq; // microseconds per quarter note
  public static int loop_count;

  private DefaultListModel<Chord> staff_model = new DefaultListModel<>();
  private ArrayList<MidiEvent> meta_event_list = new ArrayList<>();
  private LinkedHashMap<Long, Long> playback_tick_map = new LinkedHashMap<>();
  private MetaMessage tempo_msg;
  private MetaMessage time_signature_msg;
  private int numerator;
  private int denominator;
  private int ppq;
  private float mpq;
  private String description;
  private int pos;
  private boolean is_playing;

  public static void loadConfig(Properties config) {
    loop_count = Integer.parseInt(config.getProperty("Staff.loop_count", "10"));
    default_numerator = Integer.parseInt(config.getProperty("Staff.default_numerator", "4"));
    default_denominator = Integer.parseInt(config.getProperty("Staff.default_denominator", "4"));
    default_ppq = Integer.parseInt(config.getProperty("Staff.default_ppq", "480"));
    default_mpq = Float.parseFloat(config.getProperty("Staff.default_mpq", "2000000"));
  }

  public static void saveConfig(Properties config) {
    config.setProperty("Staff.loop_count", Integer.toString(loop_count));
    config.setProperty("Staff.default_numerator", Integer.toString(default_numerator));
    config.setProperty("Staff.default_denominator", Integer.toString(default_denominator));
    config.setProperty("Staff.default_ppq", Integer.toString(default_ppq));
    config.setProperty("Staff.default_mpq", Float.toString(default_mpq));
  }

  public Staff(String description) {
    this.description = description;
    pos = 0;
    is_playing = false;
    numerator = default_numerator;
    denominator = default_denominator;
    ppq = default_ppq;
    mpq = default_mpq;
  }

  public Staff(Track tr) {
    description = "";
    is_playing = false;
    pos = 0;
    ppq = default_ppq;

    for (int i = 0; i < tr.size(); i++) {
      MidiEvent e = tr.get(i);
      MidiMessage mm = e.getMessage();
      long t = e.getTick();

      if (mm instanceof MetaMessage) {
        MetaMessage meta = (MetaMessage) mm;

        if (meta.getType() == MIDI_SET_TEMPO && tempo_msg == null) {
          byte[] data = meta.getData();
          mpq = ((data[0] & 0xff) << 16) | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
          tempo_msg = meta;
        } else if (meta.getType() == MIDI_TIME_SIGNATURE && time_signature_msg == null) {
          byte[] data = meta.getData();

          if (data.length < 4) {
            throw new IllegalArgumentException("Midi message is not a time signature event.");
          }

          if ((1 << data[1]) <= MAX_DENOMINATOR) {
            numerator = data[0];
            denominator = 1 << data[1];
          }

          time_signature_msg = meta;
        } else {
          meta_event_list.add(e);
        }
      } else if (mm instanceof ShortMessage) {
        ShortMessage sm = (ShortMessage) mm;
        int cmd = sm.getCommand();

        if (cmd == Note.NOTE_ON || cmd == Note.NOTE_OFF) {
          insertMsg(t, sm);
        }
      }
    }

    try {
      if (mpq == 0) {
        mpq = default_mpq;
        byte[] data = getTempoData(default_mpq);
        tempo_msg = new MetaMessage(MIDI_SET_TEMPO, data, data.length);
      }

      if (numerator == 0 || denominator == 0) {
        numerator = default_numerator;
        denominator = default_denominator;
        byte[] data = getTimeSignatureData();
        MetaMessage time_signature_msg = new MetaMessage(MIDI_TIME_SIGNATURE, data, data.length);
      }
    } catch(InvalidMidiDataException e) {
      System.out.println(e);
    }
  }

  public Staff(String description, Track t) {
    this(t);
    this.description = description;
  }

  public LinkedHashMap<String, String> getConfigOptions() {
    LinkedHashMap<String, String> options = new LinkedHashMap<>();
    options.put("Numerator", Integer.toString(numerator));
    options.put("Denominator", Integer.toString(denominator));
    options.put("Pulses/Ticks per Quarter Note (PPQ)", Integer.toString(ppq));
    options.put("Microseconds per Quarter Note (MPQ)", Float.toString(mpq));
    return options;
  }

  public void setConfigOptions(LinkedHashMap<String, String> options) throws NumberFormatException {
    numerator = Integer.parseInt(options.get("Numerator"));
    denominator = Integer.parseInt(options.get("Denominator"));
    ppq = Integer.parseInt(options.get("Pulses/Ticks per Quarter Note (PPQ)"));
    mpq = Float.parseFloat(options.get("Microseconds per Quarter Note (MPQ)"));

    if (numerator <= 0 || denominator <= 0 || ppq <= 0 || mpq <= 0) {
      throw new NumberFormatException("Values less than or equal to 0 are not valid.");
    }
  }

  public void setNumerator(int n) throws NumberFormatException {
    if (n <= 0) {
      throw new NumberFormatException("Values less than or equal to 0 are not valid.");
    }

    numerator = n;
  }

  public void setDenominator(int d) throws NumberFormatException {
    if (d <= 0) {
      throw new NumberFormatException("Values less than or equal to 0 are not valid.");
    }

    denominator = d;
  }

  public void setPPQ(int p) throws NumberFormatException {
    if (p <= 0) {
      throw new NumberFormatException("Values less than or equal to 0 are not valid.");
    }

    ppq = p;
  }

  public void setMPQ(float m) throws NumberFormatException {
    if (m <= 0) {
      throw new NumberFormatException("Values less than or equal to 0 are not valid.");
    }

    mpq = m;
  }

  public DefaultListModel<Chord> getModel() {
    return staff_model;
  }

  private byte[] getTempoData(float f_tmp) {
    int tmp = (int) f_tmp;

    byte[] data = {
        (byte) ((tmp & 0xff000000) >> 24),
        (byte) ((tmp & 0x00ff0000) >> 16),
        (byte) ((tmp & 0x0000ff00) >> 8),
        (byte) (tmp & 0x000000ff)};
    
    // Clear leading 0's.
    int i;
    for (i = 0; i < data.length-1 && data[i] == 0; i++);

    if (i != 0) {
      data = Arrays.copyOfRange(data, i, data.length);
    }
    
    return data;
  }

  private byte[] getTimeSignatureData() {
    // Base 2 log calculator for whole numbers.
    int i = 0;

    while (denominator != 1) {
      denominator /= 2;
      i++;
    }
    
    byte[] data = {
        (byte) numerator,
        (byte) i,
        (byte) 24, // metronome
        (byte) 8}; // notated 32nd notes per beat

    return data;
  }

  public void setResolution(int ppq) {
    this.ppq = ppq;
  }

  public void setChordTimeSignature() {
    Chord.setTimeSignature(numerator, denominator, ppq, mpq);
  }

  private void insertMsg(long t, ShortMessage sm) {
    int cmd = sm.getCommand();
    int vel = sm.getData2();

    if (cmd == Note.NOTE_ON && vel != 0) {
      Note n = new Note(t, sm);

      if (staff_model.size() == 0 || this.toEnd().getTick() != t) {
        staff_model.addElement(new Chord(n));
      } else {
        this.toEnd().addNote(n);
      }

      return;
    } else if (cmd == Note.NOTE_OFF || vel == 0) {
      for (Object o : staff_model.toArray()) {
        Chord c = (Chord) o;

        for (int j = 0; j < c.size(); j++) {
          String name = Note.getMsgNoteName(sm);

          if (!(c.getNote(j).isTerminated()) && c.getNote(j).getName().equals(name)) {
            c.getNote(j).terminate(t, sm);
            return;
          }
        }
      }
    }
  }

  public void setDescription(String desc) {
    description = desc;
  }

  @Override
  public String toString() {
    return description;
  }

  public int size() {
    if (staff_model != null) {
      return staff_model.size();
    } else {
      return 0;
    }
  }

  public void addChord(Chord c) {
    if (staff_model.size() == 0) {
      pos = 0;
    }

    staff_model.addElement(c);
  }

  public void addChord(int pos, Chord c) {
    staff_model.add(pos, c);
  }

  public void setChord(int pos, Chord c) {
    staff_model.set(pos, c);
  }

  public Chord getChord(int pos) {
    return staff_model.get(pos);
  }

  public Chord moveForward() {
    pos++;

    if (pos >= this.size()) {
      pos = this.size()-1;
      return null;
    }

    return staff_model.get(pos);
  }

  public Chord moveBack() {
    pos--;

    if (pos < 0) {
      pos = 0;
      return null;
    }

    return staff_model.get(pos);
  }

  public Chord toStart() {
    pos = 0;

    if (this.size() > 0) {
      return staff_model.get(0);
    } else {
      return null;
    }
  }

  public Chord toEnd() {
    pos = staff_model.size()-1;

    if (this.size() > 0) {
      return staff_model.get(pos);
    } else {
      pos = 0;
      return null;
    }
  }

  public Chord goTo(int i) {
    pos = i;
    return staff_model.get(pos);
  }

  public int getPosition() {
    return pos;
  }

  public void resetPosition() {
    pos = 0;
  }

  public long[] getAllTicks() {
    long[] tick_list = new long[this.size()];

    for (int i = 0; i < this.size(); i++) {
      tick_list[i] = staff_model.get(i).getTick();
    }

    return tick_list;
  }

  public void print() {
    System.out.println("Staff description: "+description+"\n");

    for (int i = 0; i<staff_model.size(); i++) {
      System.out.print("pos "+i+" ");
      staff_model.get(i).print();
    }
  }

  public static Staff merge(Staff s1, Staff s2) {
    if (s1 == null && s2 != null) {
      return s2;
    } else if (s1 != null && s2 == null) {
      return s1;
    } else if (s1 == null && s2 == null) {
      return null;
    }

    Staff s = new Staff(s1.toString()+" + "+s2.toString());
    Chord c1 = s1.toStart();
    Chord c2 = s2.toStart();

    while (c1 != null && c2 != null) {
      if (c1.getTick() < c2.getTick()) {
        s.addChord(c1);
        c1 = s1.moveForward();
      } else if (c1.getTick() == c2.getTick()) {
        s.addChord(Chord.join(c1, c2));
        c1 = s1.moveForward();
        c2 = s2.moveForward();
      } else {
        s.addChord(c2);
        c2 = s2.moveForward();
      }
    }

    while(c1 != null) {
      s.addChord(c1);
      c1 = s1.moveForward();
    }

    while (c2 != null) {
      s.addChord(c2);
      c2 = s2.moveForward();
    }

    s.setConfigOptions(s1.getConfigOptions());
    return s;
  }

  public void play(Sequencer seqr, int start_pos, int stop_pos, int mode) {
    if (seqr == null) {
      return;
    }

    try {
      Sequence seq = new Sequence(Sequence.PPQ, ppq);
      Track tr = seq.createTrack();
      Note n;
      Chord c;
      long start = 0;
      long stop = 0;
      long offset_in_ticks = 0;
      long buffer_in_ticks = (long)(ppq*Chord.offset_buffer*1000/mpq);
      MidiEvent start_e;
      MidiEvent stop_e;

      if (mode == Chord.OFFSET) {
        offset_in_ticks = (long)(ppq*Chord.chord_offset*1000/mpq);
      }

      for (int i = start_pos; i <= stop_pos; i++) {
        c = staff_model.get(i);
        playback_tick_map.put(c.getTick(), start+offset_in_ticks);

        for (int j = c.size()-1; j >= 0; j--) {
          n = c.getNote(j);
          start += offset_in_ticks;

          if (mode == Chord.SYNCHRONIZED) {
            stop = start+n.length();
          } else if (mode == Chord.OFFSET) {
            stop = start+(j+1)*offset_in_ticks;
          }

          ShortMessage start_msg = new ShortMessage();

          if (mode == Chord.SYNCHRONIZED) {
            start_msg = n.start();
          } else if (mode == Chord.OFFSET) {
            start_msg = new ShortMessage(n.start().getStatus(),
                n.getIndex(), Note.review_velocity);
          }

          start_e = new MidiEvent(start_msg, start);
          stop_e = new MidiEvent(n.stop(), stop);
          tr.add(start_e);
          tr.add(stop_e);
        }

        if (i < stop_pos) {
          if (mode == Chord.SYNCHRONIZED) {
            start += staff_model.get(i+1).getTick()-c.getTick();
          } else if (mode == Chord.OFFSET) {
            start += buffer_in_ticks;
          }
        } else if (i == stop_pos) {
          // Insert a silent pause 1.5 times the length of chord offset at end of sequence.
          // Allows for a break marking the start of the next loop count.
          start = stop;
          stop += buffer_in_ticks*3/2;
          ShortMessage start_pause = new ShortMessage(Note.NOTE_ON, 0, 100, 0);
          ShortMessage stop_pause = new ShortMessage(Note.NOTE_OFF, 0, 100, 0);
          start_e = new MidiEvent(start_pause, start);
          stop_e = new MidiEvent(stop_pause, stop);
          tr.add(start_e);
          tr.add(stop_e);
        }
      }

      if (seqr.isRunning()) {
        seqr.stop();
      }

      seqr.setSequence(seq);

      if (mpq != 0) {
        seqr.setTempoInMPQ(mpq);
      }

      if (start_pos != stop_pos) {
        seqr.setLoopCount(loop_count-1);
      } else {
        seqr.setLoopCount(0);
      }

      seqr.start();

      is_playing = true;
    } catch(InvalidMidiDataException e) {
      System.out.println(e);
    }
  }

  public boolean isPlaying() {
    return is_playing;
  }

  public void stop(Sequencer seqr) {
    if (seqr == null) {
      return;
    }

    seqr.stop();
    is_playing = false;

    while (playback_tick_map.get(staff_model.get(pos).getTick()) < seqr.getTickPosition()) {
      if (pos == this.size()-1) {
        break;
      } else {
        pos++;
      }
    }

    while (playback_tick_map.get(staff_model.get(pos).getTick()) > seqr.getTickPosition()) {
      if (pos == 0) {
        break;
      } else {
        pos--;
      }
    }

    playback_tick_map.clear();
  }
}
