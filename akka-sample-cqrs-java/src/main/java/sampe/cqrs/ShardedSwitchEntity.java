package sampe.cqrs;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.ExtensionIdProvider;

public class ShardedSwitchEntity extends AbstractExtensionId<ShardedSwitchEntityImpl>
        implements ExtensionIdProvider {

    public final static ShardedSwitchEntity entityProvider = new ShardedSwitchEntity();

    private ShardedSwitchEntity() {}

    public ShardedSwitchEntity lookup() {
        //  override def lookup: EventProcessorWrapper.type = EventProcessorWrapper
        //TODO need to understand the point of the wrapper and build one
        return ShardedSwitchEntity.entityProvider;
    }

    public ShardedSwitchEntityImpl createExtension(ExtendedActorSystem system) {

        return new ShardedSwitchEntityImpl(system);

    }
}
