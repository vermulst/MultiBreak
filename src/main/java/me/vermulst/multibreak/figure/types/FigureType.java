package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.figure.Figure;

/** Keep order the same */
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
    },
    TRIANGULAR {
        @Override
        public Figure build(int width, int height, int depth) {
            return new FigureTriangle(width, height, depth);
        }
    };

    public abstract Figure build(int width, int height, int depth);

    public int getSize(int width, int height, int depth) {
        return switch (this) {
            case LINEAR -> width * height * depth;
            case CIRCULAR -> (width * height * depth * 11) / 21;
            case TRIANGULAR -> (width * height * depth) / 2;
            case null, default -> 0;
        };
    }
}
