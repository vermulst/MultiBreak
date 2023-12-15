package me.vermulst.multibreak.config;

import me.vermulst.multibreak.figure.Figure;

public class ConfigOption {

    private final String name;
    private final Figure figure;

    public ConfigOption(String name, Figure figure) {
        this.name = name;
        this.figure = figure;
    }


    public String getName() {
        return name;
    }

    public Figure getFigure() {
        return figure;
    }
}
