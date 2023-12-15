package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureCircle;
import me.vermulst.multibreak.figure.types.FigureLinear;

public enum FigureType {

    LINEAR {
        @Override
        public Figure build(int width, int height, int depth) {
            return new FigureLinear(width, height, depth);
        }
    },
    CIRCULAR {
        @Override
        public Figure build(int width, int height, int depth) {
            return new FigureCircle(width, height, depth);
        }
    };

    public abstract Figure build(int width, int height, int depth);

}
