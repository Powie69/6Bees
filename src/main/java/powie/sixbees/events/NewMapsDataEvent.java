package powie.sixbees.events;

import java.util.Set;

public class NewMapsDataEvent {
    public Set<Integer> mapIds;

    public NewMapsDataEvent(Set<Integer> mapIds) {
        this.mapIds = mapIds;
    }
}
