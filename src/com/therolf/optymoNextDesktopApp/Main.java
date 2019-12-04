package com.therolf.optymoNextDesktopApp;

import com.therolf.optymoNextModel.OptymoLine;
import com.therolf.optymoNextModel.OptymoNetwork;
import com.therolf.optymoNextModel.OptymoNextTime;
import com.therolf.optymoNextModel.OptymoStop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;

@SuppressWarnings({"FieldCanBeLocal", "ResultOfMethodCallIgnored"})
public class Main {

    public static void main(String[] args) {
//        System.out.println("Hello World!");
        new Main();
    }

    private String stopsJson = "res/network.json";
    private String linesXml = "res/belfort.xml";

    private JTable table;
    private JLabel label;

    private FavWindow favWindow = null;

    private Main() {
        Instant start = Instant.now();
        OptymoNetwork network = new OptymoNetwork();
        File file = new File(stopsJson);
        InputStream is;
        try {
            FileInputStream jsonInputStream = new FileInputStream(file);
            byte[] jsonBytes = new byte[jsonInputStream.available()];
            jsonInputStream.read(jsonBytes);

            is = new FileInputStream(linesXml);
            network.begin(new String(jsonBytes), is, false);

            if(!network.getResultJson().equals("")) {
                FileOutputStream fos = new FileOutputStream(stopsJson);
                fos.write(network.getResultJson().getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

//your code
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: "+ timeElapsed.toMillis() +" milliseconds");

        new Thread(() -> {
            while (true) {
                System.out.println("starting improving network");
                String resultJson  = network.improveNetwork();
                System.out.println("stopped improving network");
                if(resultJson != null && !resultJson.equals("")) {
                    FileOutputStream fos; // = null
                    try {
                        fos = new FileOutputStream(stopsJson);
                        fos.write(network.getResultJson().getBytes());
                        System.out.println("new network generated at " + Calendar.getInstance().getTime());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000*60*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 0, 10, 10));

        Object[] columns = new Object[] { "Ligne", "Direction", "Arrivée"};
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columns);
        table = new JTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setEnabled(false);

        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("sans-serif", Font.BOLD, 20));
        panel.add(BorderLayout.NORTH, label);
        panel.add(BorderLayout.CENTER, new JScrollPane(table));

        JScrollPane scrollPane = new JScrollPane();
        JList<OptymoStop> list = new JList<>(network.getStops());
        list.addListSelectionListener(e -> this.displayTab(list.getSelectedValue()));
        scrollPane.setViewportView(list);

        // search for line then for stop
        JPanel lineSearch = new JPanel(new BorderLayout(10, 10));
        JComboBox<OptymoLine> linesComboBox = new JComboBox<>(network.getLines());
        JComboBox<OptymoStop> stopsComboBox = new JComboBox<>();
        stopsComboBox.setEnabled(false);
        lineSearch.add(BorderLayout.NORTH, new JLabel("Search by line"));
        lineSearch.add(BorderLayout.CENTER, linesComboBox);
        lineSearch.add(BorderLayout.SOUTH, stopsComboBox);
        linesComboBox.addActionListener(e -> {
            OptymoLine line = (OptymoLine) linesComboBox.getSelectedItem();
            if(line != null) {
                while (stopsComboBox.getItemCount() != 0) {
                    stopsComboBox.removeItemAt(0);
                }
                for(OptymoStop stop : line.getStops()) {
                    stopsComboBox.addItem(stop);
                }
                stopsComboBox.setEnabled(true);
            }
        });
        stopsComboBox.addActionListener(e -> {
            OptymoStop stop = (OptymoStop) stopsComboBox.getSelectedItem();
            if(stop != null && linesComboBox.getSelectedItem() != null) {
                displayTab(stop, ((OptymoLine) linesComboBox.getSelectedItem()).getNumber());
            }
        });

        String[] packages = this.getClass().getPackage().toString().split("\\.");
        JFrame frame = new JFrame(packages[packages.length -1]);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(10, 10));

        frame.getContentPane().add(BorderLayout.CENTER, panel);

        frame.getContentPane().add(BorderLayout.WEST, scrollPane);

        frame.getContentPane().add(BorderLayout.NORTH, lineSearch);

        JButton favButton = new JButton("FAVORIS");
        favButton.addActionListener(e -> displayFavWindow(network));
        frame.getContentPane().add(BorderLayout.SOUTH, favButton);

        frame.pack();
        frame.setSize(800, 600);
//        frame.setVisible(true);
    }

    private void displayFavWindow(OptymoNetwork network) {
        if(favWindow == null) {
            favWindow = new FavWindow(network);
        } else {
            if(!favWindow.isVisible()) {
                favWindow.setVisible(true);
            }
        }
    }

    private void displayTab(OptymoStop stop) {
        displayTab(stop, 0);
    }

    private void displayTab(OptymoStop stop, int lineFilter) {
        ((DefaultTableModel)table.getModel()).setNumRows(0);

        OptymoNextTime[] nextTimes = new OptymoNextTime[0];
        try {
            nextTimes = stop.getNextTimes(lineFilter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (OptymoNextTime nextTime : nextTimes) {
            ((DefaultTableModel) table.getModel()).addRow(new Object[]{"" + nextTime.getLineNumber(), nextTime.getDirection(), nextTime.getNextTime()});
        }

        label.setText(stop.getName());
    }
}
