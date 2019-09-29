package org.therolf.OptymoNext.model;

public class Stop implements Comparable<Stop> {
    String key;
    String name;

    public Stop(String key, String name) {
        this.key = key;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Stop o) {
        return key.compareTo(o.getKey());
    }
}
