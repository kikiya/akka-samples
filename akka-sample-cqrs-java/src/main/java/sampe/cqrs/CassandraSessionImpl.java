package sampe.cqrs;

import akka.Done;
import akka.actor.ActorSystem;
import akka.actor.Extension;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.cassandra.ConfigSessionProvider;
import akka.persistence.cassandra.session.CassandraSessionSettings;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.typesafe.config.Config;

import java.util.concurrent.CompletableFuture;

public class CassandraSessionImpl implements Extension {

    public final CassandraSession cassandraSession;

    public final Materializer materializer;

    public CassandraSessionImpl(ActorSystem system){

        LoggingAdapter log = Logging.getLogger(system, getClass());

        Config sessionConfig = system.settings().config().getConfig("cassandra-journal");

        cassandraSession =
            new CassandraSession(system,
                new ConfigSessionProvider(system, sessionConfig),
                new CassandraSessionSettings(sessionConfig),
                system.dispatcher(),
                log,
                "sample",
                (init) -> CompletableFuture.completedFuture(Done.done())
                );

        materializer = ActorMaterializer.create(system);
    }
}
