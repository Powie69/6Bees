package powie.sixbees.events;

import meteordevelopment.meteorclient.events.Cancellable;

// TODO: Maybe add separate fields for command and arguments
public class SendCommandEvent extends Cancellable {
    private static final SendCommandEvent INSTANCE = new SendCommandEvent();

    public String command;

    public static SendCommandEvent get(String command) {
        INSTANCE.setCancelled(false);
        INSTANCE.command = command;
        return INSTANCE;
    }
}
