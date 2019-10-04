package org.therolf.OptymoNext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.therolf.OptymoNext.model.OptymoDirection;
import org.therolf.OptymoNext.model.OptymoNetwork;
import org.therolf.OptymoNext.model.OptymoStop;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

@SuppressWarnings({"unused", "WeakerAccess"})
public class FavWindow extends JFrame {

    private JTable table;
    private JComboBox<OptymoDirection> directionsComboBox;
    private OptymoDirection[] favorites;

    public FavWindow(OptymoNetwork network) throws HeadlessException {
        this.favorites = loadFavorites();

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

        Object[] columns = new Object[] { "Arret", "Ligne", "Direction", "Arrivée", "Supprimer"};
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columns);
        table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setEnabled(false);
        this.getContentPane().add(BorderLayout.CENTER, new JScrollPane(table));

        JButton refreshButton = new JButton("ACTUALISER");
        refreshButton.addActionListener(e -> updateNextTime());

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
        ((DefaultTableModel)table.getModel()).setNumRows(0);

        for(OptymoDirection direction : favorites) {
            ((DefaultTableModel) table.getModel()).addRow(new Object[] { direction.getStopName(), direction.getLineNumber(), direction.getDirection(), "soon", "x" });
        }
    }

    private void addFavorite(OptymoDirection newDirection) {
        if(newDirection != null) {
            OptymoDirection[] newFavorites = new OptymoDirection[this.favorites.length+1];
            System.arraycopy(this.favorites, 0, newFavorites, 0, this.favorites.length);
            newFavorites[this.favorites.length] = newDirection;
            this.favorites = newFavorites;
            // add to current list

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

        updateNextTime();
    }

    private OptymoDirection[] loadFavorites() {
        OptymoDirection[] result = new OptymoDirection[0];

        try {
            String jsonPath = Path.of("res/favorites.json").toAbsolutePath().toString();

            FileInputStream jsonInputStream = null;
            try {
                jsonInputStream = new FileInputStream(jsonPath);
            } catch (IOException ignored) {}

            if(jsonInputStream != null) {
                ArrayList<OptymoDirection> directionsList = new ArrayList<>();
                JSONArray array = new JSONArray(new String(jsonInputStream.readAllBytes()));

                for (int i = 0; i < array.length(); i++) {

                    JSONObject direction = array.getJSONObject(i);
                    directionsList.add(new OptymoDirection(direction.getInt("lineNumber"), direction.getString("direction"), direction.getString("stopName"), direction.getString("stopSlug")));
                }

                jsonInputStream.close();

                result = directionsList.toArray(new OptymoDirection[0]);
            }
        } catch (IOException ignored){}

        return result;
    }
}
