package sampe.cqrs;

import akka.actor.*;

public class EventProcessorWrapper extends AbstractExtensionId<EventProcessorWrapperImpl> implements ExtensionIdProvider {

    public static final EventProcessorWrapper EventProcessorWrapperProvider = new EventProcessorWrapper();

    private EventProcessorWrapper() {}

    @Override
    public EventProcessorWrapperImpl createExtension(ExtendedActorSystem system) {
        return new EventProcessorWrapperImpl(system);
    }

    @Override
    public ExtensionId<? extends Extension> lookup() {
        return EventProcessorWrapper.EventProcessorWrapperProvider;
    }
}
