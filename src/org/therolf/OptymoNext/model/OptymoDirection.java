package org.therolf.OptymoNext.model;

@SuppressWarnings("unused")
public class OptymoDirection {
    private int lineNumber;
    private String direction;
    private String stopName;
    private String stopSlug;

    public String getStopName() {
        return stopName;
    }

    public String getStopSlug() {
        return stopSlug;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getDirection() {
        return direction;
    }

    public OptymoDirection(int lineNumber, String direction, String stopName, String stopSlug) {
        this.lineNumber = lineNumber;
        this.direction = direction;
        this.stopName = stopName;
        this.stopSlug = stopSlug;
    }

    @Override
    public String toString() {
        return "[" + lineNumber + "] " + direction;
    }
}
