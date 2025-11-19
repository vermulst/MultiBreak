
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
	<version>v1.2.0</version>
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
	compileOnly 'com.github.vermulst:MultiBreak:v1.1.0-prerelease'
}
```

## Events
```java

@EventHandler
public void figureRequest(FetchFigureEvent e) {
	if (!Material.IRON_PICKAXE.equals(e.getItem().getType())) return;
	Figure figure = e.getFigure(); // get figure on the tool
	Figure newFigure = new FigureLinear(3, 3, 1);
	e.setFigure(newFigure);
}

@EventHandler
public void filterBlocks(FilterBlocksEvent e) {
	e.includeOnly(EnumSet.of(Material.GRASS_BLOCK, Material.STONE, Material.DIRT)); // replace included list
	e.exclude(Material.DIRT); // add this to the excluded list
}

@EventHandler
public void multiBreakStart(MultiBreakStartEvent e) {
	Figure figure = e.getFigure(); // get the figure being used
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

public void giveItem(Player p) {
    ItemStack itemStack = new ItemStack(Material.DIAMOND_PICKAXE);
    Figure square_3x3x1 = new FigureLinear(3, 3, 1);
    
    // ***
    
    // this figure will be passed into the FetchFigureEvent
    // you can also set it directly in the event if you want to use your own datatype
    MultiBreakAPI.setFigure(itemStack, square_3x3x1);

    // ***
    
    if (!MultiBreakAPI.hasFigure(itemStack)) return; // return true
    Figure figure = MultiBreakAPI.getFigure(itemStack);
    figure.getWidth(); // use for whatever you want (e.g. lore)
	
    // ***
    
    p.getInventory().setItemInMainHand(itemStack);
    MultiBreakAPI.updateTool(p); // use this when setting the main hand to a multibreak tool
    
    // ***
}
```

[![](https://jitpack.io/v/vermulst/MultiBreak.svg)](https://jitpack.io/#vermulst/MultiBreak)
