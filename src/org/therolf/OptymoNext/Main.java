package org.therolf.OptymoNext;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.therolf.OptymoNext.model.OptymoLine;
import org.therolf.OptymoNext.model.OptymoNetwork;
import org.therolf.OptymoNext.model.OptymoStop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

@SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
public class Main {

    public static void main(String[] args) {
//        System.out.println("Hello World!");
        new Main();
    }

    private String stopsJson = "res/network.json";
    private String linesXml = "res/belfort.xml";

    private JTable table;
    private JLabel label;

    private Main() {
        OptymoNetwork network = new OptymoNetwork();
        network.begin(stopsJson, linesXml, false);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 0, 10, 10));

        Object[] columns = new Object[] { "Ligne", "Direction", "Arrivée"};
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columns);
        table = new JTable(model);
        table.setEnabled(false);

        label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("sans-serif", Font.BOLD, 20));
        panel.add(BorderLayout.NORTH, label);
        panel.add(BorderLayout.CENTER, new JScrollPane(table));

        JScrollPane scrollPane = new JScrollPane();
        JList<OptymoStop> list = new JList<>(network.getStops());
        list.addListSelectionListener(e -> {
            this.displayTab(list.getSelectedValue());
        });
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
            if(stop != null) {
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

        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private void displayTab(OptymoStop stop) {
        displayTab(stop, 0);
    }

    private void displayTab(OptymoStop stop, int filter) {

        org.jsoup.nodes.Document doc;
        Elements errorTitle, directions, nextTimes, lines;
        doc = null;
        try {
            doc = Jsoup.connect("https://siv.optymo.fr/passage.php?ar=" + stop.getKey() + "&type=1").get();
        } catch (IOException ignored) {}

        if(doc != null) {
            errorTitle = doc.getElementsByTag("h3");
            ((DefaultTableModel)table.getModel()).setNumRows(0);
            if(errorTitle.size() == 0) {

                lines = doc.getElementsByClass("f1");
                directions = doc.getElementsByClass("f2");
                nextTimes = doc.getElementsByClass("f3");

                label.setText(stop.getName());
                for(int directionIndex = 0; directionIndex < directions.size(); directionIndex++) {
                    if(filter == 0 || filter == Integer.parseInt(lines.get(directionIndex).text())) {
                        ((DefaultTableModel) table.getModel()).addRow(new Object[]{ lines.get(directionIndex).text(), directions.get(directionIndex).text(), nextTimes.get(directionIndex).text()});
                    }
                }
            }
        }
    }
}
