Maven imports:
```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
	<dependency>
	    <groupId>com.github.vermulst</groupId>
	    <artifactId>MultiBreak</artifactId>
	    <version>master-98f06726fc-1</version>
	</dependency>
```


Gradle imports: 
```
repositories {
  maven {
    url = 'https://jitpack.io'
  }
}
dependencies {
  compileOnly 'com.github.vermulst:MultiBreak:version'
}
```

```
@EventHandler
public void multiBreakStart(MultiBreakStartEvent e) {
  FigureCircle figureCircle = new FigureCircle(10, 10, 3);
  e.setFigure(figureCircle);
  ArrayList<Block> blocks = e.getBlocks(); // get all of the blocks within the elipsoid
}

@EventHandler
public void multiBreakEnd(MultiBreakEndEvent e) {
  Player p = e.getPlayer();
  if (e.isSuccessful()) {
    p.sendMessage("Successfully broke multiple blocks");
  } else {
    p.sendMessage("Aborted breaking multiple blocks");
  }
  ArrayList<Block> blocks = e.getBlocks(); // get all of the blocks within the multibreak
}
```

[![](https://jitpack.io/v/vermulst/MultiBreak.svg)](https://jitpack.io/#vermulst/MultiBreak)
