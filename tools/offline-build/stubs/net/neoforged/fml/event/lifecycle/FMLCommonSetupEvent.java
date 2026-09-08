package net.neoforged.fml.event.lifecycle;
import java.util.concurrent.CompletableFuture;
import net.neoforged.bus.api.Event;
public class FMLCommonSetupEvent extends Event {
    public CompletableFuture<Void> enqueueWork(Runnable work) { return null; }
}
