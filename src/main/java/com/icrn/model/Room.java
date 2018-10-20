package com.icrn.model;

import io.reactivex.Observable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room implements Entity {
    private final Map<Movement,Long> allowedDirections = new HashMap<>();
    long roomLocation;
    private Long id;
    private String name;

    public String getName(){
        if (this.name == null)
            return id.toString();
        else
            return this.name;
    }

    public void setName(String name){ this.name = name; }

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

    public Observable<Map.Entry<Movement,Long>> AddRoom(Room otherRoom, Movement direction) {
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
}
