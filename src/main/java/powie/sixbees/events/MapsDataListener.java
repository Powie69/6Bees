package powie.sixbees.events;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.modules.NsfwBlock;

/**
 * <p>
 * This listener is separate from NsfwBlock because:
 * <ul>
 *   <li>Modules only subscribe to the event bus when its active</li>
 *   <li>I could have used onActivate, but also it fires on every GameJoinedEvent, which is too frequent</li>
 *   <li>We need to handle this event exactly once regardless of module state</li>
 * </ul>
 * 🤮🤮🤮🤮
 * <p>
 */
public class MapsDataListener {
    public MapsDataListener() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onNewMapsData(NewMapsDataEvent event) {
        NsfwBlock.NSFW_MAPS = event.mapIds;
        MeteorClient.EVENT_BUS.unsubscribe(this);
    }
}
