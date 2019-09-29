package org.therolf.OptymoNext.model;

@SuppressWarnings("unused")
public class OptymoStop implements Comparable<OptymoStop> {
    private String key;
    private String name;
    private OptymoLine[] lines;

    @SuppressWarnings("WeakerAccess")
    public OptymoStop(String key, String name) {
        this.key = key;
        this.name = name;
        this.lines = new OptymoLine[0];
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public OptymoLine[] getLines() {
        return lines;
    }

    public void addLineToStop(OptymoLine line) {
        OptymoLine[] newTable = new OptymoLine[lines.length+1];
        System.arraycopy(lines, 0, newTable, 0, lines.length);
        newTable[lines.length] = line;

        lines = newTable;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(OptymoStop o) {
        return key.compareTo(o.key);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof OptymoStop) {
            OptymoStop other = (OptymoStop) obj;
            return other.getKey().equals(this.getKey()) && this.getName().equals(other.getName());
        }
        return super.equals(obj);
    }
}
