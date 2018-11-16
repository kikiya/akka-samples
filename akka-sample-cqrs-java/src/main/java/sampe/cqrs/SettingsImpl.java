package sampe.cqrs;

import akka.actor.Extension;
import com.typesafe.config.Config;

import java.time.Duration;

public class SettingsImpl implements Extension {

    //event processor settings
    public final String eventProcessorId;
    public final Duration keepAliveInterval;
    public final String tagPrefix;
    public final int parallelism;
    //switch settings
    public final String switchId;
    public final int shardCount;


    public SettingsImpl(Config config) {
        eventProcessorId = config.getString("event-processor.id");

        //todo may need to do some conversion for duration
        keepAliveInterval = config.getDuration("event-processor.keep-alive-interval");
        tagPrefix = config.getString("event-processor.tag-prefix");
        parallelism = config.getInt("event-processor.parallelism");

        switchId = config.getString("switch.id");
        shardCount = config.getInt("switch.shard-count");
    }

}

