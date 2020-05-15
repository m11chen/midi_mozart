/***************************************************************************************************
Class Name: MainView
***************************************************************************************************/

import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.WindowConstants;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import music_sheet_components.*;

public class MainView extends JPanel implements FocusListener,
    TreeWillExpandListener, TreeSelectionListener, ListSelectionListener,
    KeyListener {

  // Define key codes.
  public static final int D = 68;
  public static final int E = 69;
  public static final int F = 70;
  public static final int S = 83;
  public static final int LEFT = 37;
  public static final int UP = 38;
  public static final int RIGHT = 39;
  public static final int DOWN = 40;
  public static final int PAGE_UP = 33;
  public static final int PAGE_DOWN = 34;
  public static final int HOME = 36;
  public static final int END = 35;
  public static final int SPACE = 32;
  public static final int TAB = 9;
  public static final int SHIFT = 16;
  public static final int CTRL = 17;
  public static final int ALT = 18;
  public static final int F1 = 112;
  public static final int F2 = 113;
  public static final int F3 = 114;
  public static final int F4 = 115;
  public static final int F5 = 116;
  public static final int F6 = 117;
  public static final int F11 = 122;
  public static final int ESCAPE = 27;

  public static final List<Integer> MODIFIER_KEYS = Arrays.asList(new Integer[]
      {SHIFT, ALT, CTRL});

  public static final List<Integer> ARROW_KEYS = Arrays.asList(new Integer[]
      {UP, DOWN, LEFT, RIGHT, S, D, F, E});

  public static final List<String> UI_COMPONENTS = Arrays.asList(new String[]
      {"Library", "Position", "Note", "Status"});

  // Define actions for configMidiDevice().
  public static final int LOAD_DEVICES = 0;
  public static final int SELECT_DEVICES = 1;

  // Define actions for configOptions().
  public static final int EDIT_LIBRARY_PATH = 0;
  public static final int EDIT_TIME_SIGNATURE = 1;
  public static final int EDIT_PLAYBACK_OPTIONS = 2;

  private static final Dimension time_view_minimum_size = new Dimension(500, 8);

  // Define javax.swing components.
  private static JList position_list;
  private static JList note_list;
  private static JTree lib_tree;
  private static JScrollPane time_view;
  private static JFrame frame;
  private static JPanel status_bar;
  private static JLabel    status;

  private static MidiDevice midi_in;
  private static MidiDevice midi_out;
  private static Sequencer seqr;  

  private static File lib_dir;
  private static int lib_level;
  private static Song song;
  private static Song playing_song;
  private static Staff staff;
  private static Staff playing_staff;
  private static Chord chord;
  private static Note note;
  private static int start;
  private static int stop;

  private static ArrayList<Integer> held_keys;
  private static int last_key_press;
  private static int playback_mode;
  private static String ui_component;
  private static String last_ui_component;

  // locale messages and Configuration property variables.
  private static SortedStoreProperties lc_messages;
  private static SortedStoreProperties config;
  private static String lib_path;
  private static int tree_expansion_buffer; // Number of levels to create nodes recursively.
  private static MidiDevice.Info seqr_info;
  private static MidiDevice.Info midi_in_info;
  private static MidiDevice.Info midi_out_info;

  public static void loadConfig() {
    lib_path = config.getProperty("MainView.lib_path", "midi_library");

    tree_expansion_buffer
        = Integer.parseInt(config.getProperty("MainView.tree_expansion_buffer", "1"));

    String seqr_name = config.getProperty("MainView.sequencer", "");
    String midi_in_name = config.getProperty("MainView.midi_in", "");
    String midi_out_name = config.getProperty("MainView.midi_out", "");
    MidiDevice.Info[] info_list = MidiSystem.getMidiDeviceInfo();

    for (MidiDevice.Info mdi : info_list) {
      if (seqr_name.equals(mdi.getName())) {
        seqr_info = mdi;
      }

      if (midi_in_name.equals(mdi.getName())) {
        midi_in_info = mdi;
      }

      if (midi_out_name.equals(mdi.getName())) {
        midi_out_info = mdi;
      }
    }

    Staff.loadConfig(config);
    Chord.loadConfig(config);
    Note.loadConfig(config);
  }

  public static void SaveConfig() {
    config.setProperty("MainView.lib_path", lib_path);
    config.setProperty("MainView.tree_expansion_buffer", Integer.toString(tree_expansion_buffer));

    if (seqr != null) {
      config.setProperty("MainView.sequencer", seqr_info.getName());
    }

    if (midi_out != null) {
      config.setProperty("MainView.midi_out", midi_out_info.getName());
    }

    if (midi_in != null) {
      config.setProperty("MainView.midi_in", midi_in_info.getName());
    }

    Staff.saveConfig(config);
    Chord.saveConfig(config);
    Note.saveConfig(config);

    try {
      String comment = "Configuration file for MIDI Mozart.";
      config.store(new FileOutputStream("config.properties"), comment);
    } catch(IOException e) {
      System.out.println(e);
    }
  }

  public MainView() {
    super( new GridLayout(1, 0));
    
    // Initialize library tree view and add to panel.
    lib_level = 0;
    AliasTreeNode lib_root = new AliasTreeNode(lib_dir, "MIDI Library", lib_level);
    createLibNodes(lib_root, lib_level+tree_expansion_buffer);
    lib_tree = new JTree(lib_root);
    lib_tree.getAccessibleContext().setAccessibleName("Library");
    lib_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    lib_tree.setFocusTraversalKeysEnabled(false);
    lib_tree.addFocusListener(this);
    lib_tree.addKeyListener(this);
    lib_tree.addTreeSelectionListener(this);
    lib_tree.addTreeWillExpandListener(this);
    add(lib_tree);

    // Initialize positions list an add to time view horizontal scroll pane.
    position_list = new JList<Chord>();
    position_list.getAccessibleContext().setAccessibleName("Position");
    position_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    position_list.setFocusTraversalKeysEnabled(false);
    position_list.addFocusListener(this);
    position_list.addKeyListener(this);
    position_list.getSelectionModel().addListSelectionListener(this);
    position_list.setVisibleRowCount(1);
    position_list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    time_view = new JScrollPane(position_list);
    time_view = new JScrollPane(position_list);

    // Add time view to panel.
    time_view.setMinimumSize(time_view_minimum_size);
    time_view.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    add(time_view);

    // Initialize notes list and add to panel.
    note_list = new JList<Note>();
    note_list.getAccessibleContext().setAccessibleName("Note");
    note_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    note_list.setFocusTraversalKeysEnabled(false);
    note_list.addFocusListener(this);
    note_list.addKeyListener(this);
    add(note_list);

    // Add status bar.
    status = new JLabel();
    status.setFocusTraversalKeysEnabled(false);
    status.addFocusListener(this);
    status.addKeyListener(this);
    status_bar = new JPanel();
    status_bar.add(status);
    add(status_bar, BorderLayout.SOUTH);
  }

  @Override
  public void focusGained(FocusEvent event) {
    ui_component = event.getComponent().getAccessibleContext().getAccessibleName();

    if (!UI_COMPONENTS.contains(ui_component)) {
      ui_component = "Status";
    }

    if (ui_component.equals("Position") && !ui_component.equals(last_ui_component)) {
      if (staff.isPlaying()) {
        staff.stop(seqr);
      }

      position_list.setSelectedIndex(staff.getPosition());
      loadChord(staff.getChord(staff.getPosition()));

      if (last_key_press == TAB) {
        chord.play(seqr, playback_mode);
      }
    }
  }

  @Override
  public void focusLost(FocusEvent event) {
    last_ui_component = event.getComponent().getAccessibleContext().getAccessibleName();

    if (!UI_COMPONENTS.contains(last_ui_component)) {
      last_ui_component = "Status";
    }
  }

  @Override
  public void treeWillExpand(TreeExpansionEvent event) {
    AliasTreeNode node = (AliasTreeNode) lib_tree.getLastSelectedPathComponent();
    Object node_obj = node.getUserObject();

    if (!node.hasExpanded() && node_obj instanceof File) {
      //createLibNodes(node, lib_level+tree_expansion_buffer);
      //node.setExpanded(true);
    }
  }

  @Override
  public void treeWillCollapse(TreeExpansionEvent event) {
  }

  @Override
  public void valueChanged(TreeSelectionEvent event) {
    AliasTreeNode node = (AliasTreeNode) lib_tree.getLastSelectedPathComponent();

    if (node == null) {
      return;
    }

    Object node_obj = node.getUserObject();
    lib_level = node.level();

/*
    if (!node.hasExpanded()) {
      if (node_obj instanceof File && ((File) node_obj).isDirectory()) {
        for (File f : ((File) node_obj).listFiles()) {
          node.add(new AliasTreeNode(f, f.getName(), lib_level+1));
        }

        node.setExpanded(true);
      } else if (node_obj instanceof File && ((File) node_obj).isFile()) {
        Song sg = createSong((File) node_obj, lib_level);
        node.setUserObject(sg);
        node_obj = sg;
        int track_level = lib_level+1;

        for (int i = 1; i < sg.getStaffList().size(); i++) {
          Staff s = sg.getStaffList().get(i);
          AliasTreeNode a = new AliasTreeNode(s, s.toString()+"; level "+track_level, track_level);
          node.add(a);
        }
      }

      node.setExpanded(true);
    }
*/

    if (staff != null && staff.isPlaying()) {
      playing_staff = staff;
      playing_song = song;
    } else if (song != null && staff != null && !position_list.isSelectionEmpty()) {
      song.syncTickPosition(staff.getChord(staff.getPosition()).getTick());
    }

    if (node_obj.getClass().getName() == "MainView$Song") {
      song = (Song) node_obj;
      staff = song.getStaffList().get(0);
      setStatus(staff.toString());
    } else if (node_obj.getClass().getName() == "music_sheet_components.Staff") {
      if (song != null && !song.getStaffList().contains(staff)) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        song = (Song) parent.getUserObject();
      }

      staff = (Staff) node_obj;
      setStatus(staff.toString());
    } else {
      song = null;
      staff = null;
      setStatus("No staff loaded.");
    }

    loadStaff();
  }

  @Override
  public void valueChanged(ListSelectionEvent event) {
    ListSelectionModel lsm = (ListSelectionModel)event.getSource();
    ArrayList<Integer> selected_indices = new ArrayList<>();

    // Find out which indices are selected.
    if (!lsm.isSelectionEmpty()) {
      int minIndex = lsm.getMinSelectionIndex();
      int maxIndex = lsm.getMaxSelectionIndex();

      for (int i = minIndex; i <= maxIndex; i++) {
        if (lsm.isSelectedIndex(i)) {
          selected_indices.add(i);
        }
      }
    }

    // Allow jumping to item by typing position string with keyboard.
    if (last_key_press != -1 &&
      !ARROW_KEYS.contains(last_key_press) &&
      selected_indices.size() == 1) {
      loadChord(staff.goTo(selected_indices.get(0)));
      start = selected_indices.get(0);
      stop = start;
      position_list.ensureIndexIsVisible(start);

      if (!ui_component.equals("Library")) {
        chord.play(seqr, playback_mode);
      }
    } else if (selected_indices.size() > 1) {
      start = selected_indices.get(0);
      stop = selected_indices.get(selected_indices.size()-1);
    } else if (selected_indices.size() == 1) {
      start = selected_indices.get(0);
      stop = start;
    }
  }

  @Override
  public void keyPressed(KeyEvent event) {
    int k = event.getKeyCode();

    if (!held_keys.contains(k)) {
      //held_keys.add(k);
    }

    last_key_press = k;
    //System.out.println("status: "+getStatus());

    // Implement tab/F6 key behavior for moving system focus.
    if ((k == TAB || k == F6) && staff == null && ui_component.equals("Library")) {
      status.requestFocusInWindow();
      return;
    } else if ((k == TAB || k == F6) && staff == null && ui_component.equals("Status")) {
      lib_tree.requestFocusInWindow();
      return;
    } else if ((k == TAB || k == F6) && ui_component.equals("Library")) {
      position_list.requestFocusInWindow();
      return;
    } else if ((k == TAB || k == F6) && !ui_component.equals("Library")) {
      lib_tree.requestFocusInWindow();
      return;
    }

    // Implement behavior for function keys.
    switch(k) {
      case F1: {
        configOptions(EDIT_PLAYBACK_OPTIONS);
        return;
      } case F2: {
        configOptions(EDIT_TIME_SIGNATURE);
        return;
      } case F5: {
        configOptions(EDIT_LIBRARY_PATH);
        return;
      } case F11: {
        configMidiDevice(SELECT_DEVICES);
        return;
      }
    }

    // Implement space and CTRL key behavior for play/pause/stop.
    if (k == SPACE) {
      if (playing_staff != null && playing_staff.isPlaying()) {
        if (ui_component.equals("Library")) {
          playing_staff.stop(seqr);

          playing_song.syncTickPosition(
              playing_staff.getChord(playing_staff.getPosition()).getTick());

          if (staff == null || staff == playing_staff) {
            return;
          }
        }
      }

      if (ui_component.equals("Library")) {
        if (staff != null) {
          start = staff.getPosition();
          stop = staff.size()-1;
          staff.play(seqr, start, stop, Chord.SYNCHRONIZED);
          playing_staff = staff;
          playing_song = song;
        }
      } else if (ui_component.equals("Position")) {
        staff.play(seqr, start, stop, playback_mode);
      } else if (ui_component.equals("Note")) {
        note.play(seqr);
      }

      return; // End actions for space key.
    } else if (k == CTRL) {
      if (playing_staff != null && playing_staff.isPlaying()) {
        playing_staff.stop(seqr);

        playing_song.syncTickPosition(
            playing_staff.getChord(playing_staff.getPosition()).getTick());
      } else if (seqr != null && seqr.isRunning()) {
        seqr.stop();
      }

      return;
    }

    // Implement arrow keys behavior in position list and note list.
    if (staff != null && !ui_component.equals("Library")) {
      switch(k) {
        case RIGHT:
        case F: {
          if (staff.getPosition() < staff.size()-1) {
            loadChord(staff.moveForward());
          }

          if (ui_component.equals("Note")) {
            position_list.requestFocusInWindow();
          }

          if (held_keys.contains(SHIFT)) {
            chord.play(seqr, Chord.SYNCHRONIZED);
          } else {
            chord.play(seqr, playback_mode);
          }

          break;
        } case LEFT:
        case S: {
          if (staff.getPosition() > 0) {
            loadChord(staff.moveBack());
          }

          if (ui_component.equals("Note")) {
            position_list.requestFocusInWindow();
          }

          if (held_keys.contains(SHIFT)) {
            chord.play(seqr, Chord.SYNCHRONIZED);
          } else {
            chord.play(seqr, playback_mode);
          }

          break;
        } case UP:
        case E: {
          if (ui_component.equals("Note")) {
            if (chord.getIndex() > 0) {
              note = chord.moveUp();
              note.play(seqr);
            } else if (chord.getIndex() == 0) {
              position_list.requestFocusInWindow();
              playback_mode = Chord.OFFSET;
              chord.play(seqr, playback_mode);
              note_list.clearSelection();
            }
          } else if (ui_component.equals("Position")) {
            if (playback_mode == Chord.SYNCHRONIZED) {
              note_list.requestFocusInWindow();
              note_list.setSelectedIndex(chord.size()-1);
              note = chord.getNote(chord.size()-1);
              note.play(seqr);
            } else if (playback_mode == Chord.OFFSET) {
              playback_mode = Chord.SYNCHRONIZED;

              if (staff.isPlaying()) {
                staff.stop(seqr);
                staff.play(seqr, start, stop, playback_mode);
              } else {
                chord.play(seqr, playback_mode);
              }
            }
          }

          break;
        } case DOWN:
        case D: {
          if (ui_component.equals("Note")) {
            if (chord.getIndex() < chord.size()-1) {
              note = chord.moveDown();
              note.play(seqr);
            } else if (chord.getIndex() == chord.size()-1) {
              position_list.requestFocusInWindow();
              note_list.clearSelection();
              playback_mode = Chord.SYNCHRONIZED;
              chord.play(seqr, playback_mode);
            }
          } else if (ui_component.equals("Position")) {
            if (playback_mode == Chord.SYNCHRONIZED) {
              playback_mode = Chord.OFFSET;

              if (staff.isPlaying()) {
                staff.stop(seqr);
                staff.play(seqr, start, stop, playback_mode);
              } else {
                chord.play(seqr, playback_mode);
              }
            } else if (playback_mode == Chord.OFFSET) {
              note_list.requestFocusInWindow();
              note_list.setSelectedIndex(0);
              note = chord.getNote(0);
              note.play(seqr);
            }
          }
        }
      }
    }
  }

  @Override
  public void keyTyped(KeyEvent event) {}

  @Override
  public void keyReleased(KeyEvent event) {
    int k = event.getKeyCode();
    //held_keys.remove(k);
  }

  public static void configMidiDevice(int action) {
    try {
      if (action == SELECT_DEVICES) {
        JPanel device_panel = new JPanel();
        JOptionPane device_pane = new JOptionPane();
        MidiDevice.Info[] info_list = null;
        JList<MidiDevice.Info> seqr_devices = new JList<>();;
        DefaultListModel<MidiDevice.Info> seqr_model = new DefaultListModel<>();
        JList<MidiDevice.Info> midi_in_devices = new JList<>();
        DefaultListModel<MidiDevice.Info> midi_in_model = new DefaultListModel<>();
        JList<MidiDevice.Info> midi_out_devices = new JList<>();;
        DefaultListModel<MidiDevice.Info> midi_out_model = new DefaultListModel<>();

        for (MidiDevice.Info mdi : MidiSystem.getMidiDeviceInfo()) {
          MidiDevice md = MidiSystem.getMidiDevice(mdi);

          if (md instanceof Sequencer) {
            seqr_model.addElement(mdi);
          }

          if (md.getClass().getName().contains("MidiIn")){
            midi_in_model.addElement(mdi);
          }

          if (md instanceof Synthesizer || md.getClass().getName().contains("MidiOut")){
            midi_out_model.addElement(mdi);
          }
        }

        seqr_devices.getAccessibleContext().setAccessibleName("MIDI Sequencers");
        seqr_devices.setModel(seqr_model);
        midi_in_devices.getAccessibleContext().setAccessibleName("MIDI Input Device");
        midi_in_devices.setModel(midi_in_model);
        midi_out_devices.getAccessibleContext().setAccessibleName("MIDI Output Device");
        midi_out_devices.setModel(midi_out_model);

        if (seqr_model.size() > 1) {
          device_panel.add(new JLabel("Available MIDI Sequencers:"));
          device_panel.add(seqr_devices);
        } else if (seqr_model.size() == 1) {
          seqr_info = seqr_model.firstElement();
        }

        if (midi_out_model.size() > 1) {
          device_panel.add(new JLabel("Available MIDI Output Devices:"));
          device_panel.add(midi_out_devices);
        } else if (midi_out_model.size() == 1) {
          midi_out_info = midi_out_model.firstElement();
        }

        if (midi_in_model.size() > 0) {
          device_panel.add(new JLabel("Available MIDI Input Devices:"));
          device_panel.add(midi_in_devices);
        }

        if (device_panel.getComponentCount() == 0) {
          MessageDialog msg = new MessageDialog("MIDI Device Missing",
              lc_messages.getProperty("MainView.MIDI_Device_Missing"),
              JOptionPane.QUESTION_MESSAGE);

          int i = msg.showDialog(JOptionPane.YES_NO_OPTION);

          if (i == JOptionPane.YES_OPTION) {
            return;
          } else if (i == JOptionPane.NO_OPTION || i == JOptionPane.CLOSED_OPTION) {
            System.exit(0);
          }
        }

        device_pane = new JOptionPane(device_panel, JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION) {
              @Override
              public void selectInitialValue() {
                if (midi_out_model.size() > 0) {
                  midi_out_devices.requestFocusInWindow();

                  if (midi_out_info != null && midi_out_model.contains(midi_out_info)) {
                    midi_out_devices.setSelectedValue(midi_out_info, true);
                  } else if (midi_out_model.size() > 0) {
                    midi_out_devices.setSelectedIndex(0);
                  }
                }

                if (midi_in_model.size() > 0) {
                  if (midi_in_info != null && midi_in_model.contains(midi_in_info)) {
                    midi_in_devices.setSelectedValue(midi_in_info, true);
                  }
                }

                if (seqr_model.size() > 0) {
                  if (seqr_info != null && seqr_model.contains(seqr_info)) {
                    seqr_devices.setSelectedValue(seqr_info, true);
                  } else {
                    seqr_devices.setSelectedIndex(0);
                  }
                }
              }
            };

        while (true) {
          JDialog device_dialog = device_pane.createDialog("MIDI Device");
          device_dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
          device_dialog.setVisible(true);
          int i;

          if (device_pane.getValue() == null) {
            i = JOptionPane.CLOSED_OPTION;
          } else {
            i = (int) device_pane.getValue();
          }

          if (i == JOptionPane.OK_OPTION) {
            if (seqr != null && seqr.isOpen()) {
              seqr.close();
            }

            if (midi_in != null && midi_in.isOpen()) {
              midi_in.close();
            }

            if (midi_out != null && midi_out.isOpen()) {
              midi_out.close();
            }

            if (midi_in_model.size() > 0) { 
              midi_in_info = midi_in_devices.getSelectedValue();
            }

            seqr_info = seqr_devices.getSelectedValue();
            midi_out_info = midi_out_devices.getSelectedValue();
            break;
          } else if (seqr != null && midi_out != null) {
            return;
          } else if (i == JOptionPane.CANCEL_OPTION || i == JOptionPane.CLOSED_OPTION) {
            MessageDialog msg = new MessageDialog("MIDI Device Not Selected",
                lc_messages.getProperty("MainView.No_MIDI_Device_Selected"),
                JOptionPane.QUESTION_MESSAGE);

            i = msg.showDialog(JOptionPane.YES_NO_OPTION);

            if (i == JOptionPane.YES_OPTION) {
              return;
            } else if (i == JOptionPane.NO_OPTION || i == JOptionPane.CLOSED_OPTION) {
              continue;
            }
          }
        }
      }

      Transmitter trans = null;

      if (midi_in_info != null) {
        midi_in = MidiSystem.getMidiDevice(midi_in_info);
        midi_in.open();
        trans = midi_in.getTransmitter();
      }

      seqr = (Sequencer) MidiSystem.getMidiDevice(seqr_info);
      midi_out = MidiSystem.getMidiDevice(midi_out_info);
      seqr.open();
      midi_out.open();

      if (trans == null) {
        trans = seqr.getTransmitter();
      } else {
        //seqr.getTransmitter(trans);
      }

      Receiver rcv = midi_out.getReceiver();
      trans.setReceiver(rcv);
    } catch(Exception e) {
      System.out.println(e);
    }
  }

  public static void configOptions(int action) {
    while (true) {
      StringOptionsDialog config_pane = new StringOptionsDialog();
      LinkedHashMap<String, String> options = new LinkedHashMap<>();;

      try {
        if (action == EDIT_LIBRARY_PATH) {
          options.put("Library Path", lib_path);
          options.put("Tree Expansion Buffer", Integer.toString(tree_expansion_buffer));
          config_pane.setTitle("MIDI Library Options");
          config_pane.setOptions(options);
          Object[] choices = {"Refresh Library", "Cancel"};
          config_pane.setChoices(choices) ;
          int i = config_pane.showDialog();

          if (i != -1 && choices[i].equals("Refresh Library")) {
            String new_lib_path = options.get("Library Path");
            int new_buff = Integer.parseInt(options.get("Tree Expansion Buffer"));
            lib_dir = new File(new_lib_path);

            if (new_buff < 0) {
              throw new NumberFormatException("Tree expansion buffer cannot be less than 0");
            } else {
              tree_expansion_buffer = new_buff;
            }

            if (!lib_dir.exists() && !lib_dir.mkdir()) {
              throw new IOException("Failed to create directory.");
            } else {
              lib_path = new_lib_path;
            }

            refreshLibrary();
          } else if (lib_dir == null) {
            MessageDialog msg = new MessageDialog("MIDI Library Missing",
                lc_messages.getProperty("MainView.MIDI_Library_Missing"),
                JOptionPane.QUESTION_MESSAGE);

            i = msg.showDialog(JOptionPane.YES_NO_CANCEL_OPTION);

            if (i == JOptionPane.YES_OPTION) {
              return;
            } else if (i == JOptionPane.CANCEL_OPTION || i == JOptionPane.CLOSED_OPTION) {
              continue;
            } else if (i == JOptionPane.NO_OPTION) {
              System.exit(0);
            }
          }
        } else if (action == EDIT_TIME_SIGNATURE) {
          if (staff == null) {
            MessageDialog msg = new MessageDialog("No Staff Selected",
                lc_messages.getProperty("MainView.No_Staff_Selected"),
                JOptionPane.INFORMATION_MESSAGE);

            msg.showDialog();
            return;
          } else if (staff != song.getStaffList().get(0)) {
            options = staff.getConfigOptions();
            config_pane.setTitle("Time Signature: "+staff.toString()+" - "+song.toString());
            config_pane.setOptions(options);
            int i = config_pane.showDialog();

            if (i == JOptionPane.OK_OPTION) {
              staff.setConfigOptions(options);
              loadStaff();
            }
          } else {
            options = song.getConfigOptions();
            config_pane.setTitle("Time Signature for "+song.toString());
            config_pane.setOptions(options);
            int i = config_pane.showDialog();

            if (i == JOptionPane.OK_OPTION) {
              song.setConfigOptions(options);
              loadStaff();
            }
          }
        } else if (action == EDIT_PLAYBACK_OPTIONS) {
          options.put("Chord Offset (milliseconds)", Integer.toString(Chord.chord_offset));
          options.put("Offset Buffer (milliseconds)", Integer.toString(Chord.offset_buffer));
          options.put("Review Velocity", Integer.toString(Note.review_velocity));
          options.put("Loop Count", Integer.toString(Staff.loop_count));
          config_pane.setTitle("Playback Options");
          config_pane.setOptions(options);
          int i = config_pane.showDialog();

          if (i == JOptionPane.OK_OPTION) {
            int cho = Integer.parseInt(options.get("Chord Offset (milliseconds)"));
            int ofb = Integer.parseInt(options.get("Offset Buffer (milliseconds)"));
            int vel = Integer.parseInt(options.get("Review Velocity"));
            int lc = Integer.parseInt(options.get("Loop Count"));

            if (cho <= 0 || lc <= 0 || vel <= 0) {
              throw new NumberFormatException(
                  "Chord offset, review velocity, and loop count Values must be greater than 0.");
            }

            if (ofb < 0) {
              throw new NumberFormatException("Offset buffer cannot be negative.");
            }

            Staff.loop_count = lc;
            Chord.chord_offset = cho;
            Chord.offset_buffer = ofb;
            Note.review_velocity = vel;

            if (staff != null && staff.isPlaying()) {
              staff.play(seqr, start, stop, playback_mode);
            }
          }
        }

        break;
      } catch(NumberFormatException | IOException e) {
        MessageDialog msg = new MessageDialog(e.getClass().getName(), "Invalid input: "
            +e.getMessage(), JOptionPane.ERROR_MESSAGE);

        msg.showDialog();
      }
    }
  }

  public static void refreshLibrary() {
    staff = null;
    chord = null;
    note = null;
    start = 0;
    stop = 0;
    lib_dir = new File(lib_path);

    if ((lib_dir.exists() || lib_dir.mkdir()) && lib_tree != null) {
      lib_level = 0;
      AliasTreeNode lib_root = new AliasTreeNode(lib_dir, "MIDI Library", lib_level);
      createLibNodes(lib_root, lib_level+tree_expansion_buffer);
      DefaultTreeModel lib_model = new DefaultTreeModel(lib_root);
      lib_tree.setModel(lib_model); 
      lib_tree.requestFocusInWindow();
      lib_tree.setSelectionRow(0);
lib_tree.expandPath(new TreePath(lib_root));
      setStatus("No tracks loaded");
    }
  }

  @SuppressWarnings("unchecked")
  public static void loadStaff() {
    if (staff == null) {
      chord = null;
      note = null;
      start = 0;
      stop = 0;
      position_list.setModel(new DefaultListModel());
      note_list.setModel(new DefaultListModel());
      return;
    }

    start = staff.getPosition();
    stop = start;
    position_list.setModel(staff.getModel());
    staff.setChordTimeSignature();
    loadChord(staff.getChord(start));
  }

  @SuppressWarnings("unchecked")
  public static void loadChord(Chord c) {
    if (chord != null) {
      chord.resetIndex();
    }

    chord = c;
    note = c.getNote(chord.size()-1);
    note_list.setModel(c.getModel());
  }

  public static void createLibNodes(AliasTreeNode node, int end_level) {
    int level = node.level();

    if (level > end_level && tree_expansion_buffer > 0) {
      return;
    } else if (node.getChildCount() > 0) {
      for (int i = 0; i < node.getChildCount(); i++) {
        AliasTreeNode n = (AliasTreeNode) node.getChildAt(i);
        createLibNodes(n, end_level);
      }

      return;
    } else {
      level += 1;
    }

    Object obj = node.getUserObject();

    if (!(obj instanceof File)) {
      return;
    }

    File file_obj = (File) obj;

    for (File f : file_obj.listFiles()) {
      if (f.isDirectory()) {
        String node_name = f.getName()+"; level "+level;;
        AliasTreeNode dir_node = new AliasTreeNode(f, node_name, level);
        node.add(dir_node);
        createLibNodes(dir_node, end_level);
      } else if (f.isFile()) {
        node.add(createSongNode(f, level));
      }
    }
  }

  public static AliasTreeNode createSongNode(File f, int level) {
    try {
      Sequence seq = MidiSystem.getSequence(f);
      float div_type = seq.getDivisionType();
      int ppq = seq.getResolution();
      Song sg = new Song(seq.getTracks(), f.getName());
      int track_level = level+1;

      if (div_type == Sequence.PPQ) {
        sg.setResolution(ppq);
      } else {
        sg.setResolution(Staff.default_ppq);
      }

      AliasTreeNode s_node = new AliasTreeNode(sg, sg.toString()+"; level "+level, level);

      for (int i = 1; i < sg.getStaffList().size(); i++) {
        Staff s = sg.getStaffList().get(i);
        AliasTreeNode a = new AliasTreeNode(s, s.toString()+"; level "+track_level, track_level);
        s_node.add(a);
      }

      return s_node;
    } catch(InvalidMidiDataException | IOException e) {
      System.out.println("Error creating nodes for tracks.");
      System.out.println(e);
      return null;
    }
  }

  public static void setStatus(String s) {
    status.getAccessibleContext().setAccessibleName(s);
  }

  public static String getStatus() {
    return status.getAccessibleContext().getAccessibleName();
  }

  public static void createAndShowGUI() {
    frame = new JFrame("MIDI Mozart");
    frame.add(new MainView());
    frame.pack();
    frame.setVisible(true);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent event) {
        if (midi_in != null && midi_in.isOpen()) {
          midi_in.close();
        }

        if (midi_out != null && midi_out.isOpen()) {
          midi_out.close();
        }

        if (seqr != null && seqr.isOpen()) {
          seqr.close();
        }

        SaveConfig();
      }
    });

    lib_tree.setSelectionRow(0);
    DefaultTreeModel m = (DefaultTreeModel) lib_tree.getModel();
    lib_tree.expandPath(new TreePath(m.getRoot()));
    setStatus("No tracks loaded.");
  }

  public static void main(String[] args) {
    //String root_path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    lc_messages = new SortedStoreProperties();
    config = new SortedStoreProperties();

    try {
      lc_messages.load(new FileInputStream("lc_messages.properties"));
      config.load(new FileInputStream("config.properties"));
      loadConfig();
    } catch(IOException | NumberFormatException e) {
      if (e instanceof IOException) {
        MessageDialog msg = new MessageDialog("Configuration File Missing",
            lc_messages.getProperty("MainView.Configuration_File_Missing"),
            JOptionPane.INFORMATION_MESSAGE);

        msg.showDialog();
      } else if (e instanceof NumberFormatException) {
        MessageDialog msg = new MessageDialog("Error Loading Configuration",
            lc_messages.getProperty("MainView.Error_Loading_Configuration"),
            JOptionPane.QUESTION_MESSAGE);

        int i = msg.showDialog(JOptionPane.YES_NO_OPTION);

        if (i == JOptionPane.NO_OPTION) {
          System.exit(0);
        }
      }

      config = new SortedStoreProperties();
      loadConfig();
    }

    playback_mode = Chord.SYNCHRONIZED;
    held_keys = new ArrayList<>();
    lib_dir = new File(lib_path);

    if (!lib_dir.exists()) {
      lib_dir = null;
      configOptions(EDIT_LIBRARY_PATH);
    }

    if (seqr_info == null || midi_out_info == null) {
      configMidiDevice(SELECT_DEVICES);
    } else {
      configMidiDevice(LOAD_DEVICES);
    }

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }

  private static class AliasTreeNode extends DefaultMutableTreeNode {
    private String alias;
    private int level;
    private boolean has_expanded;

    public AliasTreeNode(Object userObject, String alias, int level) {
      super(userObject);
      this.alias = alias;
      this.level = level;
      has_expanded = false;
    }

    public void setAlias(String alias) {
      this.alias = alias;
      ((DefaultTreeModel) lib_tree.getModel()).nodeChanged(this);
    }

    public int level() {
      return level;
    }

    public void setExpanded(boolean b) {
      has_expanded = b;
    }

    public boolean hasExpanded() {
      return has_expanded;
    }

    @Override
    public String toString() {
      return (alias != null) ? alias : super.toString();
    }
  }

  private static class Song {
    private ArrayList<Staff> staff_list = new ArrayList<>();
    private ArrayList<Integer> numerator = new ArrayList<>();
    private ArrayList<Integer> denominator = new ArrayList<>();
    private ArrayList<Integer> ppq = new ArrayList<>();
    private ArrayList<Float> mpq = new ArrayList<>();;
    String description = "";

    public Song(Track[] track_list, String song_title) {
      Staff merged_staff = null;

      for (Track t : track_list) {
        Staff s = new Staff(t);

        if (s.size() > 0) {
          merged_staff = Staff.merge(merged_staff, s);
        }
      }

      merged_staff.setDescription(song_title);
      merged_staff.resetPosition();
      staff_list.add(merged_staff);
      int track_num = 1;
      String staff_name = "";

      for (Track t : track_list) {
        Staff s = new Staff(t);

        if (s.size() == 0) {
          continue;
        }

        if (track_num == 1 && track_list.length == 2) {
          staff_name = "Piano Right";
        } else if (track_num == 2 && track_list.length == 2) {
          staff_name = "Piano Left";
        } else {
          staff_name = "Staff "+track_num;
        }

        s.setDescription(staff_name);
        staff_list.add(s);
        track_num++;
      }
    }

    public void setResolution(int ppq) {
      for (Staff s : staff_list) {
        s.setResolution(ppq);
      }
    }

    public LinkedHashMap<String, String> getConfigOptions() {
      LinkedHashMap<String, String> options = new LinkedHashMap<>();
      String aggregated_numerator = "";
      String aggregated_denominator = "";
      String aggregated_ppq = "";
      String aggregated_mpq = "";

      for (int i = 0; i < staff_list.size(); i++) {
        Staff s = staff_list.get(i);

        if (i == 0) {
          LinkedHashMap<String, String> init_options = s.getConfigOptions();
          aggregated_numerator = init_options.get("Numerator");
          aggregated_denominator = init_options.get("Denominator");
          aggregated_ppq = init_options.get("Pulses/Ticks per Quarter Note (PPQ)");
          aggregated_mpq = init_options.get("Microseconds per Quarter Note (MPQ)");
          continue;
        }

        options = s.getConfigOptions();
        String numerator = options.get("Numerator");

        if (!numerator.equals(aggregated_numerator)) {
          aggregated_numerator += "; "+numerator;
        }

        String denominator = options.get("Denominator");

        if (!denominator.equals( aggregated_denominator)) {
          aggregated_denominator += "; "+denominator;;
        }

        String ppq = options.get("Pulses/Ticks per Quarter Note (PPQ)");

        if (!ppq.equals(aggregated_ppq)) {
          aggregated_ppq += "; "+ppq;
        }

        String mpq = options.get("Microseconds per Quarter Note (MPQ)");

        if (!mpq.equals(aggregated_mpq)) {
          aggregated_mpq += "; "+mpq;
        }
      }

      options.put("Numerator", aggregated_numerator);
      options.put("Denominator", aggregated_denominator);
      options.put("Pulses/Ticks per Quarter Note (PPQ)", aggregated_ppq);
      options.put("Microseconds per Quarter Note (MPQ)", aggregated_mpq);
      return options;
    }

    public void setConfigOptions(LinkedHashMap<String, String> options) {
      LinkedHashMap<String, String> prev_options = this.getConfigOptions();

      for (Staff s : staff_list) {
        String numerator = options.get("Numerator");
        String denominator = options.get("Denominator");
        String ppq = options.get("Pulses/Ticks per Quarter Note (PPQ)");
        String mpq = options.get("Microseconds per Quarter Note (MPQ)");

        if (!prev_options.get("Numerator").equals(numerator)) {
          s.setNumerator(Integer.parseInt(numerator));
        }

        if (!prev_options.get("Denominator").equals(denominator)) {
          s.setDenominator(Integer.parseInt(denominator));
        }

        if (!prev_options.get("Pulses/Ticks per Quarter Note (PPQ)").equals(ppq)) {
          s.setPPQ(Integer.parseInt(ppq));
        }

        if (!prev_options.get("Microseconds per Quarter Note (MPQ)").equals(mpq)) {
          s.setMPQ(Float.parseFloat(mpq));
        }
      }
    }

    public void syncTickPosition(long tick) {
      for (Staff s : staff_list) {
        while (s.getChord(s.getPosition()).getTick() < tick) {
          if (s.getPosition() == s.size()-1) {
            break;
          } else {
            s.moveForward();
          }
        }

        while (s.getChord(s.getPosition()).getTick() > tick) {
          if (s.getPosition() == 0) {
            break;
          } else {
            s.moveBack();
          }
        }
      }
    }

    public ArrayList<Staff> getStaffList() {
      return staff_list;
    }

    @Override
    public String toString() {
      return staff_list.get(0).toString();
    }
  }

  private static class FocusTextField extends JTextField {
    {
      addFocusListener(new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
          FocusTextField.this.selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
          FocusTextField.this.select(0, 0);
        }
      });
    }
  }

  private static class StringOptionsDialog {
    private LinkedHashMap<String, String> options = new LinkedHashMap<>();
    private ArrayList<FocusTextField> text_field_list = new ArrayList<>();
    private String title;
    private JDialog dialog;
    private JOptionPane pane;
    private JPanel option_panel;
    private Object[] choices;

    public StringOptionsDialog() {
      option_panel = new JPanel();
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public void setChoices(Object[] choices) {
      this.choices = choices;
    }

    public void setOptions(LinkedHashMap<String, String> options) {
      this.options = options;

      for (String key : options.keySet()) {
        String value = options.get(key);
        FocusTextField option = new FocusTextField();

        if (value.contains(";")) {
          option.getAccessibleContext().setAccessibleName(key+", multiple values");
        } else {
          option.getAccessibleContext().setAccessibleName(key);
        }

        option.setColumns(value.length()+1);
        option.setText(value);
        option_panel.add(new JLabel(key+":"));
        text_field_list.add(option);
        option_panel.add(option);
      }
    }

    public int showDialog() {
      if (choices == null) {
        pane = new JOptionPane(option_panel, JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION) {
              @Override
              public void selectInitialValue() {
                text_field_list.get(0).requestFocusInWindow();
              }
            };
      } else {
        pane = new JOptionPane(option_panel, JOptionPane.QUESTION_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION, null, choices, choices[0]) {
              @Override
              public void selectInitialValue() {
                text_field_list.get(0).requestFocusInWindow();
              }
            };
      }

      dialog = pane.createDialog(title);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
      int i = 0;

      for (String key :options.keySet()) {
        options.put(key, text_field_list.get(i).getText());
        i++;
      }

      if (choices != null) {
        for (int j = 0; j < choices.length; j++) {
          if (choices[j].equals(pane.getValue())) {
            return j;
          }
        }

        return -1;
      }

      int selection;

      if (pane.getValue() == null) {
        selection = JOptionPane.CLOSED_OPTION;
      } else {
        selection = (int) pane.getValue();
      }

      return selection;
    }
  }

  private static class MessageDialog {
    private String title;
    private int msg_type;
    private JLabel msg_label;
    private JDialog dialog;
    private JOptionPane pane;
    private JPanel msg_panel;

    public MessageDialog(String title, String msg, int msg_type) {
      this.title = title;
      this.msg_type = msg_type;
      msg_panel = new JPanel();
      msg_label = new JLabel(msg);
      msg_panel.add(msg_label);
    }

    public void showDialog() {
      pane = new JOptionPane(msg_panel, msg_type);
      dialog = pane.createDialog(title);
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);
    }

    public int showDialog(int option_type) {
      pane = new JOptionPane(msg_panel, msg_type, option_type);
      msg_label.setFocusable(true);
      dialog = pane.createDialog(title);

      dialog.addWindowListener(new WindowAdapter() {
          @Override
          public void windowActivated(WindowEvent event) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                msg_label.requestFocusInWindow();
              }
            });
          }
      });

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.setVisible(true);

      if (pane.getValue() == null) {
        return JOptionPane.CLOSED_OPTION;
      } else {
        return (int) pane.getValue();
      }
    }
  }

  private static class SortedStoreProperties extends Properties {
    @Override
    public void store(OutputStream out, String comments) throws IOException {
      Properties sorted_props = new Properties() {
        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
          /*
           * Using comparator to avoid the following exception on jdk >=9: 
           * java.lang.ClassCastException: java.base/java.util.concurrent.ConcurrentHashMap$MapEntry cannot be cast to java.base/java.lang.Comparable
           */
          Set<Map.Entry<Object, Object>> sorted_set = new TreeSet<Map.Entry<Object,
            Object>>(new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
              return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
          }
          );

          sorted_set.addAll(super.entrySet());
          return sorted_set;
        }

        @Override
        public Set<Object> keySet() {
          return new TreeSet<Object>(super.keySet());
        }

        @Override
        public synchronized Enumeration<Object> keys() {
          return Collections.enumeration(new TreeSet<Object>(super.keySet()));
        }

      };
      sorted_props.putAll(this);
      sorted_props.store(out, comments);
    }
  }
}
