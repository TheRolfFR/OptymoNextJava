package com.therolf.optymoNextDesktopApp;

import com.therolf.optymoNextModel.OptymoDirection;
import com.therolf.optymoNextModel.OptymoNetwork;
import com.therolf.optymoNextModel.OptymoNextTime;
import com.therolf.optymoNextModel.OptymoStop;
import com.therolf.optymoNextDesktopApp.vue.ButtonColumn;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Comparator;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FavWindow extends JFrame {

    private JTable table;
    private JComboBox<OptymoDirection> directionsComboBox;
    private ArrayList<OptymoDirection> favorites;
    private OptymoNetwork network;

    public FavWindow(OptymoNetwork network) throws HeadlessException {
        loadFavorites();

        this.network = network;
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.setTitle("Favoris");
        this.getContentPane().setLayout(new BorderLayout(10, 10));

        JPanel addFav = new JPanel(new BorderLayout(10, 10));
        JComboBox<OptymoStop> stopsComboBox = new JComboBox<>(network.getStops());
        stopsComboBox.addActionListener(e -> {
            OptymoStop stop = (OptymoStop) stopsComboBox.getSelectedItem();
            if(stop != null) {
                seeDirections(stop);
            }
        });
        directionsComboBox = new JComboBox<>();
        directionsComboBox.setEnabled(false);
        JButton addButton = new JButton("AJOUTER");
        addButton.addActionListener(e -> addFavorite((OptymoDirection) directionsComboBox.getSelectedItem()));
        addFav.add(BorderLayout.NORTH, stopsComboBox);
        addFav.add(BorderLayout.CENTER, directionsComboBox);
        addFav.add(BorderLayout.SOUTH, addButton);
        this.getContentPane().add(BorderLayout.NORTH, addFav);

        Object[] columns = new Object[] { "Arret", "Direction", "Arrivée", ""};
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columns);
        table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setEnabled(false);
        Action delete = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                JTable table = (JTable)e.getSource();
                int modelRow = Integer.valueOf( e.getActionCommand() );
                favorites.remove(modelRow);

                saveFavorites();
                updateNextTime();
            }
        };

        table.getColumnModel().getColumn(3).setMaxWidth(60);
        ButtonColumn buttonColumn = new ButtonColumn(table, delete, 3);
        this.getContentPane().add(BorderLayout.CENTER, new JScrollPane(table));

        JButton refreshButton = new JButton("ACTUALISER");
        refreshButton.addActionListener(e -> updateNextTime());
        this.getContentPane().add(BorderLayout.SOUTH, refreshButton);

        updateNextTime();

        this.pack();
        this.setVisible(true);
    }

    private void seeDirections(OptymoStop stop) {
        if(!directionsComboBox.isEnabled()) directionsComboBox.setEnabled(true);

        while (directionsComboBox.getItemCount() != 0) {
            directionsComboBox.removeItemAt(0);
        }

        OptymoDirection[] directions = stop.getAvailableDirections();
        for(OptymoDirection direction : directions) {
            directionsComboBox.addItem(direction);
        }
    }

    private void updateNextTime() {
        favorites.sort(Comparator.comparing(OptymoDirection::getStopName));

        if(!table.isEnabled()) table.setEnabled(true);
        ((DefaultTableModel)table.getModel()).setNumRows(0);

        String nextTimeString;
        int i;
        for (OptymoDirection direction : favorites) {
            nextTimeString = "";
            OptymoStop stop = network.getStopBySlug(direction.getStopSlug());
            if(stop != null) {
                OptymoNextTime[] nextTimes = new OptymoNextTime[0];
                try {
                    nextTimes = stop.getNextTimes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                i = 0;
                while(nextTimeString.equals("") && i < nextTimes.length) {
                    if(direction.equals(nextTimes[i])) {
                        nextTimeString = nextTimes[i].getNextTime();
                    }
                    ++i;
                }
            }
            ((DefaultTableModel) table.getModel()).addRow(new Object[]{direction.getStopName(), direction.toString(), nextTimeString, "x"});
        }
    }

    private void addFavorite(OptymoDirection newDirection) {
        if(newDirection != null) {
            // add it
            this.favorites.add(newDirection);

            // save them
            this.saveFavorites();
        }

        updateNextTime();
    }

    private void saveFavorites() {
        JSONWriter stringer = new JSONStringer().array();
        for (OptymoDirection favorite : this.favorites) {
            stringer.object()
                    .key("lineNumber")
                    .value(favorite.getLineNumber())
                    .key("direction")
                    .value(favorite.getDirection())
                    .key("stopName")
                    .value(favorite.getStopName())
                    .key("stopSlug")
                    .value(favorite.getStopSlug())
                    .endObject();
        }

        String finalString = stringer.endArray().toString();

        try {
            FileWriter writer = new FileWriter("res/favorites.json");
            writer.append(finalString);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadFavorites() {
        this.favorites = new ArrayList<>();

        try {
            String jsonPath = FileSystems.getDefault().getPath("res", "favorites.json").toAbsolutePath().toString();

            FileInputStream jsonInputStream = null;
            try {
                jsonInputStream = new FileInputStream(jsonPath);
            } catch (IOException ignored) {}

            if(jsonInputStream != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                byte[] buffer = new byte[0xFFFF];
                for (int len = jsonInputStream.read(buffer); len != -1; len = jsonInputStream.read(buffer)) {
                    os.write(buffer, 0, len);
                }

                JSONArray array = new JSONArray(new String(os.toByteArray()));

                for (int i = 0; i < array.length(); i++) {

                    JSONObject direction = array.getJSONObject(i);
                    this.favorites.add(new OptymoDirection(direction.getInt("lineNumber"), direction.getString("direction"), direction.getString("stopName"), direction.getString("stopSlug")));
                }

                jsonInputStream.close();
            }
        } catch (IOException ignored){}
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if(b) {
            updateNextTime();
        }
    }
}
