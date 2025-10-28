package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.figure.types.FigureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.*;

public abstract class Figure {

    private final int width;
    private final int height;
    private final int depth;

    private int offSetWidth = 0;
    private int offSetHeight = 0;
    private int offSetDepth = 0;

    private short rotationWidth = 0;
    private short rotationHeight = 0;
    private short rotationDepth = 0;

    public Figure(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public abstract Set<Vector> getVectors(boolean rotated);
    public abstract FigureType getFigureType();



    public Set<Vector> applyRotation(Set<Vector> vectors, Figure figure) {
        HashSet<Vector> rotatedVectors = new HashSet<>();
        Matrix4x4 rotationMatrix = new Matrix4x4();
        rotationMatrix.setRotationX(figure.getRotationWidth() * (Math.PI / 180));
        rotationMatrix.setRotationY(figure.getRotationHeight() * (Math.PI / 180));
        rotationMatrix.setRotationZ(figure.getRotationDepth() * (Math.PI / 180));

        for (Vector vector : vectors) {
            rotationMatrix.transform(vector);
            vector.setX(Math.round(vector.getX()));
            vector.setY(Math.round(vector.getY()));
            vector.setZ(Math.round(vector.getZ()));
            rotatedVectors.add(new Vector(Math.round(vector.getX()), Math.round(vector.getY()), Math.round(vector.getZ())));
        }
        return rotatedVectors;
    }



    public void setOffsets(int offSetWidth, int offSetHeight, int offSetDepth) {
        this.offSetWidth = offSetWidth;
        this.offSetHeight = offSetHeight;
        this.offSetDepth = offSetDepth;
    }

    public void setRotations(short rotationWidth, short rotationHeight, short rotationDepth) {
        this.rotationWidth = rotationWidth;
        this.rotationHeight = rotationHeight;
        this.rotationDepth = rotationDepth;
    }

    public List<TextComponent> getLore() {
        List<TextComponent> lore = new ArrayList<>();


        TextComponent widthC = Component.text("Width: ", NamedTextColor.GRAY);
        TextComponent heightC = Component.text("Height: ", NamedTextColor.GRAY);
        TextComponent depthC = Component.text("Depth: ", NamedTextColor.GRAY);

        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Size")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.UNDERLINED, true)
                .decoration(TextDecoration.BOLD, true)
                .build());

        // Add dimensions
        lore.add(widthC
                .append(Component.text(this.getWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getDepth(), NamedTextColor.YELLOW)));


        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Rotations")
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.UNDERLINED, true)
                .build());

        lore.add(widthC
                .append(Component.text(this.getRotationWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getRotationHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getRotationDepth(), NamedTextColor.YELLOW)));


        lore.add(Component.empty());
        lore.add(Component.text()
                .content("Offsets")
                .color(TextColor.color(255, 255, 255))
                .decoration(TextDecoration.UNDERLINED, true)
                .build());

        lore.add(widthC
                .append(Component.text(this.getOffSetWidth(), NamedTextColor.YELLOW)));
        lore.add(heightC
                .append(Component.text(this.getOffSetHeight(), NamedTextColor.YELLOW)));
        lore.add(depthC
                .append(Component.text(this.getOffSetDepth(), NamedTextColor.YELLOW)));

        return lore;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int getOffSetWidth() {
        return offSetWidth;
    }

    public int getOffSetHeight() {
        return offSetHeight;
    }

    public int getOffSetDepth() {
        return offSetDepth;
    }

    public short getRotationWidth() {
        return rotationWidth;
    }

    public short getRotationHeight() {
        return rotationHeight;
    }

    public short getRotationDepth() {
        return rotationDepth;
    }

}
