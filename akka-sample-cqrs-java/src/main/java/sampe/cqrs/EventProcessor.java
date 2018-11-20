package sampe.cqrs;

import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractLoggingActor;
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.persistence.query.NoOffset;
import akka.persistence.query.Offset;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.TimeBasedUUID;
import akka.stream.Materializer;
import akka.stream.SharedKillSwitch;
import akka.stream.javadsl.RestartSource;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import scala.concurrent.ExecutionContext;
import akka.stream.KillSwitches;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;


public class EventProcessor extends AbstractLoggingActor {

    private final SettingsImpl settings =
            Settings.SettingsProvider.get(getContext().getSystem());

    private final String eventProcessorId = settings.eventProcessorId;

    private final String tag = self().path().name();

    private final CassandraSession cassandraSession = CassandraSessionExtension
            .CassandraSessionExtensionProvider.get(context().system()).cassandraSession;

    private final Materializer materializer = CassandraSessionExtension
            .CassandraSessionExtensionProvider.get(context().system()).materializer;

    private final ExecutionContext executionContext = context().dispatcher();

    private final CassandraReadJournal query = PersistenceQuery.get(context().system())
            .getReadJournalFor(CassandraReadJournal.class, CassandraReadJournal.Identifier());

    private final SharedKillSwitch killSwitches = KillSwitches.shared("eventProcessorSwitch");

    @Override
    public void preStart() throws Exception {
        super.preStart();
        runQueryStream();
    }

    @Override
    public void postStop() throws Exception {
        super.postStop();
        killSwitches.shutdown();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny((message)-> log().error("Got unexpected message: {}", message))
        .build();
    }

    private void runQueryStream(){
        RestartSource.withBackoff(
                Duration.ofMillis(500),
                Duration.ofSeconds(20),
                0.1,

                () -> Source.fromSourceCompletionStage(
                        readOffset().thenApply(
                                offset -> {
                                    log().info("Starting stream for tag [{}] from offset [{}]", tag, offset);
                                    return query.eventsByTag(tag, offset).map( eventEnvelope -> {
                                        System.out.println(String.format("EventProcessor tag {} got envelope {} ", tag, eventEnvelope));

                                        return eventEnvelope.offset();
                                    }
                                    ).mapAsync(1, o -> writeOffset(o));
                                }
                        )
                )
            )
            .via(killSwitches.flow())
            .runWith(Sink.ignore(), materializer);

    }

    private CompletionStage<Offset> readOffset(){
        return cassandraSession.selectOne(
                "SELECT timeUuidOffset FROM akka_cqrs_sample.offsetStore WHERE eventProcessorId = ? AND tag = ?",
                eventProcessorId, tag
        ).thenCompose(maybe -> CompletableFuture.completedFuture(extractOffset(maybe)));
    }

    private Offset extractOffset(Optional<Row> rowOptional){

        Optional<Offset> timeBasedUUIDOption = rowOptional
                .flatMap( row -> Optional.ofNullable(row.getUUID("timeUuidOffset"))
                    .flatMap(uuid -> Optional.of(new TimeBasedUUID(uuid))));

        return timeBasedUUIDOption.orElse(NoOffset.getInstance());
    }

    private CompletionStage<PreparedStatement> prepareWriteOffset(){
        return cassandraSession.prepare("INSERT INTO akka_cqrs_sample.offsetStore (eventProcessorId, tag, timeUuidOffset) VALUES (?, ?, ?)");
    }

    private CompletionStage<Done> writeOffset(Offset offset){
        if (offset instanceof TimeBasedUUID){
            TimeBasedUUID timeBasedUUID = (TimeBasedUUID)offset;

            return prepareWriteOffset().thenCompose(
                    (PreparedStatement s) -> CompletableFuture.completedFuture(s.bind(eventProcessorId, tag, timeBasedUUID.value())))
                    .thenCompose(
                    (Statement boundStatement) -> cassandraSession.executeWrite(boundStatement));

        } else {
            throw new IllegalArgumentException("Unexpected offset type: " + offset);
        }
    }
}
