package mindustry.world.blocks.storage;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.entities.units.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import java.io.*;

import static mindustry.Vars.content;

public class Unloader extends Block{
    public float speed = 1f;
    public final int timerUnload = timers++;

    private static Item lastItem;

    public Unloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 70;
        hasItems = true;
        configurable = true;
        entityType = UnloaderEntity::new;
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.item(req.config), "unloader-center");
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return !(to.block() instanceof StorageBlock);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastItem != null){
            tile.configure(lastItem.id);
        }
    }

    @Override
    public void configured(Tile tile, Playerc player, Object value){
        tile.entity.items().clear();
        tile.<UnloaderEntity>ent().sortItem = content.item(value);
    }

    @Override
    public void update(Tile tile){
        UnloaderEntity entity = tile.ent();

        if(tile.entity.timer(timerUnload, speed / entity.timeScale()) && tile.entity.items().total() == 0){
            for(Tile other : tile.entity.proximity()){
                if(other.interactable(tile.team()) && other.block().unloadable && other.block().hasItems && entity.items().total() == 0 &&
                ((entity.sortItem == null && other.entity.items().total() > 0) || hasItem(other, entity.sortItem))){
                    offloadNear(tile, removeItem(other, entity.sortItem));
                }
            }
        }

        if(entity.items().total() > 0){
            tryDump(tile);
        }
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    private Item removeItem(Tile tile, Item item){
        Tilec entity = tile.entity;

        if(item == null){
            return entity.items().take();
        }else{
            if(entity.items().has(item)){
                entity.items().remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    private boolean hasItem(Tile tile, Item item){
        Tilec entity = tile.entity;
        if(item == null){
            return entity.items().total() > 0;
        }else{
            return entity.items().has(item);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        UnloaderEntity entity = tile.ent();

        Draw.color(entity.sortItem == null ? Color.clear : entity.sortItem.color);
        Draw.rect("unloader-center", tile.worldx(), tile.worldy());
        Draw.color();
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        UnloaderEntity entity = tile.ent();
        ItemSelection.buildTable(table, content.items(), () -> entity.sortItem, item -> {
            lastItem = item;
            tile.configure(item == null ? -1 : item.id);
        });
    }

    public static class UnloaderEntity extends TileEntity{
        public Item sortItem = null;

        @Override
        public int config(){
            return sortItem == null ? -1 : sortItem.id;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(sortItem == null ? -1 : sortItem.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte id = read.b();
            sortItem = id == -1 ? null : content.items().get(id);
        }
    }
}
