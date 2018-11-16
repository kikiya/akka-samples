package sampe.cqrs;

import akka.actor.ActorRef;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;

public class ShardedSwitchEntityImpl implements Extension {

    private final String typeName;

    private final int shardCount;

    private final ActorRef region;



    public ShardedSwitchEntityImpl(ExtendedActorSystem system){

        SettingsImpl settings =  Settings.SettingsProvider.get(system);

        this.typeName = settings.switchId;
        this.shardCount = settings.shardCount;

        ClusterShardingSettings clusterShardingSettings = ClusterShardingSettings.create(system).withRole("write-model");
        region = ClusterSharding.get(system)
                .start(
                        typeName,
                        Props.create(SwitchEntity.class),
                        clusterShardingSettings,
                        buildMessageExtractor()
                );

    }

    //TODO who is eventuallyy using these?
    public void tell(String id, Object msg){

        region.tell(msg, ActorRef.noSender());

    }

    public ActorRef getShardRegion(){ return region; }

    private ShardRegion.MessageExtractor buildMessageExtractor() {
        ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {
            //TODO fix entity id?
            @Override
            public String entityId(Object message) {
                if (message instanceof SwitchCommand)
                    return String.valueOf(typeName.hashCode());
                else
                    return null;
            }

            @Override
            public Object entityMessage(Object message) {
                if (message instanceof SwitchCommand)
                    return message;
                else
                    return message;
            }

            @Override
            public String shardId(Object message) {
                if (message instanceof SwitchCommand) {
                    return String.valueOf(Math.abs(typeName.hashCode() % shardCount));
                } else {
                    return null;
                }
            }
        };

        return messageExtractor;
    }

}
