package sampe.cqrs;

import akka.Done;
import akka.actor.AbstractLoggingActor;
import akka.persistence.cassandra.query.javadsl.CassandraReadJournal;
import akka.persistence.cassandra.session.javadsl.CassandraSession;
import akka.persistence.query.NoOffset;
import akka.persistence.query.Offset;
import akka.persistence.query.PersistenceQuery;
import akka.persistence.query.TimeBasedUUID;
import akka.stream.Materializer;
import akka.stream.SharedKillSwitch;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.sun.xml.internal.ws.util.CompletedFuture;
import scala.concurrent.ExecutionContext;
import akka.stream.KillSwitches;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


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
    public Receive createReceive() {
        return null;
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
