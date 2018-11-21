package sampe.cqrs;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorSystem;
import akka.actor.Extension;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class EventProcessorWrapperImpl implements Extension {

    final SettingsImpl settings;

    final ActorSystem system;

    final String typeName;


    final class EntityEnvelop {

        final String eventProcessorId;
        final Object payload;

        EntityEnvelop(String eventProcessorId, Object payload){
            this.eventProcessorId = eventProcessorId;
            this.payload = payload;
        }
    }

    public void start(){
        Config clusterShardingReferenceConfig = ConfigFactory.load("reference.conf").getConfig("akka.cluster.sharding");

        Config tunedClusterSharding = ConfigFactory.parseString("least-shard-allocation-strategy.rebalance-threshold = 1")
                .withFallback(clusterShardingReferenceConfig);

        ClusterSharding.get(system)
                .start(typeName,
                        Props.create(EventProcessor.class),
                        ClusterShardingSettings.create(tunedClusterSharding).withRole("read-model"),
                        buildMessageExtractor());
    }

    private ShardRegion.MessageExtractor buildMessageExtractor() { return null;}


    public EventProcessorWrapperImpl(ActorSystem system) {

        this.system = system;

        settings =  Settings.SettingsProvider.get(system);

        typeName = settings.eventProcessorId;

    }

    final class ProbeEventProcessors{}
    final class Ping{}
    final class Pong{}

    class KeepAlive extends AbstractActorWithTimers {

        @Override
        public void preStart() throws Exception {
            super.preStart();
            timers().startPeriodicTimer("keep-alive", new ProbeEventProcessors(), settings.keepAliveInterval);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ProbeEventProcessors.class, message -> System.out.println(message ))
                    .match(Pong.class, pong -> System.out.println("nothing implemented for pong?"))
                    .build();
        }
    }
}
