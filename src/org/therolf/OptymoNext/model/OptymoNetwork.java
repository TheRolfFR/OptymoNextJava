package org.therolf.OptymoNext.model;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

@SuppressWarnings({"unused"})
public class OptymoNetwork {
    private HashMap<String, OptymoLine> lines;
    private HashMap<String, OptymoStop> stops;

    // https://www.optymo.fr/wp-admin/admin-ajax.php?action=getLine&param=2 trace des lignes
    // https://www.optymo.fr/wp-admin/admin-ajax.php?action=getItrBus&src=itrsub/get_markers_urb.php c est pour suivre les bus
    // https://app.mecatran.com/utw/ws/gtfs/stops/belfort?includePatterns=false&apiKey=2c643c5655034f467a070f5f7028613c2a3c6c71&includeStops=true // les stops (XML)
    // https://siv.optymo.fr/passage.php?ar=technhom1utbm&type=1 (offre passage)

    public OptymoNetwork() {
        lines = new HashMap<>();
        stops = new HashMap<>();
    }

    public void begin(String stopsJson, String linesXml) {
        begin(stopsJson, linesXml, false);
    }
    public void begin(String stopsJson, String linesXml, boolean forceXml) {
        if(forceXml || !decodeJSON(stopsJson)) {
            generateFromXML(stopsJson,linesXml);
        }
    }

    /**
     * decodes the network from json
     * @param stopsJson relative path to the json
     * @return whever it worked or not
     */
    private boolean decodeJSON(String stopsJson) {
        boolean returnValue = false;

        try {
            String jsonPath = Path.of(stopsJson).toAbsolutePath().toString();

            FileInputStream jsonInputStream = null;
            try {
                jsonInputStream = new FileInputStream(jsonPath);
            } catch (IOException ignored) {}

            if(jsonInputStream != null) {
                JSONArray array = new JSONArray(new String(jsonInputStream.readAllBytes()));
                for (int i = 0; i < array.length(); i++) {

                    JSONObject stopObject = array.getJSONObject(i);
                    for(String key : stopObject.keySet()) {
                        JSONObject stop = stopObject.getJSONObject(key);

                        addOptymoStop(stop.getString("name"));
                        JSONArray directions = stop.getJSONArray("directions");
                    }
                }

                jsonInputStream.close();
            }
            returnValue = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return returnValue;
    }

    private void generateFromXML(String stopsJson, String linesXml) {
        System.out.println("on regenere tout");
        try {
            String path = Path.of(linesXml).toAbsolutePath().toString();
            File file = new File(path);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(file);

            NodeList names = document.getElementsByTagName("name");
            Elements lines, directions;
            String cleanedName, name;

            JSONWriter stringer = new JSONStringer().array();

            for(int i = 0; i < names.getLength(); i++) {
                name = names.item(i).getTextContent();
                cleanedName = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^A-Za-z0-9]", "").toLowerCase();

                org.jsoup.nodes.Document doc = null;
                try {
                    doc = Jsoup.connect("https://siv.optymo.fr/passage.php?ar=" + cleanedName + "&type=1").get();
                } catch (IOException ignored) {}
                if(doc != null && doc.getElementsByTag("h3").size() == 0) {
                    System.out.println("" + (float) i / names.getLength() * 100 + "%");
                    stringer = stringer
                            .object()
                                .key(cleanedName)
                                .object()
                                    .key("name").value(name)
                                    .key("directions").array();

                                        lines = doc.getElementsByClass("f1");
                                        directions = doc.getElementsByClass("f2");

                                        for(int a  = 0; a < lines.size(); ++a) {
                                            stringer = stringer.value(lines.get(a).text() + " - " + directions.get(a).text());
                                        }

                                    stringer = stringer.endArray()
                                .endObject()
                            .endObject();
                    addOptymoStop(name);
                }
            }

            String finalString  = stringer.endArray().toString();
            FileWriter writer = new FileWriter(stopsJson);
            writer.append(finalString);
            writer.flush();
            writer.close();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that adds the stops to the stops list and the given line
     * @param stopName the name of the stops
     */
    private void addOptymoStop(String stopName) {
        String cleanedName = Normalizer.normalize(stopName, Normalizer.Form.NFD).replaceAll("[^A-Za-z0-9]", "").toLowerCase();

        org.jsoup.nodes.Document doc = null;
        try {
            doc = Jsoup.connect("https://siv.optymo.fr/passage.php?ar=" + cleanedName + "&type=1").get(); // get the page
        } catch (IOException ignored) {}
        if(doc != null && doc.getElementsByTag("h3").size() == 0) { // if not error page
            OptymoStop newStop = new OptymoStop(cleanedName, stopName); // create a new stop
            stops.put(cleanedName, newStop); // add the stop to the global list

            // add the new lines to the global list
            Elements lines = doc.getElementsByClass("f1"), directions = doc.getElementsByClass("f2");
            for(int i = 0; i < lines.size(); ++i) {
                OptymoLine l = getLine(lines.get(i).text(), directions.get(i).text());
                if(l != null) {
                    l.addStopToLine(newStop); // add the stop to the line
                }
            }
        }
    }

    /**
     * Add or get a line given its lin number and direction
     * @param lineNumber the number of the line
     * @param lineDirection the direction of the line
     * @return the given line
     */
    private OptymoLine getLine(String lineNumber, String lineDirection) {
        String key =  lineNumber + lineDirection; // create the key

        if(lines.containsKey(key)) { // if the line already exists
            return lines.get(key); // get the line
        } else { // else
            if(!lineDirection.equals("Dépôt")) {
                OptymoLine newLine = new OptymoLine(Integer.parseInt(lineNumber), lineDirection); // create it
                lines.put(key, newLine); // and add it to the global list

                return newLine; // then return the added line
            }
        }

        return null;
    }

    public OptymoStop[] getStops() {
        ArrayList<OptymoStop> list = new ArrayList<>(this.stops.values());
        Collections.sort(list);
        return list.toArray(new OptymoStop[0]);
    }

    public OptymoLine[] getLines() {
        ArrayList<OptymoLine> list = new ArrayList<>(this.lines.values());
        Collections.sort(list);
        System.err.println(list.size());
        return list.toArray(new OptymoLine[0]);
    }

    public OptymoStop getStopByKey(String key) {
        OptymoStop result = null, tmp;
        int i = 0;
        Object[] keys = stops.keySet().toArray();
        do
        {
            //noinspection SuspiciousMethodCalls
            tmp = stops.get(keys[i]);
            if(tmp.getKey().equals(key)) {
                result = tmp;
            }

            ++i;
        } while (result == null && i < keys.length);

        return result;
    }
}
