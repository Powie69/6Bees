package powie.sixbees.events;

public class TeleportMessageEvent {
    public int seconds;
    public String destination;

    /**
     * @param seconds <code>-1</code> means null
     */
    public TeleportMessageEvent(int seconds, String destination) {
        this.seconds = seconds;
        this.destination = destination;
    }
}
