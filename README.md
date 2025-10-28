
# Releases
[**Spigot**](https://www.spigotmc.org/resources/multibreak-1-18-1-20.113810/)

[**Paper**](https://hangar.papermc.io/vermulst/MultiBreak)

[**Modrinth**](https://modrinth.com/plugin/multibreak)

# API usage

## Maven
```xml
<repository>
	<id>jitpack.io</id>
	<url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
	<groupId>com.github.vermulst</groupId>
	<artifactId>MultiBreak</artifactId>
	<version>v1.0.0</version>
	<scope>provided</scope>
</dependency>
```


## Gradle
```groovy
repositories {
	maven {
		url = 'https://jitpack.io'
	}
}
```
```groovy
dependencies {
	compileOnly 'com.github.vermulst:MultiBreak:v1.0.0'
}
```

## Events
```java

@EventHandler
public void multiBreakAllow(MultiBreakAllowEvent e) {
	if (Material.IRON_PICKAXE.equals(e.getItem().getType()) {
		e.setAllowed(true);
		// Now all iron pickaxes will call the MultiBreakStartEvent
	}
}

@EventHandler
public void multiBreakStart(MultiBreakStartEvent e) {
	Figure figure = new FigureLinear(3, 3, 1);
	e.setFigure(figure);

	// replaces list in the config
	e.includeOnly(EnumSet.of(Material.GRASS_BLOCK, Material.STONE, Material.DIRT));

	// add to list in the config
	e.exclude(Material.DIRT)

	List<Block> blocks = e.getBlocks(); // get all of the blocks within the elipsoid
}

@EventHandler
public void multiBreakEnd(MultiBreakEndEvent e) {
	Player p = e.getPlayer();
	if (e.isSuccessful()) {
		p.sendMessage("Successfully broke multiple blocks");
	} else {
		p.sendMessage("Aborted breaking multiple blocks");
	}
	List<Block> blocks = e.getBlocks(); // get all of the blocks within the multibreak
}
```

## Item data types

```java

public void giveItem() {
	.
	.
	// no need to use "e.setFigure(figure)" when applied to an itemstack
	MultiBreakAPI.setFigure(@NotNull ItemStack itemStack, @NotNull Figure figure)

	if (!MultiBreakAPI.hasFigure(@NotNull ItemStack itemStack)) return;
	Figure figure = MultiBreakAPI.getFigure(@NotNull ItemStack itemStack)
	figure.getWidth();
	// use for whatever you want (e.g. lore)
	.
	.
}
```

[![](https://jitpack.io/v/vermulst/MultiBreak.svg)](https://jitpack.io/#vermulst/MultiBreak)
