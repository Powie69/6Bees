package powie.sixbees.events;

import meteordevelopment.meteorclient.events.Cancellable;

public class SendCommandEvent extends Cancellable {
    private static final SendCommandEvent INSTANCE = new SendCommandEvent();

    public String command;

    public static SendCommandEvent get(String command) {
        INSTANCE.setCancelled(false);
        INSTANCE.command = command;
        return INSTANCE;
    }
}
