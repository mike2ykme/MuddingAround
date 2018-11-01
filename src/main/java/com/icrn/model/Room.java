package com.icrn.model;

import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.HashMap;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room implements Entity {
    private final Map<Movement,Long> allowedDirections = new HashMap<>();
//    long roomLocation;
    private Long id;
    private String name;
    private MobInfo mobInfo;
    private boolean safeZone;
    public String getName(){
        if (this.name == null)
            return "ROOM ID:" + id.toString();
        else
            return this.name;
    }

    public long getRoomLocation(){
        return this.id;
    }
    public void setName(String name){ this.name = name; }

    @Override
    public boolean isStatsBased() {
        return false;
    }

    public Room(Long id){ this.id = id; }

    @Override
    public EntityType getType() {
        return EntityType.ROOM;
    }

    public boolean allowsMovement(Movement movement) {
        return this.allowedDirections.containsKey(movement);
    }
    
    public void addRoomDirection(Movement movement, Long otherRoomId){
        this.allowedDirections.put(movement,otherRoomId);
    }
    public long getRoomIdFromDirection(Movement movement){
        return this.allowedDirections.get(movement);
    }

    public Observable<Map.Entry<Movement,Long>> addRoom(Room otherRoom, Movement direction) {
        return Observable.create(observableEmitter -> {
            this.getAllowedDirections().put(direction,otherRoom.getId());
            otherRoom.getAllowedDirections().put(Movement.reverse(direction),this.getId());

            this.getAllowedDirections()
                    .entrySet()
                    .stream()
                    .forEach(observableEmitter::onNext);

            observableEmitter.onComplete();
        });
    }

    public static Room makeTrapRoom(){
        return makeTrapRoom(100L);
    }
    public static Room makeTrapRoom(long ROOM_NUMBER){
//        val ROOM_NUMBER = 100L;
        val ROOM_NAME = "TRAP ROOM";
        val HP = 10;
        val STR = 10;
        val CON = 10;
        val DEX = 10;
        val MAX_HP = 25;
        val SAFE_ZONE = false;

        val MOB_COUNT = 2L;
        val MOB_NAME = "MOB";

        val builder = StatsBasedEntityTemplate.builder();
        val TEMPLATE = builder.room(ROOM_NUMBER)
                .STR(STR)
                .maxHP(MAX_HP)
                .HP(HP)
                .DEX(DEX)
                .CON(CON)
                .build();

        val mobInfo = MobInfo.of(MOB_COUNT,MOB_NAME,TEMPLATE);
        val room = new Room(ROOM_NUMBER);

        room.setSafeZone(SAFE_ZONE);
        room.setMobInfo(mobInfo);
        room.setName(ROOM_NAME);

        return room;

    }

}
