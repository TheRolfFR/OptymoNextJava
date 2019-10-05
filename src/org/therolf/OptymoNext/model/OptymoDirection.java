package org.therolf.OptymoNext.model;

@SuppressWarnings("unused")
public class OptymoDirection {
    private int lineNumber;
    private String direction;
    private String stopName;
    private String stopKey;

    public String getStopName() {
        return stopName;
    }

    public String getStopKey() {
        return stopKey;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getDirection() {
        return direction;
    }

    public OptymoDirection(int lineNumber, String direction, String stopName, String stopKey) {
        this.lineNumber = lineNumber;
        this.direction = direction;
        this.stopName = stopName;
        this.stopKey = stopKey;
    }

    @Override
    public String toString() {
        return "[" + lineNumber + "] " + direction;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof OptymoDirection) {
            OptymoDirection dir = (OptymoDirection) obj;
            return this.lineNumber == dir.lineNumber && this.stopKey.equals(dir.stopKey) && this.direction.equals(dir.direction);
        }

        return super.equals(obj);
    }
}
